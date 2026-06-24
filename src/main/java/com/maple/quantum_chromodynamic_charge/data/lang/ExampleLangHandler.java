package com.maple.quantum_chromodynamic_charge.data.lang;

import com.mapleutillib.api.registry.ModLangProvider;

import static com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod.REGISTRY;

public class ExampleLangHandler {

    public static void addLang(String key, String cn, String en) {
        REGISTRY.lang(key, en);
        REGISTRY.lang(ModLangProvider.LANG_ZH_CN, key, cn);
    }

    public static void init() {
        if (!REGISTRY.doDatagen()) return;

        addLang("example.example.example", "示例", "Example");
    }
}
