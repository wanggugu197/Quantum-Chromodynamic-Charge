package com.maple.quantum_chromodynamic_charge.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * 批量方块写入与空气清除（直写 section，绕过邻接更新）。
 */
public interface ILevel {

    /** 单区块实际变化数达到此值时发整区块包，否则逐方块 */
    int WHOLE_CHUNK_THRESHOLD = 4096 * 2;

    record BlockSelection(Set<TagKey<Block>> tags, Set<Block> blocks) {}

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
        SetBlocks(level, changes, updateHeightmap, updateLight, syncToClient, null, null);
    }

    /**
     * 批量设置任意方块，并支持黑白名单过滤。
     *
     * <p>
     * 过滤依据为当前位置原始方块状态：
     * 黑名单命中则跳过，白名单存在时仅处理白名单命中的方块。
     * </p>
     */
    static void SetBlocks(
                          ServerLevel level,
                          Map<BlockPos, BlockState> changes,
                          boolean updateHeightmap,
                          boolean updateLight,
                          boolean syncToClient,
                          BlockSelection blacklist,
                          BlockSelection whitelist) {
        if (changes == null || changes.isEmpty()) return;

        Map<ChunkPos, Map<BlockPos, BlockState>> chunkGroups = changes.entrySet().stream()
                .collect(Collectors.groupingBy(
                        e -> new ChunkPos(
                                SectionPos.blockToSectionCoord(e.getKey().getX()),
                                SectionPos.blockToSectionCoord(e.getKey().getZ())),
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b)));

        fastSetBlocks(level, chunkGroups, updateHeightmap, updateLight, syncToClient, blacklist, whitelist);
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
        setAirBlocks(level, positions, updateHeightmap, updateLight, syncToClient, null, null);
    }

    /**
     * 批量设为空气，并支持黑白名单过滤。
     *
     * <p>
     * 过滤依据为当前位置原始方块状态：
     * 黑名单命中则跳过，白名单存在时仅处理白名单命中的方块。
     * </p>
     */
    static void setAirBlocks(
                             ServerLevel level,
                             List<BlockPos> positions,
                             boolean updateHeightmap,
                             boolean updateLight,
                             boolean syncToClient,
                             BlockSelection blacklist,
                             BlockSelection whitelist) {
        if (positions == null || positions.isEmpty()) return;

        Long2ObjectMap<LongList> chunkGroups = new Long2ObjectOpenHashMap<>();
        for (BlockPos pos : positions) {
            long key = ChunkPos.asLong(
                    SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getZ()));
            chunkGroups.computeIfAbsent(key, ignored -> new LongArrayList()).add(pos.asLong());
        }
        applyAirByChunk(level, chunkGroups, updateHeightmap, updateLight, syncToClient, blacklist, whitelist);
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
        setAirBlocksPacked(level, packed, count, updateHeightmap, updateLight, syncToClient, null, null);
    }

    /**
     * 批量设为空气（打包坐标缓冲），并支持黑白名单过滤。
     */
    static void setAirBlocksPacked(
                                   ServerLevel level,
                                   long[] packed,
                                   int count,
                                   boolean updateHeightmap,
                                   boolean updateLight,
                                   boolean syncToClient,
                                   BlockSelection blacklist,
                                   BlockSelection whitelist) {
        if (packed == null || count <= 0) return;

        Long2ObjectMap<LongList> chunkGroups = new Long2ObjectOpenHashMap<>();
        for (int i = 0; i < count; i++) {
            long p = packed[i];
            int x = BlockPos.getX(p);
            int z = BlockPos.getZ(p);
            long key = ChunkPos.asLong(
                    SectionPos.blockToSectionCoord(x),
                    SectionPos.blockToSectionCoord(z));
            chunkGroups.computeIfAbsent(key, ignored -> new LongArrayList()).add(p);
        }
        applyAirByChunk(level, chunkGroups, updateHeightmap, updateLight, syncToClient, blacklist, whitelist);
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
                                        boolean syncToClient,
                                        BlockSelection blacklist,
                                        BlockSelection whitelist) {
        Map<Block, Boolean> selectionCache = createSelectionCache(blacklist, whitelist);
        for (Long2ObjectMap.Entry<LongList> entry : chunkGroups.long2ObjectEntrySet()) {
            long key = entry.getLongKey();
            setAirBlocksInternal(
                    level,
                    ChunkPos.getX(key),
                    ChunkPos.getZ(key),
                    entry.getValue(),
                    updateHeightmap,
                    updateLight,
                    syncToClient,
                    blacklist,
                    whitelist,
                    selectionCache);
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
        fastSetBlocks(level, chunkGroups, updateHeightmap, updateLight, syncToClient, null, null);
    }

    static void fastSetBlocks(
                              ServerLevel level,
                              Map<ChunkPos, Map<BlockPos, BlockState>> chunkGroups,
                              boolean updateHeightmap,
                              boolean updateLight,
                              boolean syncToClient,
                              BlockSelection blacklist,
                              BlockSelection whitelist) {
        if (chunkGroups.isEmpty()) return;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Map<Block, Boolean> selectionCache = createSelectionCache(blacklist, whitelist);

        for (Map.Entry<ChunkPos, Map<BlockPos, BlockState>> chunkEntry : chunkGroups.entrySet()) {
            ChunkPos chunkPos = chunkEntry.getKey();
            Map<BlockPos, BlockState> chunkChanges = chunkEntry.getValue();
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);

            Short2IntMap columnMaxY = updateHeightmap ? new Short2IntOpenHashMap() : null;
            IntSet changedSections = updateLight ? new IntOpenHashSet() : null;
            LongList lightChangePositions = updateLight ? new LongArrayList() : null;
            List<BlockState> lightChangeOldStates = updateLight ? new ArrayList<>() : null;
            List<BlockState> lightChangeNewStates = updateLight ? new ArrayList<>() : null;
            List<BlockPos> syncPositions = syncToClient ? new ArrayList<>(chunkChanges.size()) : null;
            List<BlockState> syncOldStates = syncToClient ? new ArrayList<>(chunkChanges.size()) : null;
            List<BlockState> syncNewStates = syncToClient ? new ArrayList<>(chunkChanges.size()) : null;
            List<BlockPos> beToRemove = new ArrayList<>();
            List<Map.Entry<BlockPos, BlockState>> beToCreate = new ArrayList<>();
            int changedCount = 0;

            for (Map.Entry<BlockPos, BlockState> entry : chunkChanges.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();
                int y = pos.getY();
                if (level.isOutsideBuildHeight(y)) continue;

                int sectionIndex = chunk.getSectionIndex(y);
                LevelChunkSection section = chunk.getSection(sectionIndex);
                BlockState currentState = section.getBlockState(pos.getX() & 15, y & 15, pos.getZ() & 15);
                if (!matchesSelection(currentState, blacklist, whitelist, selectionCache)) {
                    continue;
                }
                int lx = pos.getX() & 15;
                int ly = y & 15;
                int lz = pos.getZ() & 15;

                boolean wasEmpty = section.hasOnlyAir();
                BlockState oldState = section.setBlockState(lx, ly, lz, state, false);
                if (oldState == state) continue;

                changedCount++;
                if (syncPositions != null) {
                    syncPositions.add(pos);
                    syncOldStates.add(currentState);
                    syncNewStates.add(state);
                }

                if (columnMaxY != null) {
                    short colKey = (short) ((lx << 4) | lz);
                    int curMax = columnMaxY.getOrDefault(colKey, Integer.MIN_VALUE);
                    if (y > curMax) columnMaxY.put(colKey, y);
                }

                if (updateLight) {
                    if (wasEmpty != section.hasOnlyAir()) {
                        changedSections.add(sectionIndex);
                    }
                    lightChangePositions.add(pos.asLong());
                    lightChangeOldStates.add(oldState);
                    lightChangeNewStates.add(state);
                }

                if (oldState.hasBlockEntity()) beToRemove.add(pos);
                if (state.hasBlockEntity()) beToCreate.add(entry);
            }

            applyHeightmapUpdates(chunk, chunkPos, columnMaxY, mutablePos);
            applyLightUpdates(level, chunk, chunkPos, changedSections,
                    lightChangePositions, lightChangeOldStates, lightChangeNewStates);

            for (BlockPos pos : beToRemove) {
                chunk.removeBlockEntity(pos);
            }
            for (Map.Entry<BlockPos, BlockState> entry : beToCreate) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();
                BlockEntity existing = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
                if (existing != null && existing.getType().isValid(state)) {
                    continue;
                }
                if (state.getBlock() instanceof EntityBlock entityBlock) {
                    BlockEntity be = entityBlock.newBlockEntity(pos, state);
                    if (be != null) chunk.addAndRegisterBlockEntity(be);
                }
            }

            chunk.setUnsaved(true);

            if (syncToClient && changedCount > 0) {
                syncChunkUpdates(level, chunk, chunkPos, syncPositions, syncOldStates, syncNewStates,
                        updateLight, changedCount);
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
                                             boolean syncToClient,
                                             BlockSelection blacklist,
                                             BlockSelection whitelist,
                                             Map<Block, Boolean> selectionCache) {
        if (positions.isEmpty()) return;

        final BlockState air = Blocks.AIR.defaultBlockState();
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        Short2IntMap columnMaxY = updateHeightmap ? new Short2IntOpenHashMap() : null;
        IntSet changedSections = updateLight ? new IntOpenHashSet() : null;
        LongList lightChangePositions = updateLight ? new LongArrayList() : null;
        List<BlockState> lightChangeOldStates = updateLight ? new ArrayList<>() : null;
        List<BlockState> lightChangeNewStates = updateLight ? new ArrayList<>() : null;
        LongList changedPositions = syncToClient ? new LongArrayList(positions.size()) : null;
        List<BlockState> syncOldStates = syncToClient ? new ArrayList<>(positions.size()) : null;
        List<BlockState> syncNewStates = syncToClient ? new ArrayList<>(positions.size()) : null;
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
            if (!matchesSelection(oldState, blacklist, whitelist, selectionCache)) {
                continue;
            }
            if (oldState.isAir()) {
                // 已是空气：跳过高度图（避免用高空空气污染列更新）
                continue;
            }

            boolean wasEmpty = section.hasOnlyAir();

            changedCount++;
            if (changedPositions != null) {
                changedPositions.add(packed);
                syncOldStates.add(oldState);
                syncNewStates.add(air);
            }
            if (oldState.hasBlockEntity()) {
                mutablePos.set(x, y, z);
                chunk.removeBlockEntity(mutablePos);
            }

            section.setBlockState(lx, ly, lz, air, false);

            if (updateLight) {
                if (wasEmpty != section.hasOnlyAir()) {
                    changedSections.add(secIdx);
                }
                lightChangePositions.add(packed);
                lightChangeOldStates.add(oldState);
                lightChangeNewStates.add(air);
            }

            if (columnMaxY != null) {
                short colKey = (short) ((lx << 4) | lz);
                int cur = columnMaxY.getOrDefault(colKey, Integer.MIN_VALUE);
                if (y > cur) columnMaxY.put(colKey, y);
            }
        }

        applyHeightmapUpdates(chunk, chunkPos, columnMaxY, mutablePos);
        applyLightUpdates(level, chunk, chunkPos, changedSections,
                lightChangePositions, lightChangeOldStates, lightChangeNewStates);
        chunk.setUnsaved(true);

        if (syncToClient && changedCount > 0) {
            syncChunkUpdatesPacked(level, chunk, chunkPos, changedPositions, syncOldStates, syncNewStates,
                    updateLight, changedCount);
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
                                          LongList lightChangePositions,
                                          List<BlockState> lightChangeOldStates,
                                          List<BlockState> lightChangeNewStates) {
        if (changedSections == null || lightChangePositions == null || lightChangeOldStates == null || lightChangeNewStates == null) return;

        LevelLightEngine engine = level.getChunkSource().getLightEngine();
        int minSectionY = chunk.getMinSection();

        IntSet sectionsToUpdate = new IntOpenHashSet();
        sectionsToUpdate.addAll(changedSections);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int lightChangeCount = Math.min(lightChangePositions.size(),
                Math.min(lightChangeOldStates.size(), lightChangeNewStates.size()));
        LongList lightCheckPositions = new LongArrayList(lightChangeCount);

        for (int i = 0; i < lightChangeCount; i++) {
            long packed = lightChangePositions.getLong(i);
            mutablePos.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            if (level.isOutsideBuildHeight(mutablePos.getY())) continue;
            if (LightEngine.hasDifferentLightProperties(chunk, mutablePos,
                    lightChangeOldStates.get(i), lightChangeNewStates.get(i))) {
                sectionsToUpdate.add(chunk.getSectionIndex(mutablePos.getY()));
                lightCheckPositions.add(packed);
            }
        }

        for (int idx : sectionsToUpdate) {
            if (idx < 0 || idx >= chunk.getSectionsCount()) continue;
            SectionPos sp = SectionPos.of(chunkPos, minSectionY + idx);
            boolean empty = chunk.getSection(idx).hasOnlyAir();
            engine.updateSectionStatus(sp, empty);
        }

        for (int i = 0, size = lightCheckPositions.size(); i < size; i++) {
            long packed = lightCheckPositions.getLong(i);
            mutablePos.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            if (level.isOutsideBuildHeight(mutablePos.getY())) continue;
            int idx = chunk.getSectionIndex(mutablePos.getY());
            if (!sectionsToUpdate.contains(idx)) continue;

            chunk.getSkyLightSources().update(chunk, mutablePos.getX() & 15, mutablePos.getY(), mutablePos.getZ() & 15);
            engine.checkBlock(mutablePos);
        }
    }

    /**
     * 网络同步：变化量大发全量区块包，否则发区段包 + BE 补偿。
     */
    private static void syncChunkUpdates(
                                         ServerLevel level,
                                         LevelChunk chunk,
                                         ChunkPos chunkPos,
                                         List<BlockPos> changedPositions,
                                         List<BlockState> oldStates,
                                         List<BlockState> newStates,
                                         boolean updateLight,
                                         int changedCount) {
        if (changedPositions == null || changedPositions.isEmpty()) return;

        if (changedCount >= WHOLE_CHUNK_THRESHOLD) {
            BitSet skyChanged = null;
            BitSet blockChanged = null;
            if (updateLight) {
                int minSection = chunk.getMinSection();
                int maxSection = chunk.getMaxSection();
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
            sendToTrackedPlayers(level, chunkPos, packet);
            return;
        }

        int syncCount = Math.min(changedPositions.size(), Math.min(oldStates.size(), newStates.size()));
        for (int i = 0; i < syncCount; i++) {
            BlockPos pos = changedPositions.get(i);
            BlockState oldState = oldStates.get(i);
            BlockState newState = newStates.get(i);
            level.sendBlockUpdated(pos, oldState, newState, Block.UPDATE_CLIENTS);
        }
    }

    private static void syncChunkUpdatesPacked(
                                               ServerLevel level,
                                               LevelChunk chunk,
                                               ChunkPos chunkPos,
                                               LongList changedPositions,
                                               List<BlockState> oldStates,
                                               List<BlockState> newStates,
                                               boolean updateLight,
                                               int changedCount) {
        if (changedPositions == null || changedPositions.isEmpty()) return;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        if (changedCount >= WHOLE_CHUNK_THRESHOLD) {
            BitSet skyChanged = null;
            BitSet blockChanged = null;
            if (updateLight) {
                int minSection = chunk.getMinSection();
                int maxSection = chunk.getMaxSection();
                int totalSections = maxSection - minSection + 1;
                skyChanged = new BitSet(totalSections);
                blockChanged = new BitSet(totalSections);
                for (int i = 0, size = changedPositions.size(); i < size; i++) {
                    long packed = changedPositions.getLong(i);
                    mutablePos.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
                    int secIdx = chunk.getSectionIndex(mutablePos.getY());
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
            sendToTrackedPlayers(level, chunkPos, packet);
            return;
        }

        int syncCount = Math.min(changedPositions.size(), Math.min(oldStates.size(), newStates.size()));
        for (int i = 0; i < syncCount; i++) {
            long packed = changedPositions.getLong(i);
            mutablePos.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            BlockState oldState = oldStates.get(i);
            BlockState newState = newStates.get(i);
            level.sendBlockUpdated(mutablePos, oldState, newState, Block.UPDATE_CLIENTS);
        }
    }

    private static void sendToTrackedPlayers(ServerLevel level, ChunkPos chunkPos, ClientboundLevelChunkWithLightPacket packet) {
        List<ServerPlayer> players = level.getChunkSource().chunkMap.getPlayers(chunkPos, false);
        if (players.isEmpty()) return;
        for (ServerPlayer player : players) {
            player.connection.send(packet);
        }
    }

    private static Map<Block, Boolean> createSelectionCache(BlockSelection blacklist, BlockSelection whitelist) {
        return hasTagRules(blacklist) || hasTagRules(whitelist) ? new HashMap<>() : null;
    }

    private static boolean hasTagRules(BlockSelection selection) {
        return selection != null && selection.tags() != null && !selection.tags().isEmpty();
    }

    private static boolean matchesSelection(BlockState state,
                                            BlockSelection blacklist,
                                            BlockSelection whitelist,
                                            Map<Block, Boolean> cache) {
        if (state == null) return false;

        Block block = state.getBlock();
        if (cache != null) {
            Boolean cached = cache.get(block);
            if (cached != null) {
                return cached;
            }
        }

        boolean result = !matchesAny(state, blacklist) && (whitelist == null || matchesAny(state, whitelist));
        if (cache != null) {
            cache.put(block, result);
        }
        return result;
    }

    private static boolean matchesAny(BlockState state, BlockSelection selection) {
        if (selection == null) return false;
        if (selection.blocks() != null && selection.blocks().contains(state.getBlock())) {
            return true;
        }
        Set<TagKey<Block>> tags = selection.tags();
        if (tags != null) {
            for (TagKey<Block> tag : tags) {
                if (tag != null && state.is(tag)) return true;
            }
        }
        return false;
    }
}
