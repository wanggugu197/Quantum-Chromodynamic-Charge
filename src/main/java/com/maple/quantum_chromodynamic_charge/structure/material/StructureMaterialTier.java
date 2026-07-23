package com.maple.quantum_chromodynamic_charge.structure.material;

import net.minecraft.util.StringRepresentable;

import lombok.Getter;
import org.jspecify.annotations.NonNull;

/**
 * 结构材料档位：点数与序列化名。
 * <p>
 * 卸载时按 {@link #unloadOrder()}（高档优先）兑出。
 * </p>
 */
public enum StructureMaterialTier implements StringRepresentable {

    BASIC("basic", "初级", 200),
    ADVANCED("advanced", "中级", 1000),
    ELITE("elite", "高级", 5000);

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
    @Getter
    private final String nameCn;
    @Getter
    private final int points;

    StructureMaterialTier(String name, String nameCn, int points) {
        this.name = name;
        this.nameCn = nameCn;
        this.points = points;
    }

    @Override
    public @NonNull String getSerializedName() {
        return name;
    }

    public int index() {
        return ordinal();
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
}
