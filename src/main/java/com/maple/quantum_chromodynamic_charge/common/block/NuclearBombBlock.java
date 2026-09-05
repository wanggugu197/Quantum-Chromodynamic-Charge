package com.maple.quantum_chromodynamic_charge.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import org.jetbrains.annotations.Nullable;

public class NuclearBombBlock extends BigTntBlock {

    public NuclearBombBlock(Properties properties) {
        super(properties, 512.0F, true, true);
    }

    @Override
    protected NuclearBombEntity createEntity(Level level, double x, double y, double z,
                                             float explosionPower, BlockState blockState,
                                             boolean forceNoDrops, @Nullable LivingEntity owner) {
        return new NuclearBombEntity(level, x, y, z, blockState, owner);
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos,
                             @Nullable Direction face, @Nullable LivingEntity igniter) {
        if (level instanceof ServerLevel) {
            NuclearBombEntity primedTnt = createEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    2048.0F, this.defaultBlockState(), true, igniter);
            level.addFreshEntity(primedTnt);
            level.playSound(null, primedTnt.getX(), primedTnt.getY(), primedTnt.getZ(),
                    SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
        }
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        NuclearBombEntity primed = createEntity(level, pos.getX() + 0.5F, pos.getY(), pos.getZ() + 0.5F,
                2048.0F, this.defaultBlockState(), true, explosion.getIndirectSourceEntity());
        int fuse = primed.getFuse();
        primed.setFuse(level.getRandom().nextInt(fuse) + fuse / 4);
        level.addFreshEntity(primed);
    }
}
