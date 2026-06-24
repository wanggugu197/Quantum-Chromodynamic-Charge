package com.maple.quantum_chromodynamic_charge.common;

import com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import static com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod.REGISTRY;

public class QCCItemTags {

    public static void init() {
        REGISTRY.itemTags().add(EXPLOSION_EQUIVALENT_HOLDER);
    }

    public static final TagKey<Item> EXPLOSION_EQUIVALENT_HOLDER = createItemTag("explosion_equivalent_holder");

    private static TagKey<Item> createItemTag(String path) {
        return TagKey.create(Registries.ITEM, QuantumChromodynamicChargeMod.id(path));
    }
}
