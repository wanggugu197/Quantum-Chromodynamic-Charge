package com.maple.quantum_chromodynamic_charge.data.lang;

import com.maple.quantum_chromodynamic_charge.structure.registry.StructureTemplateRegistry;

import net.minecraft.network.chat.Component;

import com.mapleutillib.api.registry.ModLangProvider;

import static com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod.REGISTRY;

public class ExampleLangHandler {

    public static Component addLang(String key, String cn, String en) {
        if (REGISTRY.doDatagen()) {
            REGISTRY.lang(key, en);
            REGISTRY.lang(ModLangProvider.LANG_ZH_CN, key, cn);
        }
        return Component.translatable(key);
    }

    public static void init() {
        if (!REGISTRY.doDatagen()) return;

        // datagen 时加载预设库，通过 tr() 注册结构显示文案
        StructureTemplateRegistry.initLang();

        addLang("tooltip.quantum_chromodynamic_charge.coordinate_positioning", "坐标: X·%d, Y·%d, Z·%d", "Coordinate: X·%d, Y·%d, Z·%d");

        addLang("ui.quantum_chromodynamic_charge.area_destroyer.enabled", "开关", "Enable");
        addLang("ui.quantum_chromodynamic_charge.area_destroyer.mode", "爆炸模式", "Explosion Mode");
        addLang("ui.quantum_chromodynamic_charge.area_destroyer.mode.sphere", "球形", "Sphere");
        addLang("ui.quantum_chromodynamic_charge.area_destroyer.mode.chunk", "区块", "Chunk");
        addLang("ui.quantum_chromodynamic_charge.area_destroyer.mode.area", "区域(坐标卡)", "Area (Coords)");
        addLang("ui.quantum_chromodynamic_charge.area_destroyer.explosives", "装药", "Explosives");
        addLang("ui.quantum_chromodynamic_charge.area_destroyer.coordinates", "坐标卡", "Coordinate Cards");
        addLang("ui.quantum_chromodynamic_charge.area_destroyer.detonate", "引爆", "Detonate");
        addLang("ui.quantum_chromodynamic_charge.area_destroyer.status", "当量: %s", "Yield: %s");

        // 结构放置器 UI 壳（非结构注册文案；预设显示名由 StructureTemplateRegistry.tr 注册）
        addLang("ui.quantum_chromodynamic_charge.structure.tab.blueprint", "蓝图", "Blueprint");
        addLang("ui.quantum_chromodynamic_charge.structure.tab.material", "材料", "Materials");
        addLang("ui.quantum_chromodynamic_charge.structure.tab.settings", "设置", "Settings");
        addLang("ui.quantum_chromodynamic_charge.structure.tab.export", "导出", "Export");
        addLang("ui.quantum_chromodynamic_charge.structure.preset", "预设库", "Library");
        addLang("ui.quantum_chromodynamic_charge.structure.template", "样板", "Template");
        addLang("ui.quantum_chromodynamic_charge.structure.size_short", "%s×%s×%s · %s×%s", "%s×%s×%s · %s×%s");
        addLang("ui.quantum_chromodynamic_charge.structure.template_index", "样板 %s/%s", "Template %s/%s");
        addLang("ui.quantum_chromodynamic_charge.structure.summary", "当前选择", "Selection");
        addLang("ui.quantum_chromodynamic_charge.structure.summary_preset", "预设: %s", "Library: %s");
        addLang("ui.quantum_chromodynamic_charge.structure.summary_template", "样板: %s", "Template: %s");
        addLang("ui.quantum_chromodynamic_charge.structure.summary_type_size", "类型: %s · 尺寸: %s", "Type: %s · Size: %s");
        addLang("ui.quantum_chromodynamic_charge.structure.summary_size", "尺寸: %s", "Size: %s");
        addLang("ui.quantum_chromodynamic_charge.structure.source", "来源: %s", "Source: %s");
        addLang("ui.quantum_chromodynamic_charge.structure.no_presets", "尚未注册任何结构预设。", "No structure presets registered.");
        addLang("ui.quantum_chromodynamic_charge.structure.offset_section", "偏移 (X/Z 区块 · Y 方块)", "Offset (X/Z chunks · Y blocks)");
        addLang("ui.quantum_chromodynamic_charge.structure.offset", "%s", "%s");
        addLang("ui.quantum_chromodynamic_charge.structure.place_options_section", "放置选项", "Place Options");
        addLang("ui.quantum_chromodynamic_charge.structure.transform_section", "变换", "Transform");
        addLang("ui.quantum_chromodynamic_charge.structure.skip_air", "跳过空气", "Skip Air");
        addLang("ui.quantum_chromodynamic_charge.structure.skip_occupied", "跳过方块", "Skip Occupied");
        addLang("ui.quantum_chromodynamic_charge.structure.update_light", "更新光照", "Update Light");
        addLang("ui.quantum_chromodynamic_charge.structure.update_heightmap", "更新高度图", "Update Heightmap");
        addLang("ui.quantum_chromodynamic_charge.structure.x_mirror", "X 镜像", "X Mirror");
        addLang("ui.quantum_chromodynamic_charge.structure.z_mirror", "Z 镜像", "Z Mirror");
        addLang("ui.quantum_chromodynamic_charge.structure.rotation", "旋转", "Rotation");
        addLang("ui.quantum_chromodynamic_charge.structure.speed", "速度", "Speed");
        addLang("ui.quantum_chromodynamic_charge.structure.boundary", "放置边界", "Bounds");
        addLang("ui.quantum_chromodynamic_charge.structure.bounds_compact",
                "(%s,%s,%s) → (%s,%s,%s)",
                "(%s,%s,%s) → (%s,%s,%s)");
        addLang("ui.quantum_chromodynamic_charge.structure.export_section", "区域导出", "Region Export");
        addLang("ui.quantum_chromodynamic_charge.structure.export_hint",
                "放入两张坐标定位卡，分别记录对角点后导出为 .mbs + 映射。",
                "Insert two coordinate cards for opposite corners, then export .mbs + mapping.");
        addLang("ui.quantum_chromodynamic_charge.structure.export_slots", "坐标卡", "Coordinate Cards");
        addLang("ui.quantum_chromodynamic_charge.structure.export_ready", "§a可导出", "§aReady");
        addLang("ui.quantum_chromodynamic_charge.structure.export_need_cards", "§c需两张坐标卡", "§cNeed 2 cards");
        addLang("ui.quantum_chromodynamic_charge.structure.place", "放置", "Place");
        addLang("ui.quantum_chromodynamic_charge.structure.export", "导出", "Export");
        addLang("ui.quantum_chromodynamic_charge.structure.progress", "放置中 %s%%", "Placing %s%%");
        addLang("ui.quantum_chromodynamic_charge.structure.exporting", "导出中…", "Exporting…");
        addLang("ui.quantum_chromodynamic_charge.structure.idle", "就绪", "Ready");

        addLang("ui.quantum_chromodynamic_charge.structure.material.summary", "材料（存量/需求）", "Materials (have/need)");
        addLang("ui.quantum_chromodynamic_charge.structure.material.line", "%s %s/%s", "%s %s/%s");
        addLang("ui.quantum_chromodynamic_charge.structure.material.extra_line", "%s %s/%s", "%s %s/%s");
        addLang("ui.quantum_chromodynamic_charge.structure.material.none_active", "当前无材料需求与存量", "No active materials");
        // type.* 由 StructureMaterialType.displayName() / langKey() 使用
        addLang("ui.quantum_chromodynamic_charge.structure.material.type.frame", "框架", "Frame");
        addLang("ui.quantum_chromodynamic_charge.structure.material.type.plate", "板材", "Plate");
        addLang("ui.quantum_chromodynamic_charge.structure.material.type.finish", "饰面", "Finish");
        addLang("ui.quantum_chromodynamic_charge.structure.material.adequate", "§a材料充足", "§aMaterials OK");
        addLang("ui.quantum_chromodynamic_charge.structure.material.insufficient", "§c材料不足", "§cNeed materials");
        addLang("ui.quantum_chromodynamic_charge.structure.material.load", "装载", "Load");
        addLang("ui.quantum_chromodynamic_charge.structure.material.unload", "卸载", "Unload");
    }
}
