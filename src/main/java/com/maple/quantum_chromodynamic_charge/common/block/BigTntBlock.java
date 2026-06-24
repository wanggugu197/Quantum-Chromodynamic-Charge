package com.maple.quantum_chromodynamic_charge.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import static com.maple.quantum_chromodynamic_charge.common.QCCRegistration.ENTITY_QCC_TNT;

public class BigTntBlock extends TntBlock {

    private final float explosionPower;
    private final boolean forceNoDrops;
    private final boolean explodeOnDestroy;

    public BigTntBlock(Properties properties, float explosionPower, boolean forceNoDrops, boolean explodeOnDestroy) {
        super(properties);
        this.explosionPower = explosionPower;
        this.forceNoDrops = forceNoDrops;
        this.explodeOnDestroy = explodeOnDestroy;
    }

    @Override
    protected @NonNull InteractionResult useItemOn(@NonNull ItemStack itemStack, @NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos, @NonNull Player player, @NonNull InteractionHand hand, @NonNull BlockHitResult hitResult) {
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public boolean onCaughtFire(@NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos,
                                Direction face, LivingEntity igniter) {
        if (level instanceof ServerLevel) {
            BigPrimedTnt primedTnt = createEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    explosionPower, this.defaultBlockState(), forceNoDrops, igniter);
            level.addFreshEntity(primedTnt);
            level.playSound(null, primedTnt.getX(), primedTnt.getY(), primedTnt.getZ(),
                    SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
            return true;
        }
        return false;
    }

    @Override
    public void wasExploded(@NonNull ServerLevel level, @NonNull BlockPos pos, @NonNull Explosion explosion) {
        BigPrimedTnt primed = createEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                explosionPower, this.defaultBlockState(), forceNoDrops, explosion.getIndirectSourceEntity());
        int fuse = primed.getFuse();
        primed.setFuse(level.getRandom().nextInt(fuse / 4) + fuse / 8);
        level.addFreshEntity(primed);
    }

    protected BigPrimedTnt createEntity(Level level, double x, double y, double z,
                                        float explosionPower, BlockState blockState,
                                        boolean forceNoDrops, @Nullable LivingEntity owner) {
        return new BigPrimedTnt(ENTITY_QCC_TNT.get(), level, x, y, z, explosionPower, blockState, forceNoDrops, owner);
    }

    @Override
    public @NonNull BlockState playerWillDestroy(@NonNull Level level, @NonNull BlockPos pos, @NonNull BlockState state, @NonNull Player player) {
        if (explodeOnDestroy && !level.isClientSide() && !player.getAbilities().instabuild) {
            this.onCaughtFire(state, level, pos, null, null);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockState getStateForPlacement(@NonNull BlockPlaceContext context) {
        return this.defaultBlockState();
    }
}
