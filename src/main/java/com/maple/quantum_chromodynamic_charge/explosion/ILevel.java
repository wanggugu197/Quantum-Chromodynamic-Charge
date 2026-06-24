package com.maple.quantum_chromodynamic_charge.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEngine;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 批量方块写入与空气清除（直写 section，绕过邻接更新）。
 */
public interface ILevel {

    /** 单区块实际变化数达到此值时发整区块包，否则发区段更新包 */
    int WHOLE_CHUNK_THRESHOLD = 4096 * 2;

    // ========== 公共入口 ==========

    /**
     * 批量设置任意方块。
     */
    static void SetBlocks(
                          ServerLevel level,
                          Map<BlockPos, BlockState> changes,
                          boolean updateHeightmap,
                          boolean updateLight,
                          boolean syncToClient) {
        if (changes == null || changes.isEmpty()) return;

        Map<ChunkPos, Map<BlockPos, BlockState>> chunkGroups = changes.entrySet().stream()
                .collect(Collectors.groupingBy(
                        e -> new ChunkPos(
                                SectionPos.blockToSectionCoord(e.getKey().getX()),
                                SectionPos.blockToSectionCoord(e.getKey().getZ())),
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (_, b) -> b)));

        fastSetBlocks(level, chunkGroups, updateHeightmap, updateLight, syncToClient);
    }

    /**
     * 批量设为空气（{@link BlockPos} 列表）。
     */
    static void setAirBlocks(
                             ServerLevel level,
                             List<BlockPos> positions,
                             boolean updateHeightmap,
                             boolean updateLight,
                             boolean syncToClient) {
        if (positions == null || positions.isEmpty()) return;

        Long2ObjectMap<LongList> chunkGroups = new Long2ObjectOpenHashMap<>();
        for (BlockPos pos : positions) {
            long key = ChunkPos.pack(
                    SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getZ()));
            chunkGroups.computeIfAbsent(key, _ -> new LongArrayList()).add(pos.asLong());
        }
        applyAirByChunk(level, chunkGroups, updateHeightmap, updateLight, syncToClient);
    }

    /**
     * 批量设为空气（打包坐标缓冲，减少临时 {@link BlockPos} 分配）。
     *
     * @param updateLight 为 {@code false} 时跳过光照引擎，大爆炸可显著减轻 TPS 压力
     *                    （客户端仍可收到方块/全量区块包；天空光可能暂不同步直至后续更新）
     */
    static void setAirBlocksPacked(
                                   ServerLevel level,
                                   long[] packed,
                                   int count,
                                   boolean updateHeightmap,
                                   boolean updateLight,
                                   boolean syncToClient) {
        if (packed == null || count <= 0) return;

        Long2ObjectMap<LongList> chunkGroups = new Long2ObjectOpenHashMap<>();
        for (int i = 0; i < count; i++) {
            long p = packed[i];
            int x = BlockPos.getX(p);
            int z = BlockPos.getZ(p);
            long key = ChunkPos.pack(
                    SectionPos.blockToSectionCoord(x),
                    SectionPos.blockToSectionCoord(z));
            chunkGroups.computeIfAbsent(key, _ -> new LongArrayList()).add(p);
        }
        applyAirByChunk(level, chunkGroups, updateHeightmap, updateLight, syncToClient);
    }

    /** 便捷：默认同步客户端 */
    static void setAirBlocks(ServerLevel level, List<BlockPos> positions,
                             boolean updateHeightmap, boolean updateLight) {
        setAirBlocks(level, positions, updateHeightmap, updateLight, true);
    }

    /** 便捷：打包坐标 + 默认同步客户端 */
    static void setAirBlocksPacked(ServerLevel level, long[] packed, int count,
                                   boolean updateHeightmap, boolean updateLight) {
        setAirBlocksPacked(level, packed, count, updateHeightmap, updateLight, true);
    }

    // ========== 内部实现 ==========

    private static void applyAirByChunk(
                                        ServerLevel level,
                                        Long2ObjectMap<LongList> chunkGroups,
                                        boolean updateHeightmap,
                                        boolean updateLight,
                                        boolean syncToClient) {
        for (Long2ObjectMap.Entry<LongList> entry : chunkGroups.long2ObjectEntrySet()) {
            long key = entry.getLongKey();
            setAirBlocksInternal(
                    level,
                    ChunkPos.getX(key),
                    ChunkPos.getZ(key),
                    entry.getValue(),
                    updateHeightmap,
                    updateLight,
                    syncToClient);
        }
    }

    /**
     * 对已按区块分组的任意方块变更进行处理。
     */
    static void fastSetBlocks(
                              ServerLevel level,
                              Map<ChunkPos, Map<BlockPos, BlockState>> chunkGroups,
                              boolean updateHeightmap,
                              boolean updateLight,
                              boolean syncToClient) {
        if (chunkGroups.isEmpty()) return;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (Map.Entry<ChunkPos, Map<BlockPos, BlockState>> chunkEntry : chunkGroups.entrySet()) {
            ChunkPos chunkPos = chunkEntry.getKey();
            Map<BlockPos, BlockState> chunkChanges = chunkEntry.getValue();
            LevelChunk chunk = level.getChunk(chunkPos.x(), chunkPos.z());

            Short2IntMap columnMaxY = updateHeightmap ? new Short2IntOpenHashMap() : null;
            IntSet changedSections = updateLight ? new IntOpenHashSet() : null;
            List<BlockPos> lightCheckPositions = updateLight ? new ArrayList<>() : null;
            List<BlockPos> beToRemove = new ArrayList<>();
            List<Map.Entry<BlockPos, BlockState>> beToCreate = new ArrayList<>();
            List<BlockPos> changedPositions = syncToClient ? new ArrayList<>(chunkChanges.size()) : null;
            int changedCount = 0;

            for (Map.Entry<BlockPos, BlockState> entry : chunkChanges.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();
                int y = pos.getY();
                if (level.isOutsideBuildHeight(y)) continue;

                int sectionIndex = chunk.getSectionIndex(y);
                LevelChunkSection section = chunk.getSection(sectionIndex);
                int lx = pos.getX() & 15;
                int ly = y & 15;
                int lz = pos.getZ() & 15;

                boolean wasEmpty = section.hasOnlyAir();
                BlockState oldState = section.setBlockState(lx, ly, lz, state, false);
                if (oldState == state) continue;

                changedCount++;
                if (changedPositions != null) changedPositions.add(pos);

                if (columnMaxY != null) {
                    short colKey = (short) ((lx << 4) | lz);
                    int curMax = columnMaxY.getOrDefault(colKey, Integer.MIN_VALUE);
                    if (y > curMax) columnMaxY.put(colKey, y);
                }

                if (updateLight) {
                    if (wasEmpty != section.hasOnlyAir()) {
                        changedSections.add(sectionIndex);
                    }
                    if (LightEngine.hasDifferentLightProperties(chunk, pos, oldState, state)) {
                        lightCheckPositions.add(pos);
                    }
                }

                if (oldState.hasBlockEntity()) beToRemove.add(pos);
                if (state.hasBlockEntity()) beToCreate.add(entry);
            }

            applyHeightmapUpdates(chunk, chunkPos, columnMaxY, mutablePos);
            applyLightUpdates(level, chunk, chunkPos, changedSections, lightCheckPositions);

            for (BlockPos pos : beToRemove) {
                chunk.removeBlockEntity(pos);
            }
            for (Map.Entry<BlockPos, BlockState> entry : beToCreate) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();
                BlockEntity existing = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
                if (existing != null && existing.getType().isValid(state)) {
                    updateBlockEntityState(existing, state);
                    continue;
                }
                if (state.getBlock() instanceof EntityBlock entityBlock) {
                    BlockEntity be = entityBlock.newBlockEntity(pos, state);
                    if (be != null) chunk.addAndRegisterBlockEntity(be);
                }
            }

            chunk.markUnsaved();

            if (syncToClient && changedCount > 0) {
                syncChunkChanges(level, chunk, chunkPos, changedPositions,
                        beToRemove, List.of(), updateLight, changedCount);
            }
        }
    }

    /**
     * 单区块空气清除（打包坐标列表）。
     */
    private static void setAirBlocksInternal(
                                             ServerLevel level,
                                             int chunkX,
                                             int chunkZ,
                                             LongList positions,
                                             boolean updateHeightmap,
                                             boolean updateLight,
                                             boolean syncToClient) {
        if (positions.isEmpty()) return;

        final BlockState air = Blocks.AIR.defaultBlockState();
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        Short2IntMap columnMaxY = updateHeightmap ? new Short2IntOpenHashMap() : null;
        IntSet changedSections = updateLight ? new IntOpenHashSet() : null;
        List<BlockPos> lightCheckPositions = updateLight ? new ArrayList<>() : null;
        List<BlockPos> bePositions = new ArrayList<>();
        List<BlockState> beOldStates = new ArrayList<>();
        List<BlockPos> changedPositions = syncToClient ? new ArrayList<>(positions.size()) : null;
        int changedCount = 0;

        for (int i = 0, size = positions.size(); i < size; i++) {
            long packed = positions.getLong(i);
            int x = BlockPos.getX(packed);
            int y = BlockPos.getY(packed);
            int z = BlockPos.getZ(packed);
            if (level.isOutsideBuildHeight(y)) continue;

            int secIdx = chunk.getSectionIndex(y);
            LevelChunkSection section = chunk.getSection(secIdx);
            int lx = x & 15;
            int ly = y & 15;
            int lz = z & 15;

            BlockState oldState = section.getBlockState(lx, ly, lz);
            if (oldState.isAir()) {
                // 已是空气：跳过高度图（避免用高空空气污染列更新）
                continue;
            }

            boolean wasEmpty = section.hasOnlyAir();

            changedCount++;
            mutablePos.set(x, y, z);
            if (changedPositions != null) {
                changedPositions.add(mutablePos.immutable());
            }
            if (oldState.hasBlockEntity()) {
                bePositions.add(mutablePos.immutable());
                beOldStates.add(oldState);
            }

            chunk.removeBlockEntity(mutablePos);
            section.setBlockState(lx, ly, lz, air, false);

            if (updateLight) {
                if (wasEmpty != section.hasOnlyAir()) {
                    changedSections.add(secIdx);
                }
                if (LightEngine.hasDifferentLightProperties(chunk, mutablePos, oldState, air)) {
                    lightCheckPositions.add(mutablePos.immutable());
                }
            }

            if (columnMaxY != null) {
                short colKey = (short) ((lx << 4) | lz);
                int cur = columnMaxY.getOrDefault(colKey, Integer.MIN_VALUE);
                if (y > cur) columnMaxY.put(colKey, y);
            }
        }

        applyHeightmapUpdates(chunk, chunkPos, columnMaxY, mutablePos);
        applyLightUpdates(level, chunk, chunkPos, changedSections, lightCheckPositions);
        chunk.markUnsaved();

        if (syncToClient && changedCount > 0) {
            syncChunkChanges(level, chunk, chunkPos, changedPositions,
                    bePositions, beOldStates, updateLight, changedCount);
        }
    }

    // ========== 高度图、光照、同步 ==========

    private static void applyHeightmapUpdates(
                                              LevelChunk chunk,
                                              ChunkPos chunkPos,
                                              Short2IntMap columnMaxY,
                                              BlockPos.MutableBlockPos mutablePos) {
        if (columnMaxY == null || columnMaxY.isEmpty()) return;

        Heightmap[] maps = {
                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING),
                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR),
                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE)
        };
        for (Short2IntMap.Entry e : columnMaxY.short2IntEntrySet()) {
            short col = e.getShortKey();
            int lx = (col >> 4) & 15;
            int lz = col & 15;
            int maxY = e.getIntValue();
            mutablePos.set(chunkPos.getBlockX(lx), maxY, chunkPos.getBlockZ(lz));
            BlockState top = chunk.getBlockState(mutablePos);
            for (Heightmap map : maps) {
                map.update(lx, maxY, lz, top);
            }
        }
    }

    private static void applyLightUpdates(
                                          ServerLevel level,
                                          LevelChunk chunk,
                                          ChunkPos chunkPos,
                                          IntSet changedSections,
                                          List<BlockPos> lightCheckPositions) {
        if (changedSections == null || lightCheckPositions == null) return;

        LevelLightEngine engine = level.getChunkSource().getLightEngine();
        int minSectionY = chunk.getMinSectionY();

        for (int idx : changedSections) {
            SectionPos sp = SectionPos.of(chunkPos, minSectionY + idx);
            engine.updateSectionStatus(sp, chunk.getSection(idx).hasOnlyAir());
        }
        for (BlockPos pos : lightCheckPositions) {
            chunk.getSkyLightSources().update(chunk, pos.getX() & 15, pos.getY(), pos.getZ() & 15);
            engine.checkBlock(pos);
        }
    }

    /**
     * 网络同步：变化量大发全量区块包，否则发区段包 + BE 补偿。
     */
    private static void syncChunkChanges(
                                         ServerLevel level,
                                         LevelChunk chunk,
                                         ChunkPos chunkPos,
                                         List<BlockPos> changedPositions,
                                         List<BlockPos> bePositions,
                                         List<BlockState> beOldStates,
                                         boolean updateLight,
                                         int changedCount) {
        if (changedPositions == null || changedPositions.isEmpty()) return;

        List<ServerPlayer> players = level.getChunkSource().chunkMap.getPlayers(chunkPos, false);
        if (players.isEmpty()) return;

        if (changedCount >= WHOLE_CHUNK_THRESHOLD) {
            BitSet skyChanged = null;
            BitSet blockChanged = null;
            if (updateLight) {
                int minSection = chunk.getMinSectionY();
                int maxSection = chunk.getMaxSectionY();
                int totalSections = maxSection - minSection + 1;
                skyChanged = new BitSet(totalSections);
                blockChanged = new BitSet(totalSections);
                for (BlockPos pos : changedPositions) {
                    int secIdx = chunk.getSectionIndex(pos.getY());
                    int sectionY = minSection + secIdx;
                    int bit = sectionY - minSection;
                    if (bit >= 0 && bit < totalSections) {
                        skyChanged.set(bit);
                        blockChanged.set(bit);
                    }
                }
            }
            LevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
            ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(chunk, lightEngine, skyChanged, blockChanged);
            for (ServerPlayer player : players) {
                player.connection.send(packet);
            }
            return;
        }

        Map<SectionPos, ShortSet> sectionPacketMap = new HashMap<>();
        for (BlockPos pos : changedPositions) {
            int y = pos.getY();
            if (level.isOutsideBuildHeight(y)) continue;
            SectionPos sp = SectionPos.of(pos);
            sectionPacketMap.computeIfAbsent(sp, _ -> new ShortOpenHashSet())
                    .add(SectionPos.sectionRelativePos(pos));
        }

        int minSectionY = chunk.getMinSectionY();
        for (Map.Entry<SectionPos, ShortSet> sec : sectionPacketMap.entrySet()) {
            SectionPos sp = sec.getKey();
            int idx = sp.y() - minSectionY;
            if (idx < 0 || idx >= chunk.getSectionsCount()) continue;
            ClientboundSectionBlocksUpdatePacket pkt = new ClientboundSectionBlocksUpdatePacket(sp, sec.getValue(), chunk.getSection(idx));
            for (ServerPlayer player : players) {
                player.connection.send(pkt);
            }
        }

        int beCount = Math.min(bePositions.size(), beOldStates.size());
        for (int i = 0; i < beCount; i++) {
            BlockPos pos = bePositions.get(i);
            BlockState oldState = beOldStates.get(i);
            level.sendBlockUpdated(pos, oldState, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @SuppressWarnings("deprecation")
    private static void updateBlockEntityState(BlockEntity blockEntity, BlockState state) {
        blockEntity.setBlockState(state);
    }
}
