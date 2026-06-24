package com.maple.quantum_chromodynamic_charge.common;

import net.minecraft.world.item.CreativeModeTab;

import com.gto.registrylib.util.entry.RegistryEntry;

import java.util.Map;

import static com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod.REGISTRY;
import static com.maple.quantum_chromodynamic_charge.common.QCCRegistration.QUANTUM_CHROMODYNAMIC_CHARGE;

public class QCCTab {

    public static void init() {}

    // 创造模式标签注册
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB_QCC = REGISTRY
            .creativeTab("quantum_chromodynamic_charge", "Quantum Chromodynamic Charge", Map.of("zh_cn", "量子色动力学爆弹"),
                    builder -> builder.icon(QUANTUM_CHROMODYNAMIC_CHARGE::asStack));
}
