package com.maple.quantum_chromodynamic_charge.common.item.attachment;

import com.maple.quantum_chromodynamic_charge.common.QCCDataComponent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.gto.registrylib.composite.ComponentItem;
import com.gto.registrylib.composite.ItemAttachment;
import com.gto.registrylib.tooltip.SubNode;
import com.gto.registrylib.tooltip.TooltipNodeCollector;

public class CoordinatePositioningAttachment extends ItemAttachment<ComponentItem> {

    public CoordinatePositioningAttachment() {}

    @Override
    public InteractionResult useOn(ComponentItem item, UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        QCCDataComponent.COORDINATE.set(context.getItemInHand(), pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(ComponentItem item, Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            QCCDataComponent.COORDINATE.set(player.getItemInHand(hand), Vec3i.ZERO);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void collectTooltipNodes(ComponentItem item, ItemStack stack, TooltipNodeCollector collector) {
        Vec3i pos = QCCDataComponent.COORDINATE.get(stack);
        if (pos != null && !pos.equals(Vec3i.ZERO)) {
            collector.node(new SubNode.Basic(
                    Component.translatable("tooltip.quantum_chromodynamic_charge.coordinate_positioning", pos.getX(), pos.getY(), pos.getZ())));
        }
    }
}
