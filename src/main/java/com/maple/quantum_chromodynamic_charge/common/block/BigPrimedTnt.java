package com.maple.quantum_chromodynamic_charge.common.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public class BigPrimedTnt extends PrimedTnt {

    public static final int DEFAULT_FUSE = 80;

    private float explosionPower;
    private boolean forceNoDrops;
    private @Nullable LivingEntity owner;

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
            serverLevel.explode(
                    this,
                    this.getX(),
                    this.getY(0.0625),
                    this.getZ(),
                    this.explosionPower,
                    false,
                    this.forceNoDrops ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.TNT);
        }
    }

    // ==================== 存档 ====================

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("qcc_explosion_power", this.explosionPower);
        tag.putBoolean("qcc_force_no_drops", this.forceNoDrops);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.explosionPower = tag.contains("qcc_explosion_power") ? tag.getFloat("qcc_explosion_power") : 4.0F;
        this.forceNoDrops = tag.getBoolean("qcc_force_no_drops");
    }
}
