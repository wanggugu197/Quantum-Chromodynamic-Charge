package com.maple.quantum_chromodynamic_charge.structure.place;

import com.maple.quantum_chromodynamic_charge.common.QCCLevelTask;
import com.maple.quantum_chromodynamic_charge.common.QCCRegistration;
import com.maple.quantum_chromodynamic_charge.explosion.ILevel;
import com.maple.quantum_chromodynamic_charge.structure.io.StructureFileIO;
import com.maple.quantum_chromodynamic_charge.structure.io.StructureMappingIO;
import com.maple.quantum_chromodynamic_charge.structure.model.StructureDefinition.Structure;
import com.maple.quantum_chromodynamic_charge.structure.transform.StructureLocalPos;
import com.maple.quantum_chromodynamic_charge.structure.transform.StructureTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.mapleutillib.utils.task.TickableSubscription;
import it.unimi.dsi.fastutil.chars.Char2ReferenceOpenHashMap;

import java.io.IOException;
import java.util.*;
import java.util.function.IntConsumer;

/**
 * 分 tick 结构放置：批量写 section（经 {@link ILevel#SetBlocks}）。
 */
public final class StructurePlacer {

    /** 单 tick 内最多扫描的 pattern 格数（含跳过空气），防止极端情况下空转过久。 */
    private static final int MAX_SCAN_PER_TICK = 1_000_000;

    private static final Set<Block> PLACEMENT_BLACKLIST = Set.of(
            Blocks.BEDROCK,
            Blocks.END_PORTAL_FRAME,
            Blocks.END_PORTAL,
            Blocks.END_GATEWAY,
            Blocks.BARRIER);

    private final ServerLevel level;
    private final BlockIterator iterator;
    private final int perTick;
    private final boolean skipOccupied;
    private final boolean skipAir;
    private final boolean updateLight;
    private final boolean updateHeightmap;
    private final IntConsumer onBatch;
    private final Runnable onFinished;
    private TickableSubscription<?> subscription;

    private StructurePlacer(ServerLevel level,
                            BlockIterator iterator,
                            int perTick,
                            boolean skipOccupied,
                            boolean skipAir,
                            boolean updateLight,
                            boolean updateHeightmap,
                            IntConsumer onBatch,
                            Runnable onFinished) {
        this.level = level;
        this.iterator = iterator;
        this.perTick = Math.max(1, perTick);
        this.skipOccupied = skipOccupied;
        this.skipAir = skipAir;
        this.updateLight = updateLight;
        this.updateHeightmap = updateHeightmap;
        this.onBatch = onBatch;
        this.onFinished = onFinished;
        this.subscription = QCCLevelTask.TASKS.enqueueTick(level, this::placeBatch, 0, 0);
    }

    private void placeBatch() {
        Map<BlockPos, BlockState> batch = new HashMap<>(Math.min(perTick, 4096));
        int placed = 0;
        int scanned = 0;

        while (iterator.hasNext() && placed < perTick && scanned < MAX_SCAN_PER_TICK) {
            scanned++;
            BlockIterator.Entry entry = iterator.next();
            BlockState state = entry.state();
            if (skipAir && state.isAir()) {
                continue;
            }
            BlockPos pos = entry.pos();
            if (level.isOutsideBuildHeight(pos.getY())) {
                continue;
            }
            BlockState old = level.getBlockState(pos);
            if (old.getBlock() == QCCRegistration.STRUCTURE_PLACER.get()) {
                continue;
            }
            if (state.isAir() && old.isAir()) {
                continue;
            }
            // 跳过已有方块：世界上非空气则不放置
            if (skipOccupied && !old.isAir()) {
                placed++;
                continue;
            }
            if (PLACEMENT_BLACKLIST.contains(old.getBlock())) {
                placed++;
                continue;
            }
            batch.put(pos, state);
            placed++;
        }

        if (!batch.isEmpty()) {
            ILevel.SetBlocks(level, batch, updateHeightmap, updateLight, true);
        }

        if (onBatch != null) {
            onBatch.accept(iterator.getProgressPercentage());
        }

        if (!iterator.hasNext()) {
            if (subscription != null) {
                subscription.unsubscribe();
                subscription = null;
            }
            if (onFinished != null) {
                onFinished.run();
            }
        }
    }

    /**
     * 预加载图案与映射；失败抛异常。应在扣材料<strong>之前</strong>调用。
     */
    public static LoadedStructure load(Structure structure) throws IOException {
        Objects.requireNonNull(structure, "structure");
        StructureFileIO.PatternData data = StructureFileIO.loadResource(structure.resource());
        if (data.sizeX() <= 0 || data.sizeY() <= 0 || data.sizeZ() <= 0) {
            throw new IOException("invalid pattern size for " + structure.resource());
        }
        Char2ReferenceOpenHashMap<BlockState> mapping = StructureMappingIO.loadResource(structure.blockMapping());
        if (mapping.isEmpty()) {
            throw new IOException("empty or missing block mapping: " + structure.blockMapping());
        }
        return new LoadedStructure(data, mapping);
    }

