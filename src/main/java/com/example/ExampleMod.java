package com.example;

import com.example.common.CommonInit;
import com.example.config.ExampleModConfig;

import com.mapleutillib.api.registry.ModRegistryCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 模组主类 - 模组的入口点
 * 负责初始化模组配置、注册事件和资源
 */
@Mod(ExampleMod.MODID)
public class ExampleMod {

    /** 模组 ID（唯一标识符） */
    public static final String MODID = "examplemod";
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
    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info(MODID + " start loading");
        // 注册配置
        modContainer.registerConfig(ModConfig.Type.COMMON, ExampleModConfig.SPEC);
        // 初始化内容
        CommonInit.init(modEventBus);
    }
}
