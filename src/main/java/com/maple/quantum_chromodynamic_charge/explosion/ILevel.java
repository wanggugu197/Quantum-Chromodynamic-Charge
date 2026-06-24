package com.maple.quantum_chromodynamic_charge.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LightEngine;

public interface ILevel {

    /**
     * 快速移除指定位置的方块。
     * <p>
     * 绕过 {@link Level#setBlock} 直接操作 {@link LevelChunkSection}，
     * 仅更新高度图和条件性光照，不触发邻居更新和客户端同步。
     *
     * @param level                   世界
     * @param pos                     方块位置
     * @param updateHeightmapUnprimed 是否更新高度图
     * @param updateLight             是否更新光照（天光和方块光照）
     */
    static void fastRemoveBlock(ServerLevel level, BlockPos pos, boolean updateHeightmapUnprimed, boolean updateLight) {
        int y = pos.getY();
        if (level.isOutsideBuildHeight(y)) return;

        LevelChunk chunk = level.getChunkAt(pos);
        int sectionIndex = chunk.getSectionIndex(y);
        LevelChunkSection section = chunk.getSection(sectionIndex);
        if (section.hasOnlyAir()) return;

        int localX = pos.getX() & 15;
        int localY = y & 15;
        int localZ = pos.getZ() & 15;

        BlockState oldState = section.getBlockState(localX, localY, localZ);
        if (oldState.isAir()) return;

        // 在 section 层面置为空气（false = 跳过线程安全检查，更快）
        BlockState air = Blocks.AIR.defaultBlockState();
        section.setBlockState(localX, localY, localZ, air, false);

        // 更新高度图
        if (updateHeightmapUnprimed) {
            chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING).update(localX, y, localZ, air);
            chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(localX, y, localZ, air);
            chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR).update(localX, y, localZ, air);
            chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE).update(localX, y, localZ, air);
        }

        // Section 从非空变空时更新区块状态和区块光源
        if (updateLight && section.hasOnlyAir()) {
            level.getChunkSource().getLightEngine().updateSectionStatus(pos, true);
        }

        // 条件性更新天光和方块光照
        if (updateLight && LightEngine.hasDifferentLightProperties(level, pos, oldState, air)) {
            chunk.getSkyLightSources().update(level, localX, y, localZ);
            level.getChunkSource().getLightEngine().checkBlock(pos);
        }

        // 移除旧方块关联的 BlockEntity
        if (oldState.hasBlockEntity()) {
            level.removeBlockEntity(pos);
        }

        // 标记区块为未保存
        chunk.markUnsaved();
        // 通知客户端该方块位置发生变化
        level.sendBlockUpdated(pos, oldState, air, Block.UPDATE_CLIENTS);
    }
}
