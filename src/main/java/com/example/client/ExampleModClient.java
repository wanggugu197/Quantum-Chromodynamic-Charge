package com.example.client;

import com.example.ExampleMod;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// 此类不会在专用服务器上加载。从此处访问客户端代码是安全的。
@Mod(value = ExampleMod.MODID, dist = Dist.CLIENT)
// 可以使用 EventBusSubscriber 自动注册类中所有带有 @SubscribeEvent 注解的静态方法
@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ExampleModClient {

    public ExampleModClient(ModContainer container) {
        // 允许 NeoForge 为此模组的配置创建配置界面。
        // 配置界面可通过以下方式访问：模组界面 > 点击你的模组 > 点击配置。
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // 客户端设置代码
        ExampleMod.LOGGER.info("来自客户端设置的问候");
        ExampleMod.LOGGER.info("MINECRAFT 名称 >> {}", Minecraft.getInstance().getUser().getName());
    }
}
