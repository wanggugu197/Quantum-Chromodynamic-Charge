package com.maple.quantum_chromodynamic_charge.common;

import com.maple.quantum_chromodynamic_charge.config.QuantumChromodynamicChargeConfig;
import com.maple.quantum_chromodynamic_charge.data.lang.QCCLangHandler;
import com.maple.quantum_chromodynamic_charge.structure.registry.StructureTemplateRegistry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;

import com.mapleutillib.utils.task.TaskHandler;

/**
 * 通用初始化类
 */
public class CommonInit {

    private static IEventBus modBus;

    public static void init(IEventBus modBus) {
        CommonInit.modBus = modBus;
        // 注册并加载配置
        QuantumChromodynamicChargeConfig.init();

        QCCDataComponent.init();
        QCCRegistration.init();
        QCCTab.init();

        TaskHandler.registerAttachment(QCCLevelTask.LEVEL_TASK_DATA);

        QCCLangHandler.init();
        NeoForgeCommonEvent.init();
        // 触发预设库加载（读 classpath 结构资源）
        StructureTemplateRegistry.presetCount();
        modBus.addListener(CommonInit::commonSetup);
        modBus.addListener(CommonInit::modConstruct);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {}

    private static void modConstruct(FMLConstructModEvent event) {}
}
