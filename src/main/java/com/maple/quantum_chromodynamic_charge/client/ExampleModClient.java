package com.maple.quantum_chromodynamic_charge.client;

import com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = QuantumChromodynamicChargeMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = QuantumChromodynamicChargeMod.MODID, value = Dist.CLIENT)
public class ExampleModClient {

    public ExampleModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {}

    @SubscribeEvent
    static void onRegisterItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {}
}
