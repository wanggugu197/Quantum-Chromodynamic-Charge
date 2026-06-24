package com.maple.quantum_chromodynamic_charge.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.*;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public class BigPrimedTnt extends PrimedTnt {

    public static final int DEFAULT_FUSE = 80;

    private float explosionPower;
    private boolean forceNoDrops;
    private boolean hasUsedPortal;
    private @Nullable LivingEntity owner;

    private static final ExplosionDamageCalculator PORTAL_DAMAGE_CALCULATOR = new ExplosionDamageCalculator() {

        @Override
        public boolean shouldBlockExplode(@NonNull Explosion explosion, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull BlockState state, float power) {
            return !state.is(Blocks.NETHER_PORTAL) && super.shouldBlockExplode(explosion, level, pos, state, power);
        }

        @Override
        public @NonNull Optional<Float> getBlockExplosionResistance(@NonNull Explosion explosion, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull BlockState block, @NonNull FluidState fluid) {
            return block.is(Blocks.NETHER_PORTAL) ? Optional.empty() : super.getBlockExplosionResistance(explosion, level, pos, block, fluid);
        }
    };

    private static final WeightedList<ExplosionParticleInfo> DEFAULT_BLOCK_PARTICLES = WeightedList.<ExplosionParticleInfo>builder()
            .add(new ExplosionParticleInfo(ParticleTypes.POOF, 0.5F, 1.0F))
            .add(new ExplosionParticleInfo(ParticleTypes.SMOKE, 1.0F, 1.0F))
            .build();

    // ==================== 构造方法 ====================

    public BigPrimedTnt(EntityType<? extends PrimedTnt> entityType, Level level) {
        super(entityType, level);
        this.explosionPower = 4.0F;
        this.forceNoDrops = false;
        this.setBlockState(Blocks.TNT.defaultBlockState());
    }

    public BigPrimedTnt(EntityType<? extends PrimedTnt> entityType, Level level, double x, double y, double z,
                        float explosionPower, BlockState blockState, boolean forceNoDrops, @Nullable LivingEntity owner) {
        super(entityType, level);
        this.explosionPower = explosionPower;
        this.forceNoDrops = forceNoDrops;
        this.owner = owner;
        this.setPos(x, y, z);
        double rot = level.getRandom().nextDouble() * (double) ((float) Math.PI * 2F);
        this.setDeltaMovement(-Math.sin(rot) * 0.02, 0.2F, -Math.cos(rot) * 0.02);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setBlockState(blockState);
        this.setFuse(DEFAULT_FUSE);
    }

    // ==================== 核心重写 ====================

    @Override
    public @Nullable LivingEntity getOwner() {
        return this.owner;
    }

    @Override
    protected void explode() {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
                this.explode(this.level(), this, this.getX(), this.getY(0.0625), this.getZ(),
                        this.explosionPower, this.forceNoDrops, this.hasUsedPortal);
            }
        }
    }

    protected void explode(Level level, @Nullable Entity source, double x, double y, double z,
                           float radius, boolean forceNoDrops, boolean usedPortal) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (forceNoDrops) {
            Vec3 center = new Vec3(x, y, z);
            ServerExplosion explosion = new ServerExplosion(
                    serverLevel, source,
                    Explosion.getDefaultDamageSource(level, source),
                    usedPortal ? PORTAL_DAMAGE_CALCULATOR : null,
                    center, radius, false,
                    BlockInteraction.DESTROY);
            if (net.neoforged.neoforge.event.EventHooks.onExplosionStart(serverLevel, explosion)) return;

            int blockCount = explosion.explode();
            ParticleOptions explosionParticle = explosion.isSmall() ? ParticleTypes.EXPLOSION : ParticleTypes.EXPLOSION_EMITTER;

            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(center) < 4096.0) {
                    Optional<Vec3> playerKnockback = Optional.ofNullable(explosion.getHitPlayers().get(player));
                    player.connection.send(new ClientboundExplodePacket(
                            center, radius, blockCount, playerKnockback,
                            explosionParticle, SoundEvents.GENERIC_EXPLODE,
                            DEFAULT_BLOCK_PARTICLES));
                }
            }
        } else {
            level.explode(
                    source,
                    Explosion.getDefaultDamageSource(level, source),
                    usedPortal ? PORTAL_DAMAGE_CALCULATOR : null,
                    x, y, z, radius,
                    false,
                    Level.ExplosionInteraction.TNT);
        }
    }

    // ==================== 传送门 ====================

    @Nullable
    @Override
    public Entity teleport(@NonNull TeleportTransition transition) {
        Entity newEntity = super.teleport(transition);
        if (newEntity instanceof BigPrimedTnt bigTnt) {
            bigTnt.hasUsedPortal = true;
        }
        return newEntity;
    }

    // ==================== 存档 ====================

    @Override
    protected void addAdditionalSaveData(@NonNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("qcc_explosion_power", this.explosionPower);
        output.putBoolean("qcc_force_no_drops", this.forceNoDrops);
        output.putBoolean("qcc_used_portal", this.hasUsedPortal);
    }

    @Override
    protected void readAdditionalSaveData(@NonNull ValueInput input) {
        super.readAdditionalSaveData(input);
        this.explosionPower = input.getFloatOr("qcc_explosion_power", 4.0F);
        this.forceNoDrops = input.getBooleanOr("qcc_force_no_drops", false);
        this.hasUsedPortal = input.getBooleanOr("qcc_used_portal", false);
    }
}
