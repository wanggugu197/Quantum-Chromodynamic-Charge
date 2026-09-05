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
 * 轴对齐矩形区域分步清除。
 * X/Z 方向相邻条带在接缝处各重叠 1 格，把边界清得更干净。
 * 支持进度回调和完成回调。
 */
public final class AreaExplosion {

    private final ServerLevel level;
    private final boolean updateHeightmap;
    private final boolean updateLight;
    private final int minX, minY, minZ, maxX, maxY, maxZ;
    private final int speedX, speedZ;
    private final int timeX, timeZ, totalTime;
    private final long[] buffer = new long[MAX_BLOCKS_PER_TICK];
    private final IntConsumer onProgress;
    private final Runnable onFinished;

    private int time = 0;
    private final TickableSubscription<?> subscription;
    private boolean stepActive = false;
    private int stepEndX;
    private int stepStartZ;
    private int stepEndZ;
    private int currentX, currentZ, currentY;

    private AreaExplosion(BlockPos center, BlockPos pos1, BlockPos pos2, ServerLevel level,
                          boolean updateHeightmap, boolean updateLight,
                          boolean spawnParticles, boolean affectEntities,
                          IntConsumer onProgress, Runnable onFinished) {
        this.level = level;
        this.updateHeightmap = updateHeightmap;
        this.updateLight = updateLight;
        this.onProgress = onProgress != null ? onProgress : (p -> {});
        this.onFinished = onFinished != null ? onFinished : () -> {};

        this.minX = Math.min(pos1.getX(), pos2.getX());
        this.minY = Math.min(pos1.getY(), pos2.getY());
        this.minZ = Math.min(pos1.getZ(), pos2.getZ());
        this.maxX = Math.max(pos1.getX(), pos2.getX());
        this.maxY = Math.max(pos1.getY(), pos2.getY());
        this.maxZ = Math.max(pos1.getZ(), pos2.getZ());

        int height = this.maxY - this.minY + 1;
        this.speedZ = areaSpeedZ(height);
        this.speedX = areaSpeedX(height);

        int sizeX = this.maxX - this.minX + 1;
        int sizeZ = this.maxZ - this.minZ + 1;
        this.timeX = Math.max(1, (int) Math.ceil(sizeX / (double) this.speedX));
        this.timeZ = Math.max(1, (int) Math.ceil(sizeZ / (double) this.speedZ));
        this.totalTime = this.timeX * this.timeZ;

        playExplosionEffects(level, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, spawnParticles);

        if (affectEntities) {
            killLivingIn(level, new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1));
        }

        subscription = TASKS.enqueueTick(level, this::breakBlocksInArea, 0, 0);
    }

    private void breakBlocksInArea() {
        if (time >= totalTime && !stepActive) {
            subscription.unsubscribe();
            onFinished.run();
            return;
        }

        if (!stepActive) {
            int indexX = time / timeZ;
            int indexZ = time % timeZ;

            int baseStartX = minX + indexX * speedX;
            int stepStartX = indexX == 0 ? baseStartX : Math.max(minX, baseStartX - 1);
            stepEndX = (indexX == timeX - 1) ? maxX : Math.min(baseStartX + speedX - 1, maxX);

            int baseStartZ = minZ + indexZ * speedZ;
            stepStartZ = indexZ == 0 ? baseStartZ : Math.max(minZ, baseStartZ - 1);
            stepEndZ = (indexZ == timeZ - 1) ? maxZ : Math.min(baseStartZ + speedZ - 1, maxZ);

            currentX = stepStartX;
            currentZ = stepStartZ;
            currentY = minY;
            stepActive = true;
        }

        int worldMinY = level.getMinBuildHeight();
        int worldMaxY = level.getMaxBuildHeight();
        int scanMinY = Math.max(minY, worldMinY);
        int yEnd = Math.min(maxY, worldMaxY);

        int count = 0;
        outer:
        for (int x = currentX; x <= stepEndX; x++) {
            for (int z = (x == currentX ? currentZ : stepStartZ); z <= stepEndZ; z++) {
                int y0 = (x == currentX && z == currentZ) ? currentY : minY;
                int yStart = Math.max(y0, scanMinY);
                if (yStart > yEnd) {
                    continue;
                }
                for (int y = yStart; y <= yEnd; y++) {
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
        } else if (currentY > maxY || currentY > yEnd) {
            if (currentZ < stepEndZ) {
                currentZ++;
                currentY = minY;
                stepFinished = false;
            } else if (currentX < stepEndX) {
                currentX++;
                currentZ = stepStartZ;
                currentY = minY;
                stepFinished = false;
            } else {
                stepFinished = true;
            }
        } else {
            stepFinished = false;
        }

        if (stepFinished) {
            stepActive = false;
            time++;
            // 进度回调：0～100
            int progress = (int) ((time * 100L) / totalTime);
            onProgress.accept(progress);
        }
    }

    // ========== 静态入口 ==========

    /** 原有方法（无回调） */
    public static void explosion(BlockPos center, BlockPos pos1, BlockPos pos2, Level level,
                                 boolean updateHeightmap, boolean updateLight,
                                 boolean spawnParticles, boolean affectEntities) {
        explosion(center, pos1, pos2, level, updateHeightmap, updateLight, spawnParticles, affectEntities, null, null);
    }

    /** 新方法：带进度和完成回调 */
    public static void explosion(BlockPos center, BlockPos pos1, BlockPos pos2, Level level,
                                 boolean updateHeightmap, boolean updateLight,
                                 boolean spawnParticles, boolean affectEntities,
                                 IntConsumer onProgress, Runnable onFinished) {
        if (level instanceof ServerLevel serverLevel) {
            new AreaExplosion(center, pos1, pos2, serverLevel, updateHeightmap, updateLight,
                    spawnParticles, affectEntities, onProgress, onFinished);
        }
    }
}
