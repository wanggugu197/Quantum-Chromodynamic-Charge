package com.example.data.lang;

import com.mapleutillib.api.registry.ModLangProvider;

import static com.example.ExampleMod.REGISTRY;

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
