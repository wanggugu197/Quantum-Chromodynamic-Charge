package com.example.common;

import net.minecraft.world.item.CreativeModeTab;

import com.gto.registrylib.util.entry.RegistryEntry;

import java.util.Map;

import static com.example.ExampleMod.REGISTRY;
import static com.example.common.ExampleRegistration.NXAMPLE_ITEM;

public class ExampleTab {

    public static void init() {}

    // 创造模式标签注册
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB_EXAMPEL = REGISTRY
            .creativeTab("example_tab", "Example Tab", Map.of("zh_cn", "示例标签"),
                    builder -> builder.icon(NXAMPLE_ITEM::asStack));
}
