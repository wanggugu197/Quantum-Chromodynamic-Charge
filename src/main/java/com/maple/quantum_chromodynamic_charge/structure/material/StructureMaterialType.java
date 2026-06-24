package com.maple.quantum_chromodynamic_charge.structure.material;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

/**
 * 结构材料类型（可扩展枚举）。
 * <p>
 * {@link #index()}（ordinal）作银行/需求数组下标；序列化名用于存档与物品 id 片段。
 * </p>
 */
public enum StructureMaterialType implements StringRepresentable {

    FRAME("frame"),
    PLATE("plate"),
    FINISH("finish");

    public static final Codec<StructureMaterialType> CODEC = StringRepresentable.fromEnum(StructureMaterialType::values);

    private static final StringRepresentable.EnumCodec<StructureMaterialType> BY_NAME = StringRepresentable.fromEnum(StructureMaterialType::values);

    private final String name;

    StructureMaterialType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
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

    public static boolean isValidIndex(int index) {
        return index >= 0 && index < values().length;
    }

    public static @Nullable StructureMaterialType byName(@Nullable String name) {
        if (name == null || name.isEmpty()) return null;
        return BY_NAME.byName(name);
    }

    public static StructureMaterialType byName(@Nullable String name, StructureMaterialType fallback) {
        if (name == null || name.isEmpty()) return fallback;
        return BY_NAME.byName(name, fallback);
    }
}
