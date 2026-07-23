package com.maple.quantum_chromodynamic_charge.common;

import com.maple.quantum_chromodynamic_charge.config.QuantumChromodynamicChargeConfig;
import com.maple.quantum_chromodynamic_charge.explosion.SphereExplosion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

public class NeoForgeCommonEvent {

    public static void init() {
        EVENT_BUS.register(NeoForgeCommonEvent.class);
    }

    @SubscribeEvent
    @SuppressWarnings("all")
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!QuantumChromodynamicChargeConfig.INSTANCE.explosion.enableEventChargeExplosions) {
            return;
        }
        Level level = event.getLevel();
        if (level == null) return;
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack itemStack = player.getItemInHand(hand);
        Item item = itemStack.getItem();

        if (item == QCCRegistration.QUANTUM_STAR.get() &&
                level.getBlockState(pos).getBlock() == QCCRegistration.NAQUADRIA_CHARGE.get()) {
            SphereExplosion.explosion(pos, level, 200, true, true, true, true);
            return;
        }
        if (item == QCCRegistration.GRAVI_STAR.get() &&
                level.getBlockState(pos).getBlock() == QCCRegistration.LEPTONIC_CHARGE.get()) {
            SphereExplosion.explosion(pos, level, 800, true, true, true, true);
            return;
        }
        if (item == QCCRegistration.UNSTABLE_STAR.get() &&
                level.getBlockState(pos).getBlock() == QCCRegistration.QUANTUM_CHROMODYNAMIC_CHARGE.get()) {
            SphereExplosion.explosion(pos, level, 2000, true, true, true, true);
            return;
        }
    }
}
