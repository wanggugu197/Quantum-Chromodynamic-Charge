package com.maple.quantum_chromodynamic_charge.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import com.mapleutillib.utils.task.TickableSubscription;

import java.util.function.IntConsumer;

import static com.maple.quantum_chromodynamic_charge.common.QCCLevelTask.TASKS;
import static com.maple.quantum_chromodynamic_charge.explosion.ExplosionSupport.*;

/**
 * 以区块为单元、螺旋向外的大范围清除。
 * 支持进度回调和完成回调。
 */
public final class ChunkExplosion {

    private final BlockPos center;
    private final ServerLevel level;
    private final boolean updateHeightmap;
    private final boolean updateLight;
    private final int speed;
    private final int stepsPerChunk;
    private final int totalTime;
    private final int minY;
    private final int maxY;
    private final long[] buffer = new long[MAX_BLOCKS_PER_TICK];
    private final IntConsumer onProgress;
    private final Runnable onFinished;

    private int time = 0;
    private boolean stepActive = false;
    private int stepXEnd;
    private int stepXStep;
    private int stepZStart, stepZEnd, stepZStep;
    private int currentX, currentZ, currentY;
    private final TickableSubscription<?> subscription;

    private ChunkExplosion(BlockPos center, ServerLevel level, int chunkSide,
                           boolean updateHeightmap, boolean updateLight,
                           boolean spawnParticles, boolean affectEntities,
                           IntConsumer onProgress, Runnable onFinished) {
        this.center = center;
        this.level = level;
        this.updateHeightmap = updateHeightmap;
        this.updateLight = updateLight;
        this.onProgress = onProgress != null ? onProgress : (p -> {});
        this.onFinished = onFinished != null ? onFinished : () -> {};
        this.minY = level.getMinY();
        this.maxY = level.getMaxY();

        int worldHeight = this.maxY - this.minY + 1;
        this.speed = chunkZSpeed(worldHeight);
        this.stepsPerChunk = Math.max(1, 16 / this.speed);
        this.totalTime = chunkSide * chunkSide * this.stepsPerChunk;

        int x = center.getX(), y = center.getY(), z = center.getZ();
        playExplosionEffects(level, x + 0.5, y + 0.5, z + 0.5, spawnParticles);

        if (affectEntities) {
            int chunkX = x >> 4, chunkZ = z >> 4;
            int radius = (chunkSide - 1) / 2;
            int minCX = chunkX - radius, maxCX = chunkX + radius;
            int minCZ = chunkZ - radius, maxCZ = chunkZ + radius;
            AABB box = new AABB(
                    minCX << 4, this.minY, minCZ << 4,
                    (maxCX << 4) + 16, this.maxY + 1.0, (maxCZ << 4) + 16);
            killLivingIn(level, box);
        }

        subscription = TASKS.enqueueTick(level, this::breakBlocksInChunk, 0, 0);
    }

    private void breakBlocksInChunk() {
        if (time >= totalTime && !stepActive) {
            subscription.unsubscribe();
            onFinished.run();
            return;
        }

        if (!stepActive) {
            int chunkIndex = time / stepsPerChunk;
            int stepInChunk = time % stepsPerChunk;

            int[] spiral = getSpiralOffset(chunkIndex);
            int dx = spiral[0], dz = spiral[1];
            int cx = (center.getX() >> 4) + dx;
            int cz = (center.getZ() >> 4) + dz;

            int minX = cx << 4;
            int minZ = cz << 4;
            int chunkMaxX = minX + 15;
            int chunkMaxZ = minZ + 15;

            int xMin = minX;
            int xMax = chunkMaxX;
            if (dx > 0) {
                xMin = minX - 1;
            } else if (dx < 0) {
                xMax = chunkMaxX + 1;
            }

            int zMin = minZ;
            int zMax = chunkMaxZ;
            if (dz > 0) {
                zMin = minZ - 1;
            } else if (dz < 0) {
                zMax = chunkMaxZ + 1;
            }

            int stepXStart;
            if (dx >= 0) {
                stepXStart = xMin;
                stepXEnd = xMax + 1;
                stepXStep = 1;
            } else {
                stepXStart = xMax;
                stepXEnd = xMin - 1;
                stepXStep = -1;
            }

            int zBase = minZ + stepInChunk * speed;
            int zLow = zBase;
            int zHigh = Math.min(zBase + speed - 1, chunkMaxZ);
            if (dz > 0 && stepInChunk == 0) {
                zLow = zMin;
            }
            if (dz < 0 && stepInChunk == stepsPerChunk - 1) {
                zHigh = zMax;
            }

            if (dz >= 0) {
                stepZStart = zLow;
                stepZEnd = zHigh + 1;
                stepZStep = 1;
            } else {
                stepZStart = zHigh;
                stepZEnd = zLow - 1;
                stepZStep = -1;
            }

            currentX = stepXStart;
            currentZ = stepZStart;
            currentY = minY;
            stepActive = true;
        }

        int count = 0;
        outer:
        for (int x = currentX; x != stepXEnd; x += stepXStep) {
            int zFrom = (x == currentX) ? currentZ : stepZStart;
            for (int z = zFrom; z != stepZEnd; z += stepZStep) {
                int yFrom = (x == currentX && z == currentZ) ? currentY : minY;
                for (int y = yFrom; y <= maxY; y++) {
                    buffer[count++] = BlockPos.asLong(x, y, z);
                    if (count >= MAX_BLOCKS_PER_TICK) {
                        currentX = x;
                        currentZ = z;
                        currentY = y + 1;
                        break outer;
                    }
                }
            }
        }

        if (count > 0) {
            ILevel.setAirBlocksPacked(level, buffer, count, updateHeightmap, updateLight);
        }

        boolean stepFinished;
        if (count < MAX_BLOCKS_PER_TICK) {
            stepFinished = true;
        } else if (currentY > maxY) {
            int nextZ = currentZ + stepZStep;
            if (nextZ != stepZEnd) {
                currentZ = nextZ;
                currentY = minY;
                stepFinished = false;
            } else {
                int nextX = currentX + stepXStep;
                if (nextX != stepXEnd) {
                    currentX = nextX;
                    currentZ = stepZStart;
                    currentY = minY;
                    stepFinished = false;
                } else {
                    stepFinished = true;
                }
            }
        } else {
            stepFinished = false;
        }

        if (stepFinished) {
            stepActive = false;
            time++;
            int progress = (int) ((time * 100L) / totalTime);
            onProgress.accept(progress);
        }
    }

