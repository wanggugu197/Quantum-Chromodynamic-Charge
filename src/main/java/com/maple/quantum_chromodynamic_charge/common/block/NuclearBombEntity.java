package com.maple.quantum_chromodynamic_charge.common.block;

import com.maple.quantum_chromodynamic_charge.explosion.SphereExplosion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import static com.maple.quantum_chromodynamic_charge.common.QCCRegistration.ENTITY_NUCLEAR_BOMB;

public class NuclearBombEntity extends BigPrimedTnt {

    public NuclearBombEntity(EntityType<? extends PrimedTnt> entityType, Level level) {
        super(entityType, level);
    }

    public NuclearBombEntity(Level level, double x, double y, double z,
                             BlockState blockState, @Nullable LivingEntity owner) {
        super(ENTITY_NUCLEAR_BOMB.get(), level, x, y, z, 0, blockState, false, owner);
    }

    @Override
    protected void explode() {
        if (this.level() instanceof ServerLevel serverLevel) {
            SphereExplosion.explosion(this.blockPosition(), serverLevel, 80, true, true, true, true);
        }
    }
}
