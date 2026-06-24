package com.maple.quantum_chromodynamic_charge.explosion;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 爆炸类共用的特效、实体处理与分批常量。
 */
final class ExplosionSupport {

    /** 单 tick 最多提交的方块数，避免 TPS 尖刺与 GC 峰值 */
    static final int MAX_BLOCKS_PER_TICK = 50_000;

    private ExplosionSupport() {}

    static void playExplosionEffects(ServerLevel level, double x, double y, double z, boolean spawnParticles) {
        float pitch = (1.0f + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2f) * 0.7f;
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0f, pitch);
        if (spawnParticles) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.gameEvent(null, GameEvent.EXPLODE, new Vec3(x, y, z));
    }

    static void killLivingIn(ServerLevel level, AABB box) {
        for (Entity entity : level.getEntities(null, box)) {
            if (entity instanceof Player player && player.gameMode() == GameType.CREATIVE) {
                continue;
            }
            if (entity instanceof LivingEntity living) {
                living.kill(level);
            }
        }
    }

    /**
     * 按区域高度估算 X/Z 方向单步跨度，高度越大水平切片越窄。
     */
    static int areaSpeedZ(int height) {
        if (height < 80) return 64;
        if (height < 160) return 32;
        if (height < 300) return 16;
        if (height < 600) return 8;
        if (height < 1200) return 4;
        if (height < 2400) return 2;
        return 1;
    }

    static int areaSpeedX(int height) {
        if (height < 20) return 128;
        if (height < 40) return 64;
        if (height < 80) return 32;
        return 16;
    }

    /**
     * 厚球壳批包含的单位壳层数（实心 0～50 之后）：
     */
    static int sphereShellBatchSize(int fromLayer) {
        if (fromLayer < 500) {
            return 8;
        }
        return 16;
    }

    /** 区块爆炸：按世界高度决定每 tick 沿 Z 推进的格数 */
    static int chunkZSpeed(int worldHeight) {
        if (worldHeight < 600) return 16;
        if (worldHeight < 1200) return 8;
        if (worldHeight < 2400) return 4;
        return 1;
    }

    /**
     * 精确整数平方根：最大 n 满足 n² ≤ value。
     * 使用 long 避免大半径下 R² 溢出 int。
     */
    static int floorSqrt(long value) {
        if (value <= 0L) return 0;
        if (value >= (long) Integer.MAX_VALUE * (long) Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        int n = (int) Math.sqrt(value);
        while ((long) (n + 1) * (n + 1) <= value) n++;
        while ((long) n * n > value) n--;
        return n;
    }

    /** 最小 n ≥ 0 满足 n² ≥ value（用于球壳环带 y 下界） */
    static int ceilSqrt(long value) {
        if (value <= 0L) return 0;
        int n = floorSqrt(value);
        if ((long) n * n == value) return n;
        return n + 1;
    }
}
