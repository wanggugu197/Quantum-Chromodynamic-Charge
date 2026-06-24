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

import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2CharLinkedOpenHashMap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将世界区域导出为结构图案 + 方块映射（异步）。
 * 输出目录：{@code logs/platform/&lt;timestamp&gt;.{mbs,json}}。
 */
public final class StructureExporter {

    private static final CharOpenHashSet ILLEGAL_CHARS = new CharOpenHashSet();
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final long PROGRESS_THRESHOLD = 1_000_000L;
    private static volatile boolean exporting;

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

    public static boolean isExporting() {
        return exporting;
    }

    public static void exportAsync(ServerLevel level, BlockPos pos1, BlockPos pos2,
                                   boolean xMirror, boolean zMirror, int rotation) {
        if (exporting) {
            QuantumChromodynamicChargeMod.LOGGER.warn("Structure export already in progress");
            return;
        }
        exporting = true;
        int rot = StructureTransform.normalizeRotation(rotation);
        QCCLevelTask.TASKS.enqueueAsyncTask(level, () -> {
            try {
                exportStructure(level, pos1, pos2, xMirror, zMirror, rot);
            } catch (Exception e) {
                QuantumChromodynamicChargeMod.LOGGER.error("Structure export failed", e);
            } finally {
                exporting = false;
            }
        });
    }

    private static void exportStructure(ServerLevel level, BlockPos a, BlockPos b,
                                        boolean xMirror, boolean zMirror, int rotation) throws Exception {
        Path outDir = Paths.get("logs", "platform");
        Files.createDirectories(outDir);
        String stamp = LocalDateTime.now().format(TIMESTAMP);
        Path structurePath = outDir.resolve(stamp + ".mbs");
        Path mappingPath = outDir.resolve(stamp + ".json");

        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX());
        int maxY = Math.max(a.getY(), b.getY());
        int maxZ = Math.max(a.getZ(), b.getZ());

        int dx = maxX - minX + 1;
        int dy = maxY - minY + 1;
        int dz = maxZ - minZ + 1;

        boolean swapXZ = rotation == 90 || rotation == 270;
        int outDx = swapXZ ? dz : dx;
        int outDz = swapXZ ? dx : dz;

        Reference2CharLinkedOpenHashMap<BlockState> stateToChar = new Reference2CharLinkedOpenHashMap<>();
        char nextChar = nextValid('A');
        stateToChar.put(Blocks.AIR.defaultBlockState(), ' ');

        long total = (long) outDx * dy * outDz;
        long progress = 0;
        MutableBlockPos mutable = new MutableBlockPos();
        Map<ChunkPos, LevelChunk> chunkCache = new HashMap<>();

        QuantumChromodynamicChargeMod.LOGGER.info("Starting structure export {}x{}x{}", outDx, dy, outDz);

        List<String[]> zSlices = new ArrayList<>(outDz);
        for (int outZ = 0; outZ < outDz; outZ++) {
            List<String> ySlices = new ArrayList<>(dy);
            for (int outY = 0; outY < dy; outY++) {
                StringBuilder row = new StringBuilder(outDx);
                for (int outX = 0; outX < outDx; outX++) {
                    StructureLocalPos sample = StructureTransform.exportSampleXZ(
                            outX, outZ, outDx, outDz, rotation, xMirror, zMirror);
                    mutable.set(minX + sample.x(), minY + outY, minZ + sample.z());
                    BlockState original = getCached(level, mutable, chunkCache);
                    BlockState transformed = StructureTransform.transformStateForExport(
                            original, rotation, xMirror, zMirror);
                    if (!stateToChar.containsKey(transformed)) {
                        stateToChar.put(transformed, nextChar);
                        nextChar = nextValid((char) (nextChar + 1));
                    }
                    row.append(stateToChar.getChar(transformed));
                    if (++progress % PROGRESS_THRESHOLD == 0 || progress == total) {
                        QuantumChromodynamicChargeMod.LOGGER.info(
                                "Export progress: {} / {} ({}%)",
                                progress, total, String.format("%.2f", progress * 100.0 / total));
                    }
                }
                ySlices.add(row.toString());
            }
            zSlices.add(ySlices.toArray(new String[0]));
        }

        StructureFileIO.save(structurePath, zSlices.toArray(new String[0][]));
        StructureMappingIO.save(StructureMappingIO.invert(stateToChar), mappingPath);
        QuantumChromodynamicChargeMod.LOGGER.info("Exported structure: {}", structurePath);
        QuantumChromodynamicChargeMod.LOGGER.info("Exported mapping: {}", mappingPath);
    }

    private static BlockState getCached(ServerLevel level, MutableBlockPos pos,
                                        Map<ChunkPos, LevelChunk> cache) {
        ChunkPos cp = ChunkPos.containing(pos);
        LevelChunk chunk = cache.computeIfAbsent(cp, c -> level.getChunk(c.x(), c.z()));
        return chunk.getBlockState(pos);
    }

    private static char nextValid(char start) {
        char ch = start;
        while (ILLEGAL_CHARS.contains(ch)) {
            ch++;
        }
        return ch;
    }
}
