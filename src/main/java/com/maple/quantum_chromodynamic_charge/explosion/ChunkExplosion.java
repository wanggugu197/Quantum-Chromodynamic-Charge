package com.maple.quantum_chromodynamic_charge.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mapleutillib.utils.task.TaskHandler;
import com.mapleutillib.utils.task.TickableSubscription;

import java.util.List;

public final class ChunkExplosion {

    private final BlockPos center;
    private final ServerLevel level;
    private final boolean breakBedrock;
    private final int sideLength;
    private int time = 0;
    private final TickableSubscription<?> subscription;

    private ChunkExplosion(BlockPos center, ServerLevel level, int sideLength, boolean breakBedrock, boolean spawnParticles, boolean affectEntities) {
        this.center = center;
        this.level = level;
        this.breakBedrock = breakBedrock;
        this.sideLength = sideLength;

        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();

        if (this.level.isClientSide()) {
            float soundPitch = (1.0f + (this.level.getRandom().nextFloat() - this.level.getRandom().nextFloat()) * 0.2f) * 0.7f;
            this.level.playLocalSound(x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0f, soundPitch, false);
        }

        if (spawnParticles) {
            this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0, 0.0, 0.0);
        }

        this.level.gameEvent(null, GameEvent.EXPLODE, new Vec3(x, y, z));

        if (affectEntities) {
            int chunkX = x >> 4;
            int chunkZ = z >> 4;

            int chunkRadius = (sideLength - 1) / 2;
            int minChunkX = chunkX - chunkRadius;
            int maxChunkX = chunkX + chunkRadius;
            int minChunkZ = chunkZ - chunkRadius;
            int maxChunkZ = chunkZ + chunkRadius;

            int minX = minChunkX << 4;
            int maxX = (maxChunkX << 4) + 15;
            int minZ = minChunkZ << 4;
            int maxZ = (maxChunkZ << 4) + 15;
            int minY = level.getMinY();
            int maxY = level.getMaxY();

            List<Entity> entities = this.level.getEntities(null, new AABB(minX, minY, minZ, maxX, maxY, maxZ));

            for (Entity entity : entities) {
                if (entity instanceof Player player && player.gameMode() == GameType.CREATIVE) continue;
                entity.kill(level);
            }
        }

        subscription = TaskHandler.enqueueTick(level, this::breakBlocksInChunk, 0, 0);
    }

    private void breakBlocksInChunk() {
        // 根据高度确定处理速度
        int height = level.getHeight();
        int speed = height < 600 ? 8 : height < 1200 ? 4 : height < 2400 ? 2 : 1;

        // 每个区块分几步处理
        int stepsPerChunk = 16 / speed;
        int totalTime = sideLength * sideLength * stepsPerChunk;

        if (time >= totalTime) {
            subscription.unsubscribe();
            return;
        }

        // 当前区块索引 + 区块内步数
        int chunkIndex = time / stepsPerChunk;
        int stepInChunk = time % stepsPerChunk;

        // 获取螺旋顺序的相对坐标
        int[] spiralPos = getSpiralOffset(chunkIndex);
        int dxChunk = spiralPos[0];
        int dzChunk = spiralPos[1];

        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;

        int chunkX = centerChunkX + dxChunk;
        int chunkZ = centerChunkZ + dzChunk;

        // 区块基准坐标
        int minX = (chunkX << 4);
        int minZ = (chunkZ << 4);
        int maxY = level.getMaxY();
        int minY = level.getMinY();

        // 决定 X 遍历顺序
        int xStart = (dxChunk >= 0 ? minX : minX + 15);
        int xEnd = (dxChunk >= 0 ? minX + 16 : minX - 1);
        int xStep = (dxChunk >= 0 ? 1 : -1);

        // 决定 Z 遍历顺序（基于 stepInChunk 分块）
        int zBase = minZ + stepInChunk * speed;
        int zStart = (dzChunk >= 0 ? zBase : zBase + speed - 1);
        int zEnd = (dzChunk >= 0 ? zBase + speed : zBase - 1);
        int zStep = (dzChunk >= 0 ? 1 : -1);

        // 确保不超出当前区块边界
        int zBound = (chunkZ + 1) << 4;
        if (dzChunk >= 0 && zEnd > zBound) zEnd = zBound;
        if (dzChunk < 0 && zEnd < minZ - 1) zEnd = minZ - 1;

        // 破坏区块内的方块（双向扫描）
        for (int x = xStart; x != xEnd; x += xStep) {
            for (int z = zStart; z != zEnd; z += zStep) {
                for (int y = minY; y <= maxY; y++) {
                    ILevel.fastRemoveBlock(level, new BlockPos(x, y, z), breakBedrock, false);
                }
            }
        }

        time++;
    }

    /**
     * 获取螺旋顺序中的偏移坐标
     * index=0 → (0,0)
     * index=1 → (1,0)
     * index=2 → (1,1)
     * index=3 → (0,1)
     * index=4 → (-1,1)
     * ...
     */
    private int[] getSpiralOffset(int index) {
        if (index == 0) return new int[] { 0, 0 };

        int layer = (int) Math.ceil((Math.sqrt(index + 1) - 1) / 2);
        int legLen = layer * 2;
        int minIndexInLayer = (2 * layer - 1) * (2 * layer - 1);
        int offset = index - minIndexInLayer;

        int x, z;
        if (offset < legLen) {                // 右边往上
            x = layer;
            z = -layer + offset;
        } else if (offset < 2 * legLen) {     // 上边往左
            x = layer - (offset - legLen);
            z = layer;
        } else if (offset < 3 * legLen) {     // 左边往下
            x = -layer;
            z = layer - (offset - 2 * legLen);
        } else {                              // 下边往右
            x = -layer + (offset - 3 * legLen);
            z = -layer;
        }
        return new int[] { x, z };
    }

    public static void explosion(BlockPos center, Level level, int sideLength, boolean breakBedrock, boolean spawnParticles, boolean affectEntities) {
        if (level instanceof ServerLevel serverLevel) {
            sideLength /= 8;
            if (sideLength < 1) sideLength = 1;
            if (sideLength % 2 == 0) sideLength--;

            new ChunkExplosion(center, serverLevel, sideLength, breakBedrock, spawnParticles, affectEntities);
        }
    }
}
