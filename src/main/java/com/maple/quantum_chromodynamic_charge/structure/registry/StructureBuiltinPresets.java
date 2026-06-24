package com.maple.quantum_chromodynamic_charge.structure.registry;

import com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod;
import com.maple.quantum_chromodynamic_charge.structure.material.StructureMaterialType;
import com.maple.quantum_chromodynamic_charge.structure.model.StructureDefinition.Preset;
import com.maple.quantum_chromodynamic_charge.structure.model.StructureDefinition.Structure;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static com.maple.quantum_chromodynamic_charge.structure.material.StructureMaterialType.FINISH;
import static com.maple.quantum_chromodynamic_charge.structure.material.StructureMaterialType.FRAME;
import static com.maple.quantum_chromodynamic_charge.structure.material.StructureMaterialType.PLATE;
import static com.maple.quantum_chromodynamic_charge.structure.model.StructureDefinition.Structure.structure;
import static com.maple.quantum_chromodynamic_charge.structure.registry.StructureTemplateRegistry.tr;

/**
 * 内置预设数据（GTO 移植 + demo）。注册 API 见 {@link StructureTemplateRegistry}。
 */
final class StructureBuiltinPresets {

    // —— 类型标签 ——
    private static final Component TYPE_PLATFORM = tr("type.platform", "平台", "Platform");
    private static final Component TYPE_PLATFORM_3X3 = tr("type.platform_3x3", "平台(3×3)", "Platform (3×3)");
    private static final Component TYPE_PLATFORM_LARGE = tr("type.platform_large", "平台(大)", "Platform (Large)");
    private static final Component TYPE_ROAD = tr("type.road", "道路", "Road");
    private static final Component TYPE_FACTORY = tr("type.factory", "工厂", "Factory");

    // —— 通用显示名 ——
    private static final Component NAME_CHESSBOARD = tr("name.high_saturation_chessboard", "高饱和棋盘", "High saturation chessboard");
    private static final Component NAME_PANEL = tr("name.high_saturation_panel", "高饱和嵌板", "High saturation panel");
    private static final Component NAME_SMALL_PLOT = tr("name.small_plot", "小型地块", "Small plot");
    private static final Component NAME_MEDIUM_PLOT = tr("name.medium_plot", "中型地块", "Medium plot");
    private static final Component NAME_LARGE_PLOT = tr("name.large_plot", "大型地块", "Large plot");
    private static final Component NAME_MIXED_PLOT = tr("name.mixed_plot", "多用途地块", "Mixed-use plot");
    private static final Component NAME_LIGHT_ROAD = tr("name.light_road", "浅色带公路地板", "Light-colored road floor");
    private static final Component NAME_GRAY_LIGHTS = tr("name.gray_lights", "浅色带灯带地板", "Gray floor with lights");

    private StructureBuiltinPresets() {}

    static void registerAll() {
        registerGtoLibraries();
        StructureTemplateRegistry.register(Preset.preset("demo")
                .displayName(tr("preset.demo", "演示预设", "Demo Preset"))
                .description(tr("preset.demo.desc", "内置示例结构", "Built-in sample structures"))
                .addStructure(structure("demo_stone_platform")
                        .type(TYPE_PLATFORM)
                        .displayName(tr("demo_stone_platform", "石质平台 16×1×16", "Stone Platform 16×1×16"))
                        .description(tr("demo_stone_platform.desc", "一层石块平台", "Single-layer stone platform"))
                        .source("QCC")
                        .resource(id("demo_stone_platform"))
                        .symbolMap(id("demo_stone_platform.json"))
                        .materials(FRAME, 144)
                        .build())
                .build());
    }

