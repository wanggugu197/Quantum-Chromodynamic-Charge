package com.maple.quantum_chromodynamic_charge.common;

import com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import static com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod.REGISTRY;

public class QCCBlockTags {

    public static void init() {
        REGISTRY.blockTags();
    }

    private static TagKey<Block> createBlockTag(String path) {
        return TagKey.create(Registries.BLOCK, QuantumChromodynamicChargeMod.id(path));
    }
}
