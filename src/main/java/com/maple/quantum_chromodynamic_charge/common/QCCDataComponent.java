package com.maple.quantum_chromodynamic_charge.common;

import net.minecraft.core.Vec3i;

import com.gto.registrylib.util.entry.DataComponentTypeEntry;

import static com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod.REGISTRY;

public class QCCDataComponent {

    public static void init() {}

    public static final DataComponentTypeEntry<Vec3i> COORDINATE = REGISTRY.dataComponentTypeEntry(
            "coordinate",
            builder -> builder.persistent(Vec3i.CODEC).networkSynchronized(Vec3i.STREAM_CODEC));
}
