package com.maple.quantum_chromodynamic_charge.common;

import com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod;
import com.maple.quantum_chromodynamic_charge.common.block.*;
import com.maple.quantum_chromodynamic_charge.common.item.attachment.CoordinatePositioningAttachment;
import com.maple.quantum_chromodynamic_charge.structure.material.StructureMaterials;

import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.gto.registrylib.composite.ComponentItem;
import com.gto.registrylib.util.entry.BlockEntityTypeEntry;
import com.gto.registrylib.util.entry.BlockEntry;
import com.gto.registrylib.util.entry.EntityEntry;
import com.gto.registrylib.util.entry.ItemEntry;
import com.mapleutillib.utils.RLUtils;
import com.mapleutillib.utils.generator.ModBlockModelGeneratorHelper;

import static com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod.REGISTRY;
import static com.maple.quantum_chromodynamic_charge.common.QCCTab.TAB_QCC;

/**
 * 方块和实体注册类
 */
public class QCCRegistration {

    public static void init() {}

    public static final BlockEntry<BigTntBlock> POWDER_BARREL = REGISTRY
            .block("powder_barrel", props -> new BigTntBlock(props, 2.0F, false, false))
            .langCn("火药桶")
            .initialProperties(Blocks.TNT)
            .item(builder -> builder.addTab(TAB_QCC.getKey()))
            .register();

    public static final BlockEntry<BigTntBlock> BIGGER_TNT = REGISTRY
            .block("bigger_tnt", props -> new BigTntBlock(props, 16.0F, false, true))
            .langCn("更大的TNT")
            .blockstate(() -> (block, prov) -> prov.create(block,
                    ModBlockModelGeneratorHelper.createCustomSixWayBlock(prov, "bigger_tnt",
                            QuantumChromodynamicChargeMod.id("block/industrial_tnt_side"), RLUtils.mc("block/tnt_top"), RLUtils.mc("block/tnt_bottom"))))
            .initialProperties(Blocks.TNT)
            .item(builder -> builder.addTab(TAB_QCC.getKey()))
            .register();

    public static final EntityEntry<BigPrimedTnt> ENTITY_QCC_TNT = REGISTRY
            .entity("tnt", (EntityType<BigPrimedTnt> type, Level level) -> new BigPrimedTnt(type, level), MobCategory.MISC)
            .langCn("TNT")
            .renderer(() -> () -> TntRenderer::new)
            .register();

    public static final BlockEntry<NuclearBombBlock> NUCLEAR_BOMB = REGISTRY
            .block("nuclear_bomb", NuclearBombBlock::new)
            .langCn("核弹")
            .blockstate(() -> (block, prov) -> prov.create(block,
                    ModBlockModelGeneratorHelper.createCustomSixWayBlock(prov, "nuclear_bomb",
                            QuantumChromodynamicChargeMod.id("block/nuclear_bomb"), RLUtils.mc("block/tnt_top"), RLUtils.mc("block/tnt_bottom"))))
            .initialProperties(Blocks.TNT)
            .item(builder -> builder.addTab(TAB_QCC.getKey()))
            .register();

    public static final EntityEntry<NuclearBombEntity> ENTITY_NUCLEAR_BOMB = REGISTRY
            .entity("nuclear_bomb", (EntityType<NuclearBombEntity> type, Level level) -> new NuclearBombEntity(type, level), MobCategory.MISC)
            .langCn("核弹")
            .renderer(() -> () -> TntRenderer::new)
            .register();

    public static final ItemEntry<Item> QUANTUM_STAR = REGISTRY
            .item("quantum_star")
            .langCn("量子之星")
            .addTab(TAB_QCC.getKey())
            .register();

    public static final BlockEntry<Block> NAQUADRIA_CHARGE = REGISTRY
            .block("naquadria_charge")
            .langCn("超能硅岩爆弹")
            .initialProperties(Blocks.IRON_BLOCK)
            .addTag(BlockTags.MINEABLE_WITH_PICKAXE)
            .item(builder -> builder.addTab(TAB_QCC.getKey()))
            .register();

    public static final ItemEntry<Item> GRAVI_STAR = REGISTRY
            .item("gravi_star")
            .langCn("引力之星")
            .addTab(TAB_QCC.getKey())
            .register();

