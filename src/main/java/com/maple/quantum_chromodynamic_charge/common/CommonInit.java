package com.maple.quantum_chromodynamic_charge.common;

import com.maple.quantum_chromodynamic_charge.data.lang.ExampleLangHandler;

import net.neoforged.bus.api.IEventBus;

/**
 * 通用初始化类
 */
public class CommonInit {

    private static IEventBus modBus;

    public static void init(IEventBus modBus) {
        CommonInit.modBus = modBus;
        QCCTab.init();
        QCCRegistration.init();
        ExampleLangHandler.init();
        NeoForgeCommonEvent.init();
    }
}
