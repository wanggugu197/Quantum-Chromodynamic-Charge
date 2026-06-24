package com.maple.quantum_chromodynamic_charge.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import com.mapleutillib.utils.task.TickableSubscription;

import static com.maple.quantum_chromodynamic_charge.common.QCCLevelTask.TASKS;
import static com.maple.quantum_chromodynamic_charge.explosion.ExplosionSupport.MAX_BLOCKS_PER_TICK;
import static com.maple.quantum_chromodynamic_charge.explosion.ExplosionSupport.chunkZSpeed;
import static com.maple.quantum_chromodynamic_charge.explosion.ExplosionSupport.killLivingIn;
import static com.maple.quantum_chromodynamic_charge.explosion.ExplosionSupport.playExplosionEffects;

/**
 * 以区块为单元、螺旋向外的大范围清除。
 * <p>
 * <b>尺寸换算（务必阅读）</b>：
 * 
 * <pre>
 *   调用方传入 blockExtent（方块尺度的“边长意图”，例如 800）
 *     → chunkSide = max(1, blockExtent / 8)
 *     → 若为偶数则减 1，强制奇数
 *     → 实际清除 (chunkSide × chunkSide) 个区块，中心对齐
 *   例：800 → 100 → 99 → 99×99 区块
 * </pre>
 * 
 * 非中心区块朝爆炸中心一侧多清 1 格；单 tick 超过 {@link ExplosionSupport#MAX_BLOCKS_PER_TICK} 断点续传。
 * {@code updateLight=false} 可关闭过程中光照更新。
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

    private int time = 0;
    private boolean stepActive = false;
    private int stepXEnd;
    private int stepXStep;
    private int stepZStart, stepZEnd, stepZStep;
    private int currentX, currentZ, currentY;
    private final TickableSubscription<?> subscription;

    private ChunkExplosion(BlockPos center, ServerLevel level, int chunkSide,
                           boolean updateHeightmap, boolean updateLight,
                           boolean spawnParticles, boolean affectEntities) {
        this.center = center;
        this.level = level;
        this.updateHeightmap = updateHeightmap;
        this.updateLight = updateLight;
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
                // Y 已是世界 min～max，无需再钳制
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

    /**
     * 将调用方“方块尺度边长意图”换算为奇数区块边长。
     * 
     * <pre>
     *   chunkSide = max(1, blockExtent / 8)
     *   if even → chunkSide--
     * </pre>
     */
    public static int blockExtentToChunkSide(int blockExtent) {
        int chunkSide = blockExtent / 8;
        if (chunkSide < 1) chunkSide = 1;
        if ((chunkSide & 1) == 0) chunkSide--;
        return chunkSide;
    }

    // ---------- 按方块尺度边长（兼容旧调用，如 800）----------

    /**
     * @param blockExtent 方块尺度边长意图，内部 {@link #blockExtentToChunkSide(int)}
     * @param updateLight 过程中是否更新光照
     */
    public static void explosion(BlockPos center, Level level, int blockExtent,
                                 boolean updateHeightmap, boolean updateLight,
                                 boolean spawnParticles, boolean affectEntities) {
        if (level instanceof ServerLevel serverLevel) {
            int chunkSide = blockExtentToChunkSide(blockExtent);
            new ChunkExplosion(center, serverLevel, chunkSide, updateHeightmap, updateLight,
                    spawnParticles, affectEntities);
        }
    }

    // ---------- 直接指定奇数区块边长 ----------

    /**
     * 按<strong>区块边长</strong>清除（已是区块数，不再 /8）。
     * 若为偶数会自动减 1；小于 1 则为 1。
     *
     * @param chunkSide   区块边长（建议奇数）
     * @param updateLight 过程中是否更新光照
     */
    public static void explosionChunks(BlockPos center, Level level, int chunkSide,
                                       boolean updateHeightmap, boolean updateLight,
                                       boolean spawnParticles, boolean affectEntities) {
        if (level instanceof ServerLevel serverLevel) {
            if (chunkSide < 1) chunkSide = 1;
            if ((chunkSide & 1) == 0) chunkSide--;
            new ChunkExplosion(center, serverLevel, chunkSide, updateHeightmap, updateLight,
                    spawnParticles, affectEntities);
        }
    }
}