    public static final BlockEntry<Block> LEPTONIC_CHARGE = REGISTRY
            .block("leptonic_charge")
            .langCn("轻子爆弹")
            .initialProperties(Blocks.IRON_BLOCK)
            .addTag(BlockTags.MINEABLE_WITH_PICKAXE)
            .item(builder -> builder.addTab(TAB_QCC.getKey()))
            .register();

    public static final ItemEntry<Item> UNSTABLE_STAR = REGISTRY
            .item("unstable_star")
            .langCn("易变之星")
            .addTab(TAB_QCC.getKey())
            .register();

    public static final BlockEntry<Block> QUANTUM_CHROMODYNAMIC_CHARGE = REGISTRY
            .block("quantum_chromodynamic_charge")
            .langCn("量子色动力学爆弹")
            .initialProperties(Blocks.IRON_BLOCK)
            .addTag(BlockTags.MINEABLE_WITH_PICKAXE)
            .item(builder -> builder.addTab(TAB_QCC.getKey()))
            .register();

    public static final ItemEntry<ComponentItem> COORDINATE_POSITIONING_CARD = REGISTRY
            .componentItem("coordinate_positioning_card")
            .langCn("坐标定位卡")
            .properties(p -> p.stacksTo(1).component(QCCDataComponent.COORDINATE, Vec3i.ZERO))
            .attach(new CoordinatePositioningAttachment())
            .addTab(TAB_QCC.getKey())
            .register();

    public static final BlockEntry<AreaDestroyerBlock> AREA_DESTROYER = REGISTRY
            .block("area_destroyer", AreaDestroyerBlock::new)
            .langCn("区域破坏器")
            .initialProperties(Blocks.IRON_BLOCK)
            .addTag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate(() -> (block, prov) -> {
                Identifier identifier = ModBlockModelGeneratorHelper.createCustomSixWayBlock(prov, "area_destroyer",
                        QuantumChromodynamicChargeMod.id("block/area_destroyer/front"),
                        QuantumChromodynamicChargeMod.id("block/area_destroyer/side"),
                        QuantumChromodynamicChargeMod.id("block/area_destroyer/side"),
                        QuantumChromodynamicChargeMod.id("block/area_destroyer/bottom"));
                ModBlockModelGeneratorHelper.createHorizontalBlock(prov, block, identifier);
            })
            .item(builder -> builder.addTab(TAB_QCC.getKey()))
            .register();

    public static final BlockEntityTypeEntry<AreaDestroyerBlockEntity> AREA_DESTROYER_ENTITY = REGISTRY
            .blockEntity(REGISTRY, "area_destroyer_entity", (_, p, s) -> new AreaDestroyerBlockEntity(p, s))
            .validBlock(AREA_DESTROYER)
            .register();

    public static final BlockEntry<StructurePlacerBlock> STRUCTURE_PLACER = REGISTRY
            .block("structure_placer", StructurePlacerBlock::new)
            .langCn("结构放置器")
            .initialProperties(Blocks.IRON_BLOCK)
            .addTag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate(() -> (block, prov) -> {
                Identifier identifier = ModBlockModelGeneratorHelper.createCustomSixWayBlock(prov, "structure_placer",
                        QuantumChromodynamicChargeMod.id("block/structure_placer/front"),
                        QuantumChromodynamicChargeMod.id("block/structure_placer/side"),
                        QuantumChromodynamicChargeMod.id("block/structure_placer/side"),
                        QuantumChromodynamicChargeMod.id("block/structure_placer/bottom"));
                ModBlockModelGeneratorHelper.createHorizontalBlock(prov, block, identifier);
            })
            .item(builder -> builder.addTab(TAB_QCC.getKey()))
            .register();

    public static final BlockEntityTypeEntry<StructurePlacerBlockEntity> STRUCTURE_PLACER_ENTITY = REGISTRY
            .blockEntity(REGISTRY, "structure_placer_entity", (_, p, s) -> new StructurePlacerBlockEntity(p, s))
            .validBlock(STRUCTURE_PLACER)
            .register();

    static {
        // 触达 StructureMaterials 类加载，由其 static 块完成类型×档位材料表注册
        StructureMaterials.bootstrap();
    }
}