    private static int[] getSpiralOffset(int index) {
        if (index == 0) return new int[] { 0, 0 };
        int layer = (int) Math.ceil((Math.sqrt(index + 1) - 1) / 2.0);
        int legLen = layer * 2;
        int minInLayer = (2 * layer - 1) * (2 * layer - 1);
        int offset = index - minInLayer;
        int x, z;
        if (offset < legLen) {
            x = layer;
            z = -layer + offset;
        } else if (offset < 2 * legLen) {
            x = layer - (offset - legLen);
            z = layer;
        } else if (offset < 3 * legLen) {
            x = -layer;
            z = layer - (offset - 2 * legLen);
        } else {
            x = -layer + (offset - 3 * legLen);
            z = -layer;
        }
        return new int[] { x, z };
    }

    public static int blockExtentToChunkSide(int blockExtent) {
        int chunkSide = blockExtent / 8;
        if (chunkSide < 1) chunkSide = 1;
        if ((chunkSide & 1) == 0) chunkSide--;
        return chunkSide;
    }

    // ========== 静态入口（向后兼容） ==========

    public static void explosion(BlockPos center, Level level, int blockExtent,
                                 boolean updateHeightmap, boolean updateLight,
                                 boolean spawnParticles, boolean affectEntities) {
        explosion(center, level, blockExtent, updateHeightmap, updateLight, spawnParticles, affectEntities, null, null);
    }

    public static void explosion(BlockPos center, Level level, int blockExtent,
                                 boolean updateHeightmap, boolean updateLight,
                                 boolean spawnParticles, boolean affectEntities,
                                 IntConsumer onProgress, Runnable onFinished) {
        if (level instanceof ServerLevel serverLevel) {
            int chunkSide = blockExtentToChunkSide(blockExtent);
            new ChunkExplosion(center, serverLevel, chunkSide, updateHeightmap, updateLight,
                    spawnParticles, affectEntities, onProgress, onFinished);
        }
    }

    public static void explosionChunks(BlockPos center, Level level, int chunkSide,
                                       boolean updateHeightmap, boolean updateLight,
                                       boolean spawnParticles, boolean affectEntities) {
        explosionChunks(center, level, chunkSide, updateHeightmap, updateLight, spawnParticles, affectEntities, null, null);
    }

    public static void explosionChunks(BlockPos center, Level level, int chunkSide,
                                       boolean updateHeightmap, boolean updateLight,
                                       boolean spawnParticles, boolean affectEntities,
                                       IntConsumer onProgress, Runnable onFinished) {
        if (level instanceof ServerLevel serverLevel) {
            if (chunkSide < 1) chunkSide = 1;
            if ((chunkSide & 1) == 0) chunkSide--;
            new ChunkExplosion(center, serverLevel, chunkSide, updateHeightmap, updateLight,
                    spawnParticles, affectEntities, onProgress, onFinished);
        }
    }
}
