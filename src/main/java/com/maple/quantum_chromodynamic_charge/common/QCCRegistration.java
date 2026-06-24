package com.maple.quantum_chromodynamic_charge.common;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.gto.registrylib.util.entry.BlockEntry;
import com.gto.registrylib.util.entry.ItemEntry;

import static com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod.REGISTRY;
import static com.maple.quantum_chromodynamic_charge.common.QCCTab.TAB_QCC;

/**
 * 方块和实体注册类
 */
public class QCCRegistration {

    public static void init() {}

    public static final ItemEntry<Item> NXAMPLE_ITEM = REGISTRY
            .item("example_item")
            .langCn("示例物品")
            .addTab(TAB_QCC.getKey())
            .register();

    public static final BlockEntry<Block> NXAMPLE_BLOCK = REGISTRY
            .block("example_block")
            .langCn("示例块")
            .item(builder -> builder.addTab(TAB_QCC.getKey()))
            .register();

    public static final ItemEntry<Item> QUANTUM_STAR = REGISTRY
            .item("quantum_star")
            .langCn("量子之星")
            .addTab(TAB_QCC.getKey())
            .register();

    public static final ItemEntry<Item> GRAVI_STAR = REGISTRY
            .item("gravi_star")
            .langCn("引力之星")
            .addTab(TAB_QCC.getKey())
            .register();

    public static final ItemEntry<Item> UNSTABLE_STAR = REGISTRY
            .item("unstable_star")
            .langCn("易变之星")
            .addTab(TAB_QCC.getKey())
            .register();

    public static final BlockEntry<Block> NAQUADRIA_CHARGE = REGISTRY
            .block("naquadria_charge")
            .langCn("超能硅岩爆弹")
            .initialProperties(Blocks.IRON_BLOCK)
            .item(builder -> builder.addTab(TAB_QCC.getKey()))
            .register();

    public static final BlockEntry<Block> LEPTONIC_CHARGE = REGISTRY
            .block("leptonic_charge")
            .langCn("轻子爆弹")
            .initialProperties(Blocks.IRON_BLOCK)
            .item(builder -> builder.addTab(TAB_QCC.getKey()))
            .register();

    public static final BlockEntry<Block> QUANTUM_CHROMODYNAMIC_CHARGE = REGISTRY
            .block("quantum_chromodynamic_charge")
            .langCn("量子色动力学爆弹")
            .initialProperties(Blocks.IRON_BLOCK)
            .item(builder -> builder.addTab(TAB_QCC.getKey()))
            .register();
}
