package com.maple.quantum_chromodynamic_charge.explosion;

import net.minecraft.client.telemetry.TelemetryProperty;
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

public final class AreaExplosion {

    private final ServerLevel level;
    private final boolean breakBedrock;
    private final int minX, minY, minZ, maxX, maxY, maxZ;
    private int time = 0;
    private final TickableSubscription<?> subscription;

    private AreaExplosion(BlockPos center, BlockPos pos1, BlockPos pos2, ServerLevel level, boolean breakBedrock, boolean spawnParticles, boolean affectEntities) {
        this.level = level;
        this.breakBedrock = breakBedrock;

        // 计算区域边界
        this.minX = Math.min(pos1.getX(), pos2.getX());
        this.minY = Math.min(pos1.getY(), pos2.getY());
        this.minZ = Math.min(pos1.getZ(), pos2.getZ());
        this.maxX = Math.max(pos1.getX(), pos2.getX());
        this.maxY = Math.max(pos1.getY(), pos2.getY());
        this.maxZ = Math.max(pos1.getZ(), pos2.getZ());

        // 计算中心点（用于声音和粒子效果）
        int X = center.getX();
        int Y = center.getY();
        int Z = center.getZ();

        if (this.level.isClientSide()) {
            float soundPitch = (1.0f + (this.level.getRandom().nextFloat() - this.level.getRandom().nextFloat()) * 0.2f) * 0.7f;
            this.level.playLocalSound(X, Y, Z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0f, soundPitch, false);
        }

        if (spawnParticles) {
            this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, X, Y, Z, 1.0, 0.0, 0.0);
        }

        this.level.gameEvent(null, GameEvent.EXPLODE, new Vec3(X, Y, Z));

        if (affectEntities) {
            AABB affectBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
            List<Entity> entities = this.level.getEntities(null, affectBox);

            for (Entity entity : entities) {
                if (entity instanceof Player player && player.gameMode() == GameType.CREATIVE) continue;
                entity.kill(level);
            }
        }

        subscription = TaskHandler.enqueueTick(level, this::breakBlocksInArea, 0, 0);
    }

    private void breakBlocksInArea() {
        // 根据高度确定处理速度
        int height = maxY - minY;
        int speedZ = height < 80 ? 64 : height < 160 ? 32 : height < 300 ? 16 : height < 600 ? 8 : height < 1200 ? 4 : height < 2400 ? 2 : 1;
        int speedX = height < 20 ? 128 : height < 40 ? 64 : height < 80 ? 32 : 16;

        int timeX = (int) Math.ceil((double) (maxX - minX) / speedX);
        int timeZ = (int) Math.ceil((double) (maxZ - minZ) / speedZ);

        int totalTime = timeX * timeZ;

        if (time >= totalTime) {
            subscription.unsubscribe();
            return;
        }

        // 计算当前步骤的索引
        int indexX = time / timeZ;
        int indexZ = time % timeZ;
        int chunkRow = time / timeX;
        // 计算当前区块的范围
        int startX = minX + indexX * speedX;
        int endX = Math.min(startX + speedX, maxX);
        int startZ = minZ + indexZ * speedZ;
        int endZ = Math.min(startZ + speedZ, maxZ);

        // 破坏当前区块内的方块
        for (int x = startX - (chunkRow == 0 ? 0 : 1); x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    ILevel.fastRemoveBlock(level, new BlockPos(x, y, z), breakBedrock, false);
                }
            }
        }

        time++;
    }

    public static void explosion(BlockPos center, BlockPos pos1, BlockPos pos2, Level level, boolean breakBedrock, boolean spawnParticles, boolean affectEntities) {
        if (level instanceof ServerLevel serverLevel) new AreaExplosion(center, pos1, pos2, serverLevel, breakBedrock, spawnParticles, affectEntities);
    }
}