    public static void placeStructureAsync(Level level,
                                           BlockPos startPos,
                                           Structure structure,
                                           int perTick,
                                           boolean skipOccupied,
                                           boolean skipAir,
                                           boolean updateLight,
                                           boolean updateHeightmap,
                                           boolean zMirror,
                                           boolean xMirror,
                                           int rotation,
                                           IntConsumer onBatch,
                                           Runnable onFinished) throws IOException {
        placeStructureAsync(
                level, startPos, load(structure), perTick,
                skipOccupied, skipAir, updateLight, updateHeightmap,
                zMirror, xMirror, rotation, onBatch, onFinished);
    }

    public static void placeStructureAsync(Level level,
                                           BlockPos startPos,
                                           LoadedStructure loaded,
                                           int perTick,
                                           boolean skipOccupied,
                                           boolean skipAir,
                                           boolean updateLight,
                                           boolean updateHeightmap,
                                           boolean zMirror,
                                           boolean xMirror,
                                           int rotation,
                                           IntConsumer onBatch,
                                           Runnable onFinished) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalArgumentException("Structure placement requires ServerLevel");
        }
        Objects.requireNonNull(loaded, "loaded");
        BlockIterator iterator = new BlockIterator(
                loaded.data().pattern(), startPos, loaded.mapping(), zMirror, xMirror, rotation);
        new StructurePlacer(
                serverLevel, iterator, perTick,
                skipOccupied, skipAir, updateLight, updateHeightmap,
                onBatch, onFinished);
    }

    /** 已加载的图案 + 映射，避免扣费后加载失败。 */
    public record LoadedStructure(
                                  StructureFileIO.PatternData data,
                                  Char2ReferenceOpenHashMap<BlockState> mapping) {}

    private static final class BlockIterator implements Iterator<BlockIterator.Entry> {

        private final String[][] pattern;
        private final BlockPos startPos;
        private final Char2ReferenceOpenHashMap<BlockState> mapping;
        private final boolean zMirror;
        private final boolean xMirror;
        private final int rotation;
        private final int sizeX;
        private final int sizeZ;
        private final StructureLocalPos originOffset;

        private int z;
        private int y;
        private int x;
        private boolean done;

        BlockIterator(String[][] pattern, BlockPos startPos,
                      Char2ReferenceOpenHashMap<BlockState> mapping,
                      boolean zMirror, boolean xMirror, int rotation) {
            this.pattern = pattern;
            this.startPos = startPos;
            this.mapping = mapping;
            this.zMirror = zMirror;
            this.xMirror = xMirror;
            this.rotation = rotation;

            int sx = pattern.length > 0 && pattern[0].length > 0 ? pattern[0][0].length() : 0;
            int sy = pattern.length > 0 ? pattern[0].length : 0;
            int sz = pattern.length;
            this.sizeX = sx;
            this.sizeZ = sz;
            this.originOffset = StructureTransform.calcOriginOffset(sx, sy, sz, rotation, xMirror, zMirror);
        }

        int getProgressPercentage() {
            if (pattern.length == 0) return 0;
            return Math.min(100, (int) (((double) z / pattern.length) * 100));
        }

        @Override
        public boolean hasNext() {
            if (done) return false;
            while (z < pattern.length) {
                String[] aisle = pattern[z];
                while (y < aisle.length) {
                    String row = aisle[y];
                    while (x < row.length()) {
                        if (mapping.containsKey(row.charAt(x))) {
                            return true;
                        }
                        x++;
                    }
                    x = 0;
                    y++;
                }
                y = 0;
                z++;
            }
            done = true;
            return false;
        }

        @Override
        public Entry next() {
            if (!hasNext()) throw new NoSuchElementException();
            char c = pattern[z][y].charAt(x);
            BlockState state = mapping.get(c);
            if (state == null) {
                state = Blocks.AIR.defaultBlockState();
            }
            StructureLocalPos t = StructureTransform.transformCoords(
                    x, y, z, sizeX, sizeZ, rotation, xMirror, zMirror);
            BlockPos pos = new BlockPos(
                    startPos.getX() + t.x() + originOffset.x(),
                    startPos.getY() + t.y() + originOffset.y(),
                    startPos.getZ() + t.z() + originOffset.z());
            state = StructureTransform.transformStateForPlace(state, rotation, xMirror, zMirror);
            x++;
            return new Entry(pos, state);
        }

        record Entry(BlockPos pos, BlockState state) {}
    }
}