    private static void registerGtoLibraries() {
        StructureTemplateRegistry.register(Preset.preset("platform_standard_library_alpha")
                .displayName(tr("preset.alpha", "平台标准预设库-α", "Platform Standard Library-α"))
                .description(tr("preset.alpha.desc", "高饱和棋盘/嵌板等（GTO 移植）", "High-sat chessboard/panel (from GTO)"))
                .addStructure(s("high_saturation_chessboard_1_blue_pink", "high_saturation_chessboard_1",
                        "high_saturation_chessboard_blue_pink.json", TYPE_PLATFORM,
                        NAME_CHESSBOARD, "1×1 蓝·粉", "1×1 blue·pink", "阿龙-还有一件事", FRAME, 144))
                .addStructure(s("high_saturation_chessboard_1_orange_white", "high_saturation_chessboard_1",
                        "high_saturation_chessboard_orange_white.json", TYPE_PLATFORM,
                        NAME_CHESSBOARD, "1×1 橙·白", "1×1 orange·white", "阿龙-还有一件事", FRAME, 144))
                .addStructure(s("high_saturation_chessboard_1_yellow_lime", "high_saturation_chessboard_1",
                        "high_saturation_chessboard_yellow_lime.json", TYPE_PLATFORM,
                        NAME_CHESSBOARD, "1×1 黄·青", "1×1 yellow·lime", "阿龙-还有一件事", FRAME, 144))
                .addStructure(s("high_saturation_chessboard_3_blue_pink", "high_saturation_chessboard_3",
                        "high_saturation_chessboard_blue_pink.json", TYPE_PLATFORM_3X3,
                        NAME_CHESSBOARD, "3×3 蓝·粉", "3×3 blue·pink", "阿龙-还有一件事", FRAME, 1296))
                .addStructure(s("high_saturation_chessboard_3_orange_white", "high_saturation_chessboard_3",
                        "high_saturation_chessboard_orange_white.json", TYPE_PLATFORM_3X3,
                        NAME_CHESSBOARD, "3×3 橙·白", "3×3 orange·white", "阿龙-还有一件事", FRAME, 1296))
                .addStructure(s("high_saturation_chessboard_3_yellow_lime", "high_saturation_chessboard_3",
                        "high_saturation_chessboard_yellow_lime.json", TYPE_PLATFORM_3X3,
                        NAME_CHESSBOARD, "3×3 黄·青", "3×3 yellow·lime", "阿龙-还有一件事", FRAME, 1296))
                .addStructure(s("high_saturation_panel_1_white_pink", "high_saturation_panel_1",
                        "high_saturation_panel_white_pink.json", TYPE_PLATFORM,
                        NAME_PANEL, "1×1 白嵌粉", "1×1 white embed pink", "阿龙-还有一件事", FRAME, 144))
                .addStructure(s("high_saturation_panel_1_black_blue", "high_saturation_panel_1",
                        "high_saturation_panel_black_blue.json", TYPE_PLATFORM,
                        NAME_PANEL, "1×1 黑嵌蓝", "1×1 black embed blue", "阿龙-还有一件事", FRAME, 144))
                .addStructure(s("high_saturation_panel_3_white_pink", "high_saturation_panel_1",
                        "high_saturation_panel_white_pink.json", TYPE_PLATFORM_3X3,
                        NAME_PANEL, "3×3 白嵌粉", "3×3 white embed pink", "阿龙-还有一件事", FRAME, 144))
                .addStructure(s("high_saturation_panel_3_black_blue", "high_saturation_panel_1",
                        "high_saturation_panel_black_blue.json", TYPE_PLATFORM_3X3,
                        NAME_PANEL, "3×3 黑嵌蓝", "3×3 black embed blue", "阿龙-还有一件事", FRAME, 144))
                .addStructure(s("white_floor_with_greenery_and_orange_and_yellow_edges",
                        "white_floor_with_greenery_and_orange_and_yellow_edges",
                        "white_floor_with_greenery_and_orange_and_yellow_edges.json", TYPE_PLATFORM_LARGE,
                        NAME_PANEL, "2×2 带绿化的镶橙黄边白色地板", "2×2 white floor with greenery",
                        "阿龙-还有一件事", FRAME, 576))
                .build());

        StructureTemplateRegistry.register(Preset.preset("platform_standard_library_beta")
                .displayName(tr("preset.beta", "平台标准预设库-β", "Platform Standard Library-β"))
                .description(tr("preset.beta.desc", "小/中/大地块地基（GTO 移植）", "Plot foundations (from GTO)"))
                .addStructure(s("small_plot_stone_foundation", "small_plot_stone_foundation",
                        "small_plot_stone_foundation.json", TYPE_PLATFORM,
                        NAME_SMALL_PLOT, "3×3 小型地块-石质地基", "3×3 small plot-stone", "疏影", FRAME, 1296))
                .addStructure(s("small_plot_concrete_foundation", "small_plot_concrete_foundation",
                        "small_plot_concrete_foundation.json", TYPE_PLATFORM,
                        NAME_SMALL_PLOT, "3×3 小型地块-混凝土地基", "3×3 small plot-concrete", "疏影", FRAME, 1296))
                .addStructure(s("medium_sized_plot_stone_foundation", "medium_sized_plot_stone_foundation",
                        "medium_sized_plot_stone_foundation.json", TYPE_PLATFORM,
                        NAME_MEDIUM_PLOT, "5×5 中型地块-石质地基", "5×5 medium plot-stone", "疏影", FRAME, 3600))
                .addStructure(s("medium_sized_plot_concrete_foundation", "medium_sized_plot_concrete_foundation",
                        "medium_sized_plot_concrete_foundation.json", TYPE_PLATFORM,
                        NAME_MEDIUM_PLOT, "5×5 中型地块-混凝土地基", "5×5 medium plot-concrete", "疏影", FRAME, 3600))
                .addStructure(s("large_plot_stone_foundation", "large_plot_stone_foundation",
                        "large_plot_stone_foundation.json", TYPE_PLATFORM,
                        NAME_LARGE_PLOT, "7×7 大型地块-石质地基", "7×7 large plot-stone", "疏影", FRAME, 7056))
                .addStructure(s("large_plot_concrete_foundation", "large_plot_concrete_foundation",
                        "large_plot_concrete_foundation.json", TYPE_PLATFORM,
                        NAME_LARGE_PLOT, "7×7 大型地块-混凝土地基", "7×7 large plot-concrete", "疏影", FRAME, 7056))
                .addStructure(s("mixed_use_plot_stone_foundation", "mixed_use_plot_stone_foundation",
                        "mixed_use_plot_stone_foundation.json", TYPE_PLATFORM,
                        NAME_MIXED_PLOT, "9|2×2 多用途地块-石质地基", "9|2×2 mixed-use plot-stone", "疏影", FRAME, 5184))
                .addStructure(s("mixed_use_plot_concrete_foundation", "mixed_use_plot_concrete_foundation",
                        "mixed_use_plot_concrete_foundation.json", TYPE_PLATFORM,
                        NAME_MIXED_PLOT, "9|2×2 多用途地块-混凝土地基", "9|2×2 mixed-use plot-concrete", "疏影", FRAME, 5184))
                .build());

        StructureTemplateRegistry.register(Preset.preset("platform_extension_library")
                .displayName(tr("preset.extension", "平台扩展预设库", "Platform Extension Library"))
                .description(tr("preset.extension.desc", "公路地板与灯带地板（GTO 移植）", "Road & light floors (from GTO)"))
                .addStructure(s("light_colored_road_floor_1", "light_colored_road_floor_1",
                        "light_colored_road_floor_1.json", TYPE_PLATFORM,
                        NAME_LIGHT_ROAD, "浅色公路地板 1", "Light road floor 1", "神官", FRAME, 100))
                .addStructure(s("light_colored_road_floor_2", "light_colored_road_floor_2",
                        "light_colored_road_floor_2.json", TYPE_ROAD,
                        NAME_LIGHT_ROAD, "浅色公路地板 2", "Light road floor 2", "神官", FRAME, 20))
                .addStructure(s("light_colored_road_floor_3", "light_colored_road_floor_3",
                        "light_colored_road_floor_3.json", TYPE_PLATFORM_3X3,
                        NAME_LIGHT_ROAD, "浅色公路地板 3", "Light road floor 3", "神官", FRAME, 676))
                .addStructure(s("light_colored_road_floor_4", "light_colored_road_floor_4",
                        "light_colored_road_floor_4.json", TYPE_PLATFORM_LARGE,
                        NAME_LIGHT_ROAD, "浅色公路地板 4", "Light road floor 4", "神官", FRAME, 676))
                .addStructure(s("gray_floor_with_lights_1", "gray_floor_with_lights_1",
                        "gray_floor_with_lights_1.json", TYPE_PLATFORM,
                        NAME_GRAY_LIGHTS, "灯带灰地板 1", "Gray lights floor 1", "呼", FINISH, 100))
                .addStructure(s("gray_floor_with_lights_2", "gray_floor_with_lights_2",
                        "gray_floor_with_lights_2.json", TYPE_ROAD,
                        NAME_GRAY_LIGHTS, "灯带灰地板 2", "Gray lights floor 2", "呼", FINISH, 20))
                .addStructure(s("gray_floor_with_lights_3", "gray_floor_with_lights_3",
                        "gray_floor_with_lights_3.json", TYPE_PLATFORM_3X3,
                        NAME_GRAY_LIGHTS, "灯带灰地板 3", "Gray lights floor 3", "呼", FINISH, 676))
                .addStructure(s("gray_floor_with_lights_4", "gray_floor_with_lights_4",
                        "gray_floor_with_lights_4.json", TYPE_PLATFORM_LARGE,
                        NAME_GRAY_LIGHTS, "灯带灰地板 4", "Gray lights floor 4", "呼", FINISH, 676))
                .build());

        StructureTemplateRegistry.register(Preset.preset("factory_standard_library")
                .displayName(tr("preset.factory", "工厂标准预设库", "Factory Standard Library"))
                .description(tr("preset.factory.desc", "厂房结构（GTO 移植）", "Factory buildings (from GTO)"))
                .addStructure(structure("standard_factory_building")
                        .type(TYPE_FACTORY)
                        .displayName(tr("name.standard_factory", "标准厂房", "Standard factory building"))
                        .source("疏影")
                        .resource(id("standard_factory_building"))
                        .symbolMap(id("standard_factory_building.json"))
                        .materials(FRAME, 400)
                        .materials(PLATE, 100)
                        .build())
                .addStructure(structure("long_corridor_factory_building")
                        .type(TYPE_FACTORY)
                        .displayName(tr("name.long_corridor_factory", "长廊厂房", "Long corridor factory building"))
                        .source("疏影")
                        .resource(id("long_corridor_factory_building"))
                        .symbolMap(id("long_corridor_factory_building.json"))
                        .materials(FRAME, 800)
                        .materials(PLATE, 800)
                        .build())
                .build());
    }

    private static Structure s(
                               String name,
                               String resource,
                               String map,
                               Component type,
                               Component displayName,
                               String descCn,
                               String descEn,
                               String source,
                               StructureMaterialType mat,
                               int cost) {
        return structure(name)
                .type(type)
                .displayName(displayName)
                .description(tr("desc." + StructureTemplateRegistry.keyOf(descEn), descCn, descEn))
                .source(source)
                .resource(id(resource))
                .symbolMap(id(map))
                .materials(mat, cost)
                .build();
    }

    private static Identifier id(String path) {
        return QuantumChromodynamicChargeMod.id(path);
    }
}
