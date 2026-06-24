package com.maple.quantum_chromodynamic_charge.structure.material;

import net.minecraft.util.StringRepresentable;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

/**
 * 结构材料档位：点数与序列化名。
 * <p>
 * 卸载时按 {@link #unloadOrder()}（高档优先）兑出。
 * </p>
 */
public enum StructureMaterialTier implements StringRepresentable {

    BASIC("basic", 200),
    ADVANCED("advanced", 1000),
    ELITE("elite", 5000);

    public static final Codec<StructureMaterialTier> CODEC = StringRepresentable.fromEnum(StructureMaterialTier::values);

    private static final StringRepresentable.EnumCodec<StructureMaterialTier> BY_NAME = StringRepresentable.fromEnum(StructureMaterialTier::values);

    /** 高点数优先，静态缓存避免每次分配。 */
    private static final StructureMaterialTier[] UNLOAD_ORDER;

    static {
        StructureMaterialTier[] all = values();
        UNLOAD_ORDER = new StructureMaterialTier[all.length];
        for (int i = 0; i < all.length; i++) {
            UNLOAD_ORDER[i] = all[all.length - 1 - i];
        }
    }

    private final String name;
    private final int points;

    StructureMaterialTier(String name, int points) {
        this.name = name;
        this.points = points;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public int index() {
        return ordinal();
    }

    public int points() {
        return points;
    }

    public static int count() {
        return values().length;
    }

    /** 卸载顺序：高点数优先（不可变视图，勿修改）。 */
    public static StructureMaterialTier[] unloadOrder() {
        return UNLOAD_ORDER;
    }

    public static StructureMaterialTier of(int index) {
        StructureMaterialTier[] all = values();
        if (index < 0 || index >= all.length) {
            throw new IllegalArgumentException("unknown StructureMaterialTier index: " + index);
        }
        return all[index];
    }

    public static @Nullable StructureMaterialTier byName(@Nullable String name) {
        if (name == null || name.isEmpty()) return null;
        return BY_NAME.byName(name);
    }

    public static StructureMaterialTier byName(@Nullable String name, StructureMaterialTier fallback) {
        if (name == null || name.isEmpty()) return fallback;
        return BY_NAME.byName(name, fallback);
    }
}
