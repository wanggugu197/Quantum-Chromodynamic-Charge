package com.maple.quantum_chromodynamic_charge.structure.material;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import lombok.Getter;
import org.jspecify.annotations.NonNull;

/**
 * 结构材料类型（可扩展枚举）。
 * <p>
 * {@link #index()}（ordinal）作银行/需求数组下标；序列化名用于存档与物品 id 片段。
 * </p>
 */
public enum StructureMaterialType implements StringRepresentable {

    FRAME("frame", "框架", 0xaa66fd3c),
    PLATE("plate", "板材", 0xaa3844f4),
    FINISH("finish", "饰面", 0xaae700ef);

    private final String name;
    @Getter
    private final String nameCn;
    @Getter
    private final int color;

    StructureMaterialType(String name, String nameCn, int color) {
        this.name = name;
        this.nameCn = nameCn;
        this.color = color;
    }

    @Override
    public @NonNull String getSerializedName() {
        return name;
    }

    public int index() {
        return ordinal();
    }

    public String langKey() {
        return "ui.quantum_chromodynamic_charge.structure.material.type." + name;
    }

    public Component displayName() {
        return Component.translatable(langKey());
    }

    public static int count() {
        return values().length;
    }

    public static StructureMaterialType of(int index) {
        StructureMaterialType[] all = values();
        if (index < 0 || index >= all.length) {
            throw new IllegalArgumentException("unknown StructureMaterialType index: " + index);
        }
        return all[index];
    }
}
