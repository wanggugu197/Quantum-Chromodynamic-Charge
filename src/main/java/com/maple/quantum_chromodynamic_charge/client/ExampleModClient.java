package com.maple.quantum_chromodynamic_charge.client;

import com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod;
import com.maple.quantum_chromodynamic_charge.structure.material.StructureMaterialTier;
import com.maple.quantum_chromodynamic_charge.structure.material.StructureMaterialType;
import com.maple.quantum_chromodynamic_charge.structure.material.StructureMaterials;

import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = QuantumChromodynamicChargeMod.MODID, dist = Dist.CLIENT)
public class ExampleModClient {

    public ExampleModClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(ExampleModClient::onRegisterItemColors);
    }

    /** 结构材料物品：layer1（tint index 1）按类型着色，其余层不着色。 */
    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        for (StructureMaterialType type : StructureMaterialType.values()) {
            final int color = type.getColor();
            for (StructureMaterialTier tier : StructureMaterialTier.values()) {
                Item item = StructureMaterials.item(type, tier);
                event.register((stack, tintIndex) -> tintIndex == 1 ? color : -1, item);
            }
        }
    }
}
