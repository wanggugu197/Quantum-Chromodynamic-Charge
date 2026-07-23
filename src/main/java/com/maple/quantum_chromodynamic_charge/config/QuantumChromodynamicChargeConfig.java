package com.maple.quantum_chromodynamic_charge.config;

import com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;
import org.jetbrains.annotations.ApiStatus;

/**
 * YAML 配置持有器（toma.configuration）。
 */
@Config(id = QuantumChromodynamicChargeMod.MODID, filename = "quantum_chromodynamic_charge/quantum_chromodynamic_charge")
public class QuantumChromodynamicChargeConfig {

    // ==============================================
    // 实例
    // ==============================================

    public static QuantumChromodynamicChargeConfig INSTANCE;

    private static final Object LOCK = new Object();

    @ApiStatus.Internal
    public static ConfigHolder<QuantumChromodynamicChargeConfig> INTERNAL_INSTANCE;

    // ==============================================
    // 初始化
    // ==============================================

    /** 注册并加载配置（幂等）。 */
    public static void init() {
        synchronized (LOCK) {
            if (INSTANCE == null || INTERNAL_INSTANCE == null) {
                INTERNAL_INSTANCE = Configuration.registerConfig(QuantumChromodynamicChargeConfig.class, ConfigFormats.YAML);
                INSTANCE = INTERNAL_INSTANCE.getConfigInstance();
            }
        }
    }

    /** 获取配置实例，未初始化时自动 init。 */
    private static QuantumChromodynamicChargeConfig config() {
        if (INSTANCE == null) init();
        return INSTANCE;
    }

    public static int maxBlocksPerTick() {
        return Math.max(1, config().explosion.maxBlocksPerTick);
    }

    // ==============================================
    // 配置项
    // ==============================================

    @Configurable
    @Configurable.Comment({
            "爆炸开关配置 Explosion switches"
    })
    public ExplosionConfigs explosion = new ExplosionConfigs();

    /**
     * 爆炸行为开关配置。
     */
    public static class ExplosionConfigs {

        @Configurable
        @Configurable.Comment({
                "Enable star-triggered charge explosions from block right-click events?",
                "Controls Quantum Star + Naquadria Charge, Gravi Star + Leptonic Charge, and Unstable Star + Quantum Chromodynamic Charge.",
                "Default: true",
                "是否启用右键事件触发的星体爆弹爆炸？",
                "控制量子之星 + 超能硅岩爆弹、引力之星 + 轻子爆弹、易变之星 + 量子色动力学爆弹。",
                "默认 true"
        })
        public boolean enableEventChargeExplosions = true;

        @Configurable
        @Configurable.Comment({
                "Enable Nuclear Bomb entity explosion after fuse ends?",
                "When false, primed nuclear bombs finish the fuse without clearing blocks.",
                "Default: true",
                "是否启用核弹实体引信结束后的爆炸？",
                "关闭后，已点燃的核弹引信结束时不会清空方块。",
                "默认 true"
        })
        public boolean enableNuclearBombExplosion = true;

        @Configurable
        @Configurable.Comment({
                "Allow Area Destroyer to start clearing blocks?",
                "When false, pressing Detonate will not start sphere/chunk/area clearing and will not consume explosives.",
                "Default: true",
                "是否允许区域破坏器开始清空方块？",
                "关闭后，点击引爆不会开始球形/区块/区域清空，也不会消耗装药。",
                "默认 true"
        })
        public boolean enableAreaDestroyerClearing = true;

        @Configurable
        @Configurable.Comment({
                "Maximum blocks processed per tick by progressive explosion engines.",
                "Lower values reduce lag spikes but make large explosions take longer.",
                "Default: 50000",
                "渐进式爆炸引擎每 tick 最多处理的方块数。",
                "数值越低，卡顿峰值越小，但大型爆炸持续时间越长。",
                "默认 50000"
        })
        public int maxBlocksPerTick = 50_000;
    }
}
