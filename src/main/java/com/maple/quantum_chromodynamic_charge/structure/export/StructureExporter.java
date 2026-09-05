package com.maple.quantum_chromodynamic_charge.structure.export;

import com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod;
import com.maple.quantum_chromodynamic_charge.common.QCCLevelTask;
import com.maple.quantum_chromodynamic_charge.structure.io.StructureFileIO;
import com.maple.quantum_chromodynamic_charge.structure.io.StructureMappingIO;
import com.maple.quantum_chromodynamic_charge.structure.transform.StructureLocalPos;
import com.maple.quantum_chromodynamic_charge.structure.transform.StructureTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import com.mapleutillib.utils.task.TickableSubscription;
import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2CharLinkedOpenHashMap;
import lombok.Getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 将世界区域导出为结构图案 + 方块映射（异步）。
 *
 * <p>
 * 架构（生产者-消费者）：MC 世界的方块状态只能由服务端主线程安全读取，因此——
 * <ul>
 * <li><b>主线程</b>（每 tick，经 {@link QCCLevelTask} 同步队列驱动）：从世界轻量采样一批
 * {@link BlockState} 放入有界队列（背压式，每 tick 至多 {@link #MAX_SAMPLE_PER_TICK} 格，
 * 通常 &lt;1ms，对 tick 影响极小）；</li>
 * <li><b>后台线程</b>（独立 daemon 线程）：从队列取出快照做旋转/镜像变换、字符表分配、行
 * 构建，全部完成后写出 {@code .mbs} 与映射 JSON（纯内存 + 文件 IO，不触碰世界对象）。</li>
 * </ul>
 * 世界卸载或出错时通过 {@code cancelled} 标志 + 空批哨兵唤醒后台线程并复位导出状态。
 * </p>
 *
 * <p>
 * 说明：MapleUtilLib 的 {@code Tasks.enqueueAsyncTask} 队列（asyncHandler）没有任何驱动者
 * （{@code LevelTickEvent} 只驱动 syncHandler），任务入队后永不执行；若改为后台线程直接读取
 * {@code ServerLevel}/{@code LevelChunk} 又存在数据竞争。因此本实现只把"读取"留在主线程，
 * 重活全部移入后台。
 * </p>
 *
 * 输出目录：{@code logs/platform/&lt;timestamp&gt;.{mbs,json}}。
 */
public final class StructureExporter {

    /** 单个导出任务最多采样的格子数；超出直接拒绝，避免时长失控。 */
    private static final long MAX_EXPORT_VOLUME = 200_000_000L;

    /** 主线程每 tick 最多采样的格子数（约 0.3~1ms，对 TPS 影响很小）。 */
    private static final int MAX_SAMPLE_PER_TICK = 131_072;

    /** 单个队列批次的格子数。 */
    private static final int MAX_BATCH = 65_536;

    /** 有界队列批数（缓冲上限 = 容量 × 批大小）。 */
    private static final int QUEUE_CAPACITY = 8;

    /** 日志进度节流：后台每处理 N 格打印一次。 */
    private static final long PROGRESS_THRESHOLD = 100_000_000L;

    /** 结束哨兵（空批 = 全部采样完毕）。 */
    private static final BlockState[] EMPTY_BATCH = new BlockState[0];

    private static final CharOpenHashSet ILLEGAL_CHARS = new CharOpenHashSet();
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    /** 是否有导出任务进行中（全局单导出）。 */
    @Getter
    private static volatile boolean exporting;

    /** 当前活动导出任务；用于世界卸载时的精确复位。 */
    private static volatile ExportJob activeJob;

    static {
        for (char c : new char[] { '.', '(', ')', ',', '/', '\\', '"', '\'', '`' }) {
            ILLEGAL_CHARS.add(c);
        }
        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
            char c = (char) i;
            if (Character.isISOControl(c)) {
                ILLEGAL_CHARS.add(c);
            }
        }
    }

    private StructureExporter() {}

    /** 世界卸载：取消该世界仍在进行的导出任务。 */
    public static void onWorldUnload(net.minecraft.world.level.Level level) {
        ExportJob job = activeJob;
        if (job != null && job.level == level) {
            job.cancel();
        }
    }

    public static void exportAsync(ServerLevel level, BlockPos pos1, BlockPos pos2,
                                   boolean xMirror, boolean zMirror, int rotation) {
        if (exporting) {
            QuantumChromodynamicChargeMod.LOGGER.warn("Structure export already in progress");
            return;
        }
        int rot = StructureTransform.normalizeRotation(rotation);
        new ExportJob(level, pos1, pos2, xMirror, zMirror, rot).start();
    }

    /** 导出任务实例；一次仅允许一个（{@link #exporting} 防并发）。 */
    private static final class ExportJob {

        /** 读取侧（主线程）与处理侧（后台线程）共享的只读配置。 */
        private final ServerLevel level;
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int outDx;
        private final int dy;
        private final int outDz;
        private final long perRowCount;
        private final boolean xMirror;
        private final boolean zMirror;
        private final int rotation;

        // ---- 主线程（生产者）侧 ----
        private final ArrayBlockingQueue<BlockState[]> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        private final Map<ChunkPos, LevelChunk> chunkCache = new HashMap<>();
        private final MutableBlockPos mutable = new MutableBlockPos();
        private final long total;
        private BlockState[] pending = new BlockState[MAX_BATCH];
        private int pendingN;
        private long index;
        private TickableSubscription<?> subscription;

        // ---- 后台线程（消费者）侧 ----
        private final Reference2CharLinkedOpenHashMap<BlockState> stateToChar = new Reference2CharLinkedOpenHashMap<>();
        private final String[][] zSlices;
        private final Thread worker;
        private volatile boolean cancelled;
        private long cursor;
        private char nextChar;
        private StringBuilder row;

        ExportJob(ServerLevel level, BlockPos a, BlockPos b,
                  boolean xMirror, boolean zMirror, int rotation) {
            this.level = level;
            this.xMirror = xMirror;
            this.zMirror = zMirror;
            this.rotation = rotation;

            this.minX = Math.min(a.getX(), b.getX());
            this.minY = Math.min(a.getY(), b.getY());
            this.minZ = Math.min(a.getZ(), b.getZ());
            int maxX = Math.max(a.getX(), b.getX());
            int maxY = Math.max(a.getY(), b.getY());
            int maxZ = Math.max(a.getZ(), b.getZ());

            int dx = maxX - minX + 1;
            this.dy = maxY - minY + 1;
            int dz = maxZ - minZ + 1;

            boolean swapXZ = rotation == 90 || rotation == 270;
            this.outDx = swapXZ ? dz : dx;
            this.outDz = swapXZ ? dx : dz;
            this.perRowCount = (long) dy * outDx;

            this.total = (long) outDx * dy * outDz;
            this.zSlices = new String[outDz][dy];

            this.worker = new Thread(this::runWorker, "QCC-StructureExport");
            this.worker.setDaemon(true);

            stateToChar.put(Blocks.AIR.defaultBlockState(), ' ');
            this.nextChar = nextValid('A');
        }

        void start() {
            if (total <= 0) {
                QuantumChromodynamicChargeMod.LOGGER.error("Structure export rejected: empty region");
                return;
            }
            if (total > MAX_EXPORT_VOLUME) {
                QuantumChromodynamicChargeMod.LOGGER.error(
                        "Structure export rejected: region too large ({} blocks, max {})",
                        total, MAX_EXPORT_VOLUME);
                return;
            }
            exporting = true;
            activeJob = this;
            QuantumChromodynamicChargeMod.LOGGER.info("Starting structure export {}x{}x{}", outDx, dy, outDz);
            worker.start();
            try {
                this.subscription = QCCLevelTask.TASKS.enqueueTick(level, this::tick, 0, 0);
            } catch (Throwable t) {
                QuantumChromodynamicChargeMod.LOGGER.error("Structure export could not be scheduled", t);
                cancel();
            }
        }

        // ==================== 主线程：轻量采样入队 ====================

        private void tick() {
            if (cancelled) {
                return;
            }
            try {
                int sampled = 0;
                while (sampled < MAX_SAMPLE_PER_TICK && index < total && queue.remainingCapacity() > 0) {
                    long idx = index++;
                    int outZ = (int) (idx / perRowCount);
                    long rem = idx % perRowCount;
                    int outY = (int) (rem / outDx);
                    int outX = (int) (rem % outDx);

                    StructureLocalPos sample = StructureTransform.exportSampleXZ(
                            outX, outZ, outDx, outDz, rotation, xMirror, zMirror);
                    mutable.set(minX + sample.x(), minY + outY, minZ + sample.z());
                    pending[pendingN++] = getCached(mutable);
                    sampled++;
                    if (pendingN == MAX_BATCH) {
                        flushPending();
                    }
                }
                if (index >= total) {
                    flushPending();
                    // 结束哨兵必须严格排在最后一个批次之后；极端情况下队列已满时短暂等待后台腾位
                    while (!queue.offer(EMPTY_BATCH)) {
                        Thread.yield();
                    }
                    if (subscription != null) {
                        subscription.unsubscribe();
                    }
                }
            } catch (Throwable t) {
                QuantumChromodynamicChargeMod.LOGGER.error("Structure export sampling failed", t);
                cancel();
            }
        }

        /** 将采样批送入队列（尾部不满批截断到有效长度）；队列满时短暂自旋等待后台消费。 */
        private void flushPending() {
            if (pendingN == 0) {
                return;
            }
            BlockState[] batch = pendingN == MAX_BATCH ? pending : Arrays.copyOf(pending, pendingN);
            pending = new BlockState[MAX_BATCH];
            pendingN = 0;
            while (!queue.offer(batch)) {
                Thread.yield();
            }
        }

        private BlockState getCached(MutableBlockPos pos) {
            ChunkPos cp = new ChunkPos(pos);
            LevelChunk chunk = chunkCache.computeIfAbsent(cp, c -> level.getChunk(c.x, c.z));
            return chunk.getBlockState(pos);
        }

        // ==================== 后台线程：变换 / 编码 / 写文件 ====================

        private void runWorker() {
            boolean ended = false;
            try {
                while (!ended) {
                    BlockState[] batch;
                    try {
                        batch = queue.take();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (batch.length == 0) {
                        ended = true;
                    } else {
                        process(batch);
                    }
                }
                if (!cancelled) {
                    finish();
                }
            } catch (Throwable t) {
                QuantumChromodynamicChargeMod.LOGGER.error("Structure export failed", t);
            } finally {
                cleanup();
            }
        }

        private void process(BlockState[] batch) {
            for (BlockState original : batch) {
                BlockState transformed = StructureTransform.transformStateForExport(
                        original, rotation, xMirror, zMirror);
                if (!stateToChar.containsKey(transformed)) {
                    stateToChar.put(transformed, nextChar);
                    nextChar = nextValid((char) (nextChar + 1));
                }
                char c = stateToChar.getChar(transformed);
                long idx = cursor++;
                int outZ = (int) (idx / perRowCount);
                long rem = idx % perRowCount;
                int outY = (int) (rem / outDx);
                int outX = (int) (rem % outDx);

                if (outX == 0) {
                    row = new StringBuilder(outDx);
                }
                row.append(c);
                if (outX == outDx - 1) {
                    zSlices[outZ][outY] = row.toString();
                    row = null;
                }
                if ((idx + 1) % PROGRESS_THRESHOLD == 0 || idx + 1 == total) {
                    QuantumChromodynamicChargeMod.LOGGER.info(
                            "Export progress: {} / {} ({}%)",
                            idx + 1, total, String.format("%.2f", (idx + 1) * 100.0 / total));
                }
            }
        }

        private void finish() {
            try {
                Path outDir = Paths.get("logs", "platform");
                Files.createDirectories(outDir);
                String stamp = LocalDateTime.now().format(TIMESTAMP);
                Path structurePath = outDir.resolve(stamp + ".mbs");
                Path mappingPath = outDir.resolve(stamp + ".json");

                StructureFileIO.save(structurePath, zSlices);
                StructureMappingIO.save(StructureMappingIO.invert(stateToChar), mappingPath);
                QuantumChromodynamicChargeMod.LOGGER.info("Exported structure: {}", structurePath);
                QuantumChromodynamicChargeMod.LOGGER.info("Exported mapping: {}", mappingPath);
            } catch (Exception e) {
                QuantumChromodynamicChargeMod.LOGGER.error("Structure export write failed", e);
            }
        }

        /** 取消导出（主线程/卸载事件调用；空批哨兵唤醒后台线程收尾）。 */
        private void cancel() {
            cancelled = true;
            queue.offer(EMPTY_BATCH);
        }

        private void cleanup() {
            if (subscription != null) {
                subscription.unsubscribe();
                subscription = null;
            }
            if (activeJob == this) {
                activeJob = null;
            }
            exporting = false;
        }
    }

    /** 生成 .mbs / 映射中允许使用的下一个字符（跳过非法与不可见字符）。 */
    private static char nextValid(char start) {
        char ch = start;
        while (ILLEGAL_CHARS.contains(ch)) {
            ch++;
        }
        return ch;
    }
}
