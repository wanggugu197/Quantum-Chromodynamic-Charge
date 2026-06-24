package com.example.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** 模组配置类 - 使用 NeoForge ModConfigSpec 管理配置 */
public class ExampleModConfig {

    /** 配置构建器 */
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** 配置规范 */
    public static final ModConfigSpec SPEC;

    /** 示例配置子节 */
    public static final ExampleConfig1 EXAMPLE_CONFIG;

    static {
        EXAMPLE_CONFIG = new ExampleConfig1();
        initConfig();
        SPEC = BUILDER.build();
    }

    /** 模组全局启用开关 */
    public static ModConfigSpec.BooleanValue modEnabled;

    /** 调试模式开关 */
    public static ModConfigSpec.BooleanValue debugMode;

    /** 最大生成数量限制 */
    public static ModConfigSpec.IntValue maxGenerationCount;

    private static void initConfig() {
        BUILDER.push("general");

        modEnabled = BUILDER.comment("启用模组核心功能").define("enabled", true);
        debugMode = BUILDER.comment("启用调试模式").define("debug_mode", false);
        maxGenerationCount = BUILDER.comment("最大生成数量限制").defineInRange("max_generation_count", 100, 1, 1000);

        BUILDER.pop();
        EXAMPLE_CONFIG.init(BUILDER);
    }

    /** 示例配置子节类 */
    public static class ExampleConfig1 {

        /** 示例功能启用开关 */
        public ModConfigSpec.BooleanValue exampleEnabled;

        /** 示例数值参数 */
        public ModConfigSpec.IntValue exampleValue;

        public void init(ModConfigSpec.Builder builder) {
            builder.push("example");

            exampleEnabled = builder.comment("启用示例功能").define("enabled", false);
            exampleValue = builder.comment("示例数值参数").defineInRange("value", 50, 1, 100);

            builder.pop();
        }
    }
}
