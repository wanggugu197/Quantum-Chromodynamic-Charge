package com.maple.quantum_chromodynamic_charge.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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

public final class SphereExplosion {

    private final BlockPos center;
    private final ServerLevel level;
    private final int radius;
    private final boolean breakBedrock;
    private final TickableSubscription<?> subscription;

    private SphereExplosion(BlockPos center, ServerLevel level, int radius, boolean breakBedrock, boolean spawnParticles, boolean affectEntities) {
        this.center = center;
        this.level = level;
        this.radius = radius;
        this.breakBedrock = breakBedrock;

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
            float f2 = this.radius * 2.0f;
            List<Entity> entities = this.level.getEntities(null, new AABB(
                    Mth.floor(x - f2 - 1.0),
                    Mth.floor(y - f2 - 1.0),
                    Mth.floor(z - f2 - 1.0),
                    Mth.floor(x + f2 + 1.0),
                    Mth.floor(y + f2 + 1.0),
                    Mth.floor(z + f2 + 1.0)));

            for (Entity entity : entities) {
                if (entity instanceof Player player && player.gameMode() == GameType.CREATIVE) continue;
                entity.kill(level);
            }
        }

        subscription = TaskHandler.enqueueTick(level, this::breakBlocksInExplosionArea, 0, 0);
    }

    private int currentLayer = -1;
    private int currentY, currentX, currentZ;
    private boolean layerInitialized = false;
    private static final int MAX_BLOCKS_PER_TICK = 50000;

    private void breakBlocksInExplosionArea() {
        if (currentLayer < 0) {
            currentLayer = Math.min(radius, 19);
        }

        if (currentLayer > radius) {
            subscription.unsubscribe();
            return;
        }

        int layerEnd = currentLayer + 1;
        int endSq = layerEnd * layerEnd;
        int startSq = currentLayer > 20 ? (currentLayer - 1) * (currentLayer - 1) : 0;

        int maxY = level.getMaxY();
        int minY = level.getMinY();

        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        int blocksProcessed = 0;

        // 0-50层依旧完整处理
        if (currentLayer <= 50) {
            for (int y = layerEnd; y >= -layerEnd; y--) { // 改为从上到下
                int worldY = centerY + y;
                if (worldY < minY || worldY > maxY) continue;

                int ySq = y * y;
                if (ySq > endSq) continue;
                int remY = endSq - ySq;

                int xMax = (int) Math.sqrt(remY);
                for (int x = -xMax; x <= xMax; x++) {
                    int xSq = x * x;
                    int xySq = xSq + ySq;
                    if (xySq > endSq) continue;
                    int remX = remY - xSq;

                    int zMax = (int) Math.sqrt(remX);
                    for (int z = -zMax; z <= zMax; z++) {
                        int distSq = xySq + z * z;
                        if (distSq < startSq || distSq > endSq) continue;

                        ILevel.fastRemoveBlock(level, new BlockPos(centerX + x, centerY + y, centerZ + z), breakBedrock, false);
                    }
                }
            }
            currentLayer++;
            return;
        }

        if (!layerInitialized) {
            currentY = layerEnd;
            currentX = Integer.MIN_VALUE;
            currentZ = 0;
            layerInitialized = true;
        }

        for (int y = currentY; y >= -layerEnd; y--) {
            int worldY = centerY + y;
            if (worldY < minY || worldY > maxY) continue;

            int ySq = y * y;
            if (ySq > endSq) continue;
            int remY = endSq - ySq;

            int xMax = (int) Math.sqrt(remY);
            int startX = (currentX == Integer.MIN_VALUE ? -xMax : currentX);
            for (int x = startX; x <= xMax; x++) {
                int xSq = x * x;
                int xySq = xSq + ySq;
                if (xySq > endSq) continue;
                int remX = remY - xSq;

                int zMax = (int) Math.sqrt(remX);
                int startZ = (currentX == x ? currentZ : -zMax);
                for (int z = startZ; z <= zMax; z++) {
                    int distSq = xySq + z * z;
                    if (distSq < startSq || distSq > endSq) continue;

                    ILevel.fastRemoveBlock(level, new BlockPos(centerX + x, centerY + y, centerZ + z), breakBedrock, false);

                    blocksProcessed++;
                    if (blocksProcessed >= MAX_BLOCKS_PER_TICK) {
                        currentY = y;
                        currentX = x;
                        currentZ = z + 1;
                        return;
                    }
                }
                currentZ = -zMax;
            }
            currentX = Integer.MIN_VALUE;
        }

        currentLayer++;
        layerInitialized = false;
    }

    public static void explosion(BlockPos center, Level level, int radius, boolean breakBedrock, boolean spawnParticles) {
        explosion(center, level, radius, breakBedrock, spawnParticles, true);
    }

    public static void explosion(BlockPos center, Level level, int radius, boolean breakBedrock, boolean spawnParticles, boolean affectEntities) {
        if (level instanceof ServerLevel serverLevel) new SphereExplosion(center, serverLevel, radius, breakBedrock, spawnParticles, affectEntities);
    }
}
