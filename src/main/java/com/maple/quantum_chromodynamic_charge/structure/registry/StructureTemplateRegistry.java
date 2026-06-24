package com.maple.quantum_chromodynamic_charge.structure.registry;

import com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod;
import com.maple.quantum_chromodynamic_charge.structure.model.StructureDefinition.Preset;
import com.maple.quantum_chromodynamic_charge.structure.model.StructureDefinition.Structure;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import com.mapleutillib.api.registry.ModLangProvider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod.REGISTRY;

/**
 * 结构预设注册表：查询 API + 翻译键构建。
 * <p>
 * 内置数据见 {@link StructureBuiltinPresets}；显示文案通过 {@link #tr} 生成
 * {@link Component}，并在 datagen 时写入语言表。
 * </p>
 */
public final class StructureTemplateRegistry {

    private static final List<Preset> PRESETS = new ArrayList<>();
    private static final List<Preset> VIEW = Collections.unmodifiableList(PRESETS);

    private static final String KEY_PREFIX = "ui.quantum_chromodynamic_charge.structure.";

    static {
        try {
            StructureBuiltinPresets.registerAll();
        } catch (Throwable t) {
            // 避免预设加载异常拖垮整个模组初始化
            QuantumChromodynamicChargeMod.LOGGER.error(
                    "Failed to register built-in structure presets", t);
        }
    }

    private StructureTemplateRegistry() {}

    // -------------------------------------------------------------------------
    // 翻译
    // -------------------------------------------------------------------------

    /**
     * 构建结构注册用显示文本：生成稳定翻译键并（datagen 时）注册中英文。
     *
     * @param key 相对 {@link #KEY_PREFIX} 的后缀，或完整 key（含 {@code ui.} 前缀）
     */
    public static Component tr(String key, String cn, String en) {
        String full = key.startsWith("ui.") ? key : KEY_PREFIX + key;
        if (REGISTRY.doDatagen()) {
            REGISTRY.lang(full, en);
            REGISTRY.lang(ModLangProvider.LANG_ZH_CN, full, cn);
        }
        return Component.translatable(full);
    }

    /** 由英文描述生成 key 后缀。 */
    public static String keyOf(String en) {
        String key = en.toLowerCase(Locale.ROOT)
                .replace("×", "x")
                .replace("·", "_")
                .replace("*", "x")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Invalid structure lang key: " + en);
        }
        return key;
    }

    // -------------------------------------------------------------------------
    // 注册 / 查询
    // -------------------------------------------------------------------------

    public static List<Preset> presets() {
        return VIEW;
    }

    public static void register(@Nullable Preset preset) {
        if (preset != null) {
            PRESETS.add(preset);
        }
    }

    public static boolean isEmpty() {
        return PRESETS.isEmpty();
    }

    public static int presetCount() {
        return PRESETS.size();
    }

    public static Preset getPreset(int group) {
        if (PRESETS.isEmpty()) {
            throw new IllegalStateException("No structure presets registered");
        }
        return PRESETS.get(Mth.clamp(group, 0, PRESETS.size() - 1));
    }

    public static Structure getStructure(int group, int id) {
        List<Structure> list = getPreset(group).structures();
        return list.get(Mth.clamp(id, 0, list.size() - 1));
    }

    /** 安全取结构；无预设时返回 null。 */
    public static @Nullable Structure tryGetStructure(int group, int id) {
        if (PRESETS.isEmpty()) return null;
        return getStructure(group, id);
    }

    /** 触发静态初始化（datagen 时收集翻译键）。 */
    public static void initLang() {
        presetCount();
    }
}
