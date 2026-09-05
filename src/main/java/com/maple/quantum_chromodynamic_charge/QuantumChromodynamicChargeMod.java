package com.maple.quantum_chromodynamic_charge;

import com.maple.quantum_chromodynamic_charge.common.CommonInit;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import com.mapleutillib.api.registry.ModRegistryCore;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 模组主类 - 模组的入口点
 * 负责初始化模组配置、注册事件和资源
 */
@Mod(QuantumChromodynamicChargeMod.MODID)
public class QuantumChromodynamicChargeMod {

    /** 模组 ID（唯一标识符） */
    public static final String MODID = "quantum_chromodynamic_charge";
    /** 日志记录器实例 */
    public static final Logger LOGGER = LogUtils.getLogger();
    /** RegistryLib 注册表核心实例 */
    public static final ModRegistryCore REGISTRY = ModRegistryCore.create(MODID);

    /**
     * 模组构造函数 - 在模组加载时被调用
     * 
     * @param modEventBus  模组事件总线
     * @param modContainer 模组容器
     */
    public QuantumChromodynamicChargeMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info(MODID + " start loading");
        // 初始化内容
        CommonInit.init(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
