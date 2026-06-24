package com.example.common;

import com.gto.registrylib.util.entry.BlockEntry;
import net.minecraft.world.item.Item;

import com.gto.registrylib.util.entry.ItemEntry;
import net.minecraft.world.level.block.Block;

import static com.example.ExampleMod.REGISTRY;
import static com.example.common.ExampleTab.TAB_EXAMPEL;

/**
 * 方块和实体注册类
 */
public class ExampleRegistration {

    public static void init() {}

    public static final ItemEntry<Item> NXAMPLE_ITEM = REGISTRY
            .item("example_item")
            .langCn("示例物品")
            .addTab(TAB_EXAMPEL.getKey())
            .register();

    public static final BlockEntry<Block> NXAMPLE_BLOCK = REGISTRY
            .block("example_block")
            .langCn("示例块")
            .item(builder -> builder.addTab(TAB_EXAMPEL.getKey()))
            .register();
}
