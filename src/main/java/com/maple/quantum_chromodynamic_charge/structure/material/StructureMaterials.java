package com.maple.quantum_chromodynamic_charge.structure.material;

import com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod;
import com.maple.quantum_chromodynamic_charge.common.QCCRegistration;
import com.maple.quantum_chromodynamic_charge.common.QCCTab;

import net.minecraft.client.color.item.Constant;
import net.minecraft.world.item.Item;

import com.gto.registrylib.util.entry.ItemEntry;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod.REGISTRY;
import static com.mapleutillib.utils.generator.ModItemModelGeneratorHelper.createMultiLayerTintedFlatItem;

/**
 * 结构材料物品总表：类型 × 档位 → {@link ItemEntry}。
 * <p>
 * 类加载时通过 {@code static} 块完成全部组件注册；Item 反查表在首次
 * {@link #lookup} 时惰性构建（此时注册表已就绪，避免 static 阶段调用 {@code asItem()}）。
 * </p>
 */
public final class StructureMaterials {

    @SuppressWarnings("unchecked")
    private static final ItemEntry<Item>[][] TABLE = new ItemEntry[StructureMaterialType.count()][StructureMaterialTier.count()];

    /** Item → 条目；注册完成前为空，首次查询时填充。 */
    private static volatile Map<Item, MaterialsEntry> lookupByItem;

    static {
        for (StructureMaterialType type : StructureMaterialType.values()) {
            for (StructureMaterialTier tier : StructureMaterialTier.values()) {
                String id = "structure_component_" + type.getSerializedName() + "_" + tier.getSerializedName();
                TABLE[type.index()][tier.index()] = REGISTRY
                        .item(id)
                        .langCn(tier.getNameCn() + type.getNameCn() + "组件")
                        .addTab(QCCTab.TAB_QCC.getKey())
                        .model(() -> (item, prov) -> createMultiLayerTintedFlatItem(prov, item, Map.of(1, new Constant(type.getColor())),
                                QuantumChromodynamicChargeMod.id("item/material_" + tier.getSerializedName() + "_0"),
                                QuantumChromodynamicChargeMod.id("item/material_" + tier.getSerializedName() + "_1")))
                        .register();
            }
        }
    }

    private StructureMaterials() {}

    public record MaterialsEntry(StructureMaterialType type, StructureMaterialTier tier, int points, Item item) {}

    /**
     * 触达类加载以完成 static 注册（幂等）。
     * 由 {@link QCCRegistration} 在合适时机调用。
     */
    public static void bootstrap() {
        // static 块已在类初始化时执行
    }

    public static ItemEntry<Item> entry(StructureMaterialType type, StructureMaterialTier tier) {
        return TABLE[type.index()][tier.index()];
    }

    public static Item item(StructureMaterialType type, StructureMaterialTier tier) {
        return entry(type, tier).asItem();
    }

    public static @Nullable MaterialsEntry lookup(@Nullable Item item) {
        if (item == null) return null;
        return ensureLookup().get(item);
    }

    public static List<MaterialsEntry> unloadEntries(StructureMaterialType type) {
        List<MaterialsEntry> list = new ArrayList<>(StructureMaterialTier.count());
        for (StructureMaterialTier tier : StructureMaterialTier.unloadOrder()) {
            list.add(new MaterialsEntry(type, tier, tier.getPoints(), item(type, tier)));
        }
        return list;
    }

    public static int bankSize() {
        return StructureMaterialType.count();
    }

    public static int[] newBankArray() {
        return new int[bankSize()];
    }

    private static Map<Item, MaterialsEntry> ensureLookup() {
        Map<Item, MaterialsEntry> cached = lookupByItem;
        if (cached != null) {
            return cached;
        }
        synchronized (StructureMaterials.class) {
            if (lookupByItem != null) {
                return lookupByItem;
            }
            Map<Item, MaterialsEntry> map = new HashMap<>();
            for (StructureMaterialType type : StructureMaterialType.values()) {
                for (StructureMaterialTier tier : StructureMaterialTier.values()) {
                    ItemEntry<Item> e = TABLE[type.index()][tier.index()];
                    if (e == null) continue;
                    Item it = e.asItem();
                    map.put(it, new MaterialsEntry(type, tier, tier.getPoints(), it));
                }
            }
            // 仅在全部条目解析成功后缓存；否则下次再试
            if (map.size() == StructureMaterialType.count() * StructureMaterialTier.count()) {
                lookupByItem = Collections.unmodifiableMap(map);
                return lookupByItem;
            }
            return map;
        }
    }
}
