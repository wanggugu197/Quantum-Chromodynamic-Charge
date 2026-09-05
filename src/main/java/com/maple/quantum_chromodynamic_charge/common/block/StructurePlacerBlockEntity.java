package com.maple.quantum_chromodynamic_charge.common.block;

import com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod;
import com.maple.quantum_chromodynamic_charge.common.QCCDataComponent;
import com.maple.quantum_chromodynamic_charge.common.QCCRegistration;
import com.maple.quantum_chromodynamic_charge.structure.export.StructureExporter;
import com.maple.quantum_chromodynamic_charge.structure.material.StructureMaterialOps;
import com.maple.quantum_chromodynamic_charge.structure.material.StructureMaterialTable;
import com.maple.quantum_chromodynamic_charge.structure.material.StructureMaterialType;
import com.maple.quantum_chromodynamic_charge.structure.material.StructureMaterials;
import com.maple.quantum_chromodynamic_charge.structure.model.StructureDefinition;
import com.maple.quantum_chromodynamic_charge.structure.model.StructureDefinition.ExtraMaterial;
import com.maple.quantum_chromodynamic_charge.structure.place.StructurePlacer;
import com.maple.quantum_chromodynamic_charge.structure.registry.StructureTemplateRegistry;
import com.maple.quantum_chromodynamic_charge.structure.transform.StructureTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DropSaved;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.mapleutillib.api.baseBlock.DirectionBlockEntity;
import com.mapleutillib.api.resource.ObservableItemResourceHandler;
import com.mapleutillib.api.resource.filter.ItemAllowListFilter;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 结构放置器：选预设、材料银行、调偏移/变换、分 tick 放置，或用坐标卡导出。
 * <p>
 * 材料逻辑在 {@link StructureMaterialOps}；本类负责持久化字段、RPC 与 UI。
 * </p>
 */
public class StructurePlacerBlockEntity extends DirectionBlockEntity implements ISyncPersistRPCBlockEntity {

    public static final int COORD_SLOTS = 2;
    public static final int MATERIAL_ROWS = 3;
    public static final int MATERIAL_COLS = 12;
    public static final int MATERIAL_SLOTS = MATERIAL_ROWS * MATERIAL_COLS;
    public static final int CHUNK_EDGE_BLOCKS = 16;

    private static final int UI_WIDTH = 270;
    private static final int UI_CONTENT_HEIGHT = 120;
    private static final int UI_TAB_HEADER_HEIGHT = 14;
    private static final int UI_PANEL_WIDTH = 260;
    private static final int LABEL_W = 52;
    private static final int SELECTOR_W = 196;

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    @DropSaved
    @Getter
    private final ObservableItemResourceHandler coordinateInventory = new ObservableItemResourceHandler(COORD_SLOTS);

    /** 结构组件 / 额外材料装卸槽。 */
    @Persisted
    @DescSynced
    @DropSaved
    @Getter
    private final ObservableItemResourceHandler materialInventory = new ObservableItemResourceHandler(MATERIAL_SLOTS);

    /**
     * 材料点数银行（int[] 便于 LDLib2 同步；逻辑层用 {@link StructureMaterialTable}）。
     * 下标 = {@link StructureMaterialType#index()}。
     */
    @Persisted
    @DescSynced
    @DropSaved
    private int[] materialBank = StructureMaterials.newBankArray();

    /** 材料是否满足当前结构需求（对齐 GTO insufficient==true 表示充足）。 */
    @DescSynced
    private boolean materialsAdequate;

    @Persisted
    @DescSynced
    private int presetIndex;

    @Persisted
    @DescSynced
    private int structureIndex;

    @Persisted
    @DescSynced
    private int offsetX;

    @Persisted
    @DescSynced
    private int offsetY = -1;

    @Persisted
    @DescSynced
    private int offsetZ;

    @Persisted
    @DescSynced
    private boolean skipAir = true;

    /**
     * 跳过已有方块：世界中该位置非空气则不覆盖放置。
     * {@code false} 时覆盖（基岩仍始终跳过）。
     */
    @Persisted
    @DescSynced
    private boolean skipOccupied;

    @Persisted
    @DescSynced
    private boolean updateLight = true;

    /** 放置时是否更新高度图；默认开启。 */
    @Persisted
    @DescSynced
    private boolean updateHeightmap = true;

    @Persisted
    @DescSynced
    private boolean xMirror;

    @Persisted
    @DescSynced
    private boolean zMirror;

    @Persisted
    @DescSynced
    private int rotation;

    @Persisted
    @DescSynced
    private int speed = 10;

    @Persisted
    @DescSynced
    private boolean taskCompleted = true;

    @DescSynced
    private int progress;

    public StructurePlacerBlockEntity(BlockPos pos, BlockState state) {
        this(QCCRegistration.STRUCTURE_PLACER_ENTITY.get(), pos, state);
    }

    protected StructurePlacerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        coordinateInventory.setContext(this);
        coordinateInventory.setIOFilter(new ItemAllowListFilter(
                Set.of(QCCRegistration.COORDINATE_POSITIONING_CARD.asItem()), Set.of()));
        coordinateInventory.setOnChanged((slot, prev) -> markDirty("coordinateInventory"));

        materialInventory.setContext(this);
        materialInventory.setOnChanged((slot, prev) -> {
            markDirty("materialInventory");
            examineMaterials();
        });
    }

    // -------------------------------------------------------------------------
    // 结构 / 位置
    // -------------------------------------------------------------------------

    private StructureDefinition.Structure currentStructure() {
        clampIndices();
        return StructureTemplateRegistry.getStructure(presetIndex, structureIndex);
    }

    private void clampIndices() {
        int presets = StructureTemplateRegistry.presetCount();
        if (presets <= 0) return;
        presetIndex = Mth.clamp(presetIndex, 0, presets - 1);
        int structs = StructureTemplateRegistry.getPreset(presetIndex).structures().size();
        structureIndex = Mth.clamp(structureIndex, 0, Math.max(0, structs - 1));
    }

    /** 放置起点：机器区块对齐 + 区块偏移 + Y 方块偏移。 */
    public BlockPos startPos() {
        StructureDefinition.Structure s = currentStructure();
        BlockPos pos = getBlockPos();
        int chunkMinX = (pos.getX() >> 4) << 4;
        int chunkMinZ = (pos.getZ() >> 4) << 4;
        int centerOffsetX = (s.sizeX() - 1) / 32;
        int centerOffsetZ = (s.sizeZ() - 1) / 32;
        int startX = chunkMinX - centerOffsetX * 16 + offsetX * 16;
        int startZ = chunkMinZ - centerOffsetZ * 16 + offsetZ * 16;
        int startY = pos.getY() + offsetY;
        return new BlockPos(startX, startY, startZ);
    }

    public StructureTransform.BlockPosBounds placementBounds() {
        StructureDefinition.Structure s = currentStructure();
        return StructureTransform.transformedBounds(
                startPos(), s.sizeX(), s.sizeY(), s.sizeZ(), rotation, xMirror, zMirror);
    }

    @Nullable
    private BlockPos readCoord(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != QCCRegistration.COORDINATE_POSITIONING_CARD.asItem()) {
            return null;
        }
        Vec3i v = QCCDataComponent.COORDINATE.get(stack);
        return v == null || v.equals(Vec3i.ZERO) ? null : new BlockPos(v);
    }

    private BlockPos[] coords() {
        BlockPos a = null, b = null;
        for (int i = 0; i < coordinateInventory.size(); i++) {
            BlockPos p = readCoord(coordinateInventory.getStackInSlot(i));
            if (p == null) continue;
            if (a == null) a = p;
            else {
                b = p;
                break;
            }
        }
        return new BlockPos[] { a, b };
    }

    public boolean canExport() {
        BlockPos[] c = coords();
        return c[0] != null && c[1] != null;
    }

    // -------------------------------------------------------------------------
    // 材料银行（逻辑在 StructureMaterialOps；此处只做持久化/同步桥接）
    // -------------------------------------------------------------------------

    private StructureMaterialTable bankTable() {
        int[] ensured = StructureMaterialOps.ensureBankSize(materialBank);
        if (ensured != materialBank) {
            materialBank = ensured;
            markDirty("materialBank");
        }
        return StructureMaterialOps.tableOf(materialBank);
    }

    private void writeBank(StructureMaterialTable table) {
        materialBank = table.toArray();
        markDirty("materialBank");
    }

    /** 槽内结构组件 -> 点数银行。 */
    public void loadingMaterial() {
        writeBank(StructureMaterialOps.loadFromSlots(bankTable(), materialInventory));
        markDirty("materialInventory");
        examineMaterials();
    }

    /** 点数银行 -> 空槽兑回组件（优先高档）。 */
    public void unloadingMaterial() {
        writeBank(StructureMaterialOps.unloadToSlots(bankTable(), materialInventory));
        markDirty("materialInventory");
        examineMaterials();
    }

    /** 检查材料表 + extraMaterials 是否充足。 */
    public void examineMaterials() {
        if (StructureTemplateRegistry.isEmpty()) {
            materialsAdequate = false;
            markDirty("materialsAdequate");
            return;
        }
        materialsAdequate = StructureMaterialOps.isAdequate(
                bankTable(), currentStructure(), materialInventory);
        markDirty("materialsAdequate");
    }

    /** 扣除材料表与额外物品；失败返回 false。 */
    private boolean consumeResources() {
        if (StructureTemplateRegistry.isEmpty()) return false;
        StructureMaterialTable next = StructureMaterialOps.consume(
                bankTable(), currentStructure(), materialInventory);
        if (next == null) {
            examineMaterials();
            return false;
        }
        writeBank(next);
        markDirty("materialInventory");
        examineMaterials();
        return true;
    }

    // -------------------------------------------------------------------------
    // 操作
    // -------------------------------------------------------------------------

    public void startPlacement() {
        Level level = getLevel();
        if (level == null || level.isClientSide() || !taskCompleted) return;
        if (StructureTemplateRegistry.isEmpty()) return;

        StructureDefinition.Structure structure = currentStructure();
        examineMaterials();
        if (!materialsAdequate) return;

        // 先加载资源，再扣材料，避免加载失败白扣费
        StructurePlacer.LoadedStructure loaded;
        try {
            loaded = StructurePlacer.load(structure);
        } catch (Exception e) {
            QuantumChromodynamicChargeMod.LOGGER.error(
                    "Structure load failed (materials not consumed): {} / {}",
                    structure.name(), structure.resource(), e);
            return;
        }

        if (!consumeResources()) return;

        progress = 0;
        taskCompleted = false;
        markDirty("taskCompleted");
        markDirty("progress");

        try {
            StructurePlacer.placeStructureAsync(
                    level,
                    startPos(),
                    loaded,
                    Math.max(10000, speed * 10000),
                    skipOccupied,
                    skipAir,
                    updateLight,
                    updateHeightmap,
                    zMirror,
                    xMirror,
                    rotation,
                    p -> {
                        progress = p;
                        markDirty("progress");
                    },
                    () -> {
                        taskCompleted = true;
                        progress = 100;
                        markDirty("taskCompleted");
                        markDirty("progress");
                    });
        } catch (Exception e) {
            QuantumChromodynamicChargeMod.LOGGER.error(
                    "Structure placement failed after consume: {} / {}",
                    structure.name(), structure.resource(), e);
            taskCompleted = true;
            markDirty("taskCompleted");
        }
    }

    public void exportRegion() {
        if (!(getLevel() instanceof ServerLevel serverLevel)) return;
        BlockPos[] c = coords();
        if (c[0] == null || c[1] == null) return;
        StructureExporter.exportAsync(serverLevel, c[0], c[1], xMirror, zMirror, rotation);
    }

    // -------------------------------------------------------------------------
    // RPC
    // -------------------------------------------------------------------------

    @RPCMethod
    public void rpcSetPreset(RPCSender sender, int index) {
        if (sender.isServer()) return;
        presetIndex = index;
        structureIndex = 0;
        clampIndices();
        examineMaterials();
    }

    @RPCMethod
    public void rpcSetStructure(RPCSender sender, int index) {
        if (sender.isServer()) return;
        structureIndex = index;
        clampIndices();
        examineMaterials();
    }

    @RPCMethod
    public void rpcNudgeOffset(RPCSender sender, int axis, int delta) {
        if (sender.isServer()) return;
        applyOffset(axis, delta);
    }

    @RPCMethod
    public void rpcSetSkipAir(RPCSender sender, boolean v) {
        if (sender.isServer()) return;
        skipAir = v;
    }

    @RPCMethod
    public void rpcSetSkipOccupied(RPCSender sender, boolean v) {
        if (sender.isServer()) return;
        skipOccupied = v;
    }

    @RPCMethod
    public void rpcSetUpdateLight(RPCSender sender, boolean v) {
        if (sender.isServer()) return;
        updateLight = v;
    }

    @RPCMethod
    public void rpcSetUpdateHeightmap(RPCSender sender, boolean v) {
        if (sender.isServer()) return;
        updateHeightmap = v;
    }

    @RPCMethod
    public void rpcSetXMirror(RPCSender sender, boolean v) {
        if (sender.isServer()) return;
        xMirror = v;
    }

    @RPCMethod
    public void rpcSetZMirror(RPCSender sender, boolean v) {
        if (sender.isServer()) return;
        zMirror = v;
    }

    @RPCMethod
    public void rpcCycleRotation(RPCSender sender) {
        if (sender.isServer()) return;
        rotation = (rotation + 90) % 360;
    }

    @RPCMethod
    public void rpcNudgeSpeed(RPCSender sender, int delta) {
        if (sender.isServer()) return;
        speed = Mth.clamp(speed + delta, 1, 100);
    }

    // -------------------------------------------------------------------------
    // UI（TabView：蓝图 / 材料 / 设置 / 导出；底栏仅放置 + 背包）
    // -------------------------------------------------------------------------

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        clampIndices();
        examineMaterials();

        var root = new UIElement()
                .style(s -> s.background(IGuiTexture.EMPTY))
                .layout(l -> l
                        .gapAll(2)
                        .alignItems(AlignItems.CENTER)
                        .justifyContent(AlignContent.CENTER)
                        .flexDirection(FlexDirection.COLUMN));

        var tabView = new TabView();
        tabView.tabHeaderContainer.layout(l -> l.width(UI_WIDTH).height(UI_TAB_HEADER_HEIGHT));
        tabView.tabContentContainer.layout(l -> l.width(UI_WIDTH).height(UI_CONTENT_HEIGHT));

        tabView.addTab(
                new Tab().setText(Component.translatable("ui.quantum_chromodynamic_charge.structure.tab.blueprint")),
                buildBlueprintTab());
        tabView.addTab(
                new Tab().setText(Component.translatable("ui.quantum_chromodynamic_charge.structure.tab.material")),
                buildMaterialTab());
        tabView.addTab(
                new Tab().setText(Component.translatable("ui.quantum_chromodynamic_charge.structure.tab.settings")),
                buildSettingsTab());
        tabView.addTab(
                new Tab().setText(Component.translatable("ui.quantum_chromodynamic_charge.structure.tab.export")),
                buildExportTab());

        root.addChild(tabView);

        // 下方：部署栏 + 玩家背包
        var lower = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(2)
                .alignItems(AlignItems.FLEX_START));
        lower.addChild(buildDeployPanel());
        lower.addChild(new InventorySlots()
                .layout(l -> l.paddingAll(5))
                .style(s -> s.background(Sprites.BORDER_THICK_RT1)));
        root.addChild(lower);

        return new ModularUI(
                UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP_MERGED))),
                holder.player);
    }

    /**
     * 材料标签页：需求/存量、装载卸载、3*12 材料槽。
     */
    private UIElement buildMaterialTab() {
        var scroller = createScrollerView();
        var panel = columnPanel();

        panel.addChild(sectionTitle("ui.quantum_chromodynamic_charge.structure.material.summary"));
        panel.addChild(infoBlock(this::materialVisibleLines));
        panel.addChild(infoBlock(() -> materialsAdequate ? Component.translatable("ui.quantum_chromodynamic_charge.structure.material.adequate") : Component.translatable("ui.quantum_chromodynamic_charge.structure.material.insufficient")));

        var loadRow = row();
        loadRow.addChild(actionButton(
                "ui.quantum_chromodynamic_charge.structure.material.load", e -> {
                    if (e.button == 0) loadingMaterial();
                }));
        loadRow.addChild(actionButton(
                "ui.quantum_chromodynamic_charge.structure.material.unload", e -> {
                    if (e.button == 0) unloadingMaterial();
                }));
        panel.addChild(loadRow);

        var slotBox = new UIElement()
                .layout(l -> l
                        .width(MATERIAL_COLS * 18 + 6)
                        .alignItems(AlignItems.CENTER));
        slotBox.addChild(slotGrid(materialInventory, MATERIAL_SLOTS, MATERIAL_COLS));
        panel.addChild(slotBox);

        scroller.addScrollViewChild(panel);
        return scroller;
    }

    /**
     * 导出标签页：坐标卡槽 + 导出状态/按钮（与放置底栏分离）。
     */
    private UIElement buildExportTab() {
        var scroller = createScrollerView();
        var panel = columnPanel();

        panel.addChild(sectionTitle("ui.quantum_chromodynamic_charge.structure.export_section"));
        panel.addChild(muted("ui.quantum_chromodynamic_charge.structure.export_hint"));

        panel.addChild(sectionTitle("ui.quantum_chromodynamic_charge.structure.export_slots"));
        var slotRow = row();
        slotRow.addChild(slotGrid(coordinateInventory, COORD_SLOTS, COORD_SLOTS));
        panel.addChild(slotRow);

        panel.addChild(infoBlock(() -> canExport() ? Component.translatable("ui.quantum_chromodynamic_charge.structure.export_ready") : Component.translatable("ui.quantum_chromodynamic_charge.structure.export_need_cards")));

        panel.addChild(actionButton(
                "ui.quantum_chromodynamic_charge.structure.export", e -> {
                    if (e.button == 0) exportRegion();
                }));

        panel.addChild(infoBlock(() -> StructureExporter.isExporting() ? Component.translatable("ui.quantum_chromodynamic_charge.structure.exporting") : Component.empty()));

        scroller.addScrollViewChild(panel);
        return scroller;
    }

    /**
     * 仅列出「银行有存量」或「结构有需求」的材料类型，以及额外物品需求/存量。
     */
    private Component materialVisibleLines() {
        if (StructureTemplateRegistry.presetCount() <= 0) {
            return Component.translatable("ui.quantum_chromodynamic_charge.structure.no_presets");
        }
        StructureDefinition.Structure structure = currentStructure();
        StructureMaterialTable bank = bankTable();
        StructureMaterialTable cost = structure.materials();
        List<StructureMaterialType> visible = bank.visibleWith(cost);

        if (visible.isEmpty() && structure.extraMaterials().isEmpty()) {
            return Component.translatable("ui.quantum_chromodynamic_charge.structure.material.none_active");
        }
        Component out = Component.empty();
        boolean first = true;
        for (StructureMaterialType type : visible) {
            Component line = Component.translatable(
                    "ui.quantum_chromodynamic_charge.structure.material.line",
                    type.displayName(), bank.get(type), cost.get(type));
            out = first ? line : Component.empty().append(out).append(Component.literal(" · ")).append(line);
            first = false;
        }
        Map<Item, Integer> slotCounts = StructureMaterialOps.countSlots(materialInventory);
        for (ExtraMaterial extra : structure.extraMaterials()) {
            if (extra.amount() <= 0) continue;
            int have = slotCounts.getOrDefault(extra.getItem(), 0);
            Component line = Component.translatable(
                    "ui.quantum_chromodynamic_charge.structure.material.extra_line",
                    extra.item().getHoverName(), have, extra.amount());
            out = first ? line : Component.empty().append(out).append(Component.literal(" · ")).append(line);
            first = false;
        }
        return out;
    }

    /**
     * 底栏部署区：仅状态 + 放置（导出已移至导出标签页）。
     */
    private UIElement buildDeployPanel() {
        var panel = new UIElement()
                .layout(l -> l.width(96).height(87).paddingAll(4).flexDirection(FlexDirection.COLUMN)
                        .gapAll(2).alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER))
                .style(s -> s.background(Sprites.BORDER_THICK_RT1));

        panel.addChild(new Label()
                .bindDataSource(SupplierDataSource.of(this::statusText))
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).fontSize(10)));

        panel.addChild(new Button()
                .setText(Component.translatable("ui.quantum_chromodynamic_charge.structure.place"))
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).fontSize(10))
                .setOnServerClick(e -> {
                    if (e.button == 0) startPlacement();
                })
                .layout(l -> l.heightAuto().widthAuto()));

        return panel;
    }

    private static Button actionButton(String langKey, Consumer<UIEvent> onClick) {
        return (Button) new Button()
                .setText(Component.translatable(langKey))
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(8))
                .setOnServerClick(onClick::accept)
                .layout(l -> l.heightAuto().widthAuto());
    }

    private Component statusText() {
        if (!taskCompleted) {
            return Component.translatable("ui.quantum_chromodynamic_charge.structure.progress", progress);
        }
        if (StructureExporter.isExporting()) {
            return Component.translatable("ui.quantum_chromodynamic_charge.structure.exporting");
        }
        if (!materialsAdequate) {
            return Component.translatable("ui.quantum_chromodynamic_charge.structure.material.insufficient");
        }
        return Component.translatable("ui.quantum_chromodynamic_charge.structure.idle");
    }

    /**
     * 蓝图页：选择器 + 摘要。
     * <p>
     * 下拉用于选择；摘要中将当前选中的下拉文案完整展示<strong>一次</strong>，再附类型/尺寸等元数据。
     * </p>
     */
    private UIElement buildBlueprintTab() {
        var scroller = createScrollerView();
        var panel = columnPanel();

        if (StructureTemplateRegistry.isEmpty()) {
            panel.addChild(muted("ui.quantum_chromodynamic_charge.structure.no_presets"));
            scroller.addScrollViewChild(panel);
            return scroller;
        }

        List<StructureDefinition.Preset> presets = StructureTemplateRegistry.presets();

        Selector<Integer> presetSel = createIndexSelector(
                presets.size(),
                presetIndex,
                i -> {
                    if (i == null || i < 0 || i >= presets.size()) return Component.literal("-");
                    return presets.get(i).title();
                });

        Selector<Integer> templateSel = createIndexSelector(
                StructureTemplateRegistry.getPreset(presetIndex).structures().size(),
                structureIndex,
                this::templateOptionLabel);

        presetSel.setOnValueChanged(i -> {
            if (i == null) return;
            rpcToServer("rpcSetPreset", i);
            List<StructureDefinition.Structure> next = StructureTemplateRegistry.getPreset(i).structures();
            templateSel.setCandidates(indexList(next.size()));
            templateSel.setCandidateUIProvider(idx -> wrapLabel(templateOptionLabelFor(next, idx)));
            templateSel.setSelected(0, false);
        });

        templateSel.setOnValueChanged(i -> {
            if (i != null) rpcToServer("rpcSetStructure", i);
        });

        panel.addChild(labeledControl(
                "ui.quantum_chromodynamic_charge.structure.preset", presetSel));
        panel.addChild(labeledControl(
                "ui.quantum_chromodynamic_charge.structure.template", templateSel));

        // 摘要分区：多行，每行信息量少、不与下拉重复堆叠
        panel.addChild(sectionTitle("ui.quantum_chromodynamic_charge.structure.summary"));
        panel.addChild(infoBlock(this::blueprintSummaryPreset));
        panel.addChild(infoBlock(this::blueprintSummaryTemplate));
        panel.addChild(infoBlock(this::blueprintSummaryTypeSize));
        panel.addChild(infoBlock(this::blueprintSummarySource));
        panel.addChild(infoBlock(this::blueprintSummaryIndex));

        scroller.addScrollViewChild(panel);
        return scroller;
    }

    /** 样板下拉：当前预设下的选项标签。 */
    private Component templateOptionLabel(Integer index) {
        if (StructureTemplateRegistry.isEmpty()) return Component.literal("-");
        return templateOptionLabelFor(
                StructureTemplateRegistry.getPreset(presetIndex).structures(), index);
    }

    /**
     * 下拉项标签：title · description（有描述时），否则仅 title。
     */
    private static Component templateOptionLabelFor(
                                                    List<StructureDefinition.Structure> list,
                                                    Integer index) {
        if (index == null || index < 0 || index >= list.size()) {
            return Component.literal("-");
        }
        return structureDropdownLabel(list.get(index));
    }

    /** 与样板下拉一致的选中文案。 */
    private static Component structureDropdownLabel(StructureDefinition.Structure s) {
        Component title = s.title();
        if (s.description() != null) {
            return Component.empty()
                    .append(title)
                    .append(Component.literal(" · "))
                    .append(s.description());
        }
        return title;
    }

    // —— 摘要多行：每行一类信息 ——

    /** 行1：当前预设库（与预设下拉文案一致，只在摘要出现一次）。 */
    private Component blueprintSummaryPreset() {
        if (StructureTemplateRegistry.isEmpty()) {
            return Component.translatable("ui.quantum_chromodynamic_charge.structure.no_presets");
        }
        return Component.translatable(
                "ui.quantum_chromodynamic_charge.structure.summary_preset",
                StructureTemplateRegistry.getPreset(presetIndex).title());
    }

    /** 行2：当前样板下拉文案（名称 · 变体）。 */
    private Component blueprintSummaryTemplate() {
        if (StructureTemplateRegistry.isEmpty()) return Component.empty();
        return Component.translatable(
                "ui.quantum_chromodynamic_charge.structure.summary_template",
                structureDropdownLabel(currentStructure()));
    }

    /** 行3：类型 + 方块尺寸。 */
    private Component blueprintSummaryTypeSize() {
        if (StructureTemplateRegistry.isEmpty()) return Component.empty();
        StructureDefinition.Structure s = currentStructure();
        Component size = Component.translatable(
                "ui.quantum_chromodynamic_charge.structure.size_short",
                s.sizeX(), s.sizeY(), s.sizeZ(), blocksToChunks(s.sizeX()), blocksToChunks(s.sizeZ()));
        if (s.type() != null) {
            return Component.translatable(
                    "ui.quantum_chromodynamic_charge.structure.summary_type_size",
                    s.type(), size);
        }
        return Component.translatable(
                "ui.quantum_chromodynamic_charge.structure.summary_size", size);
    }

    /** 方块边长 → 覆盖的区块数（向上取整，至少 0）。 */
    private static int blocksToChunks(int blocks) {
        if (blocks <= 0) return 0;
        return (blocks + CHUNK_EDGE_BLOCKS - 1) / CHUNK_EDGE_BLOCKS;
    }

    /** 行5：来源（无则空）。 */
    private Component blueprintSummarySource() {
        if (StructureTemplateRegistry.isEmpty()) return Component.empty();
        StructureDefinition.Structure s = currentStructure();
        if (s.source() == null) return Component.empty();
        return Component.translatable(
                "ui.quantum_chromodynamic_charge.structure.source", s.source());
    }

    /** 行6：样板序号。 */
    private Component blueprintSummaryIndex() {
        if (StructureTemplateRegistry.isEmpty()) return Component.empty();
        StructureDefinition.Preset p = StructureTemplateRegistry.getPreset(presetIndex);
        return Component.translatable(
                "ui.quantum_chromodynamic_charge.structure.template_index",
                structureIndex + 1,
                p.structures().size());
    }

    private static Selector<Integer> createIndexSelector(
                                                         int size,
                                                         int selected,
                                                         java.util.function.Function<Integer, Component> labelOf) {
        Selector<Integer> sel = new Selector<>();
        sel.setCandidateUIProvider(i -> wrapLabel(labelOf.apply(i)));
        sel.setCandidates(indexList(size));
        sel.setSelected(selected, false);
        sel.layout(l -> l.height(14).minWidth(SELECTOR_W));
        return sel;
    }

    /** 下拉项 / 展示标签：自适应宽高并自动换行。 */
    private static Label wrapLabel(Component text) {
        return (Label) new Label()
                .setText(text == null ? Component.literal("-") : text)
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(8))
                .layout(l -> l.width(SELECTOR_W - 8));
    }

    private static UIElement labeledControl(String labelKey, UIElement control) {
        var r = row();
        r.addChild(fixedLabel(labelKey, LABEL_W));
        r.addChild(control);
        return r;
    }

    /** 设置：偏移 + 双列开关 + 旋转/速度 + 边界。 */
    private UIElement buildSettingsTab() {
        var scroller = createScrollerView();
        var panel = columnPanel();

        panel.addChild(sectionTitle("ui.quantum_chromodynamic_charge.structure.offset_section"));
        panel.addChild(offsetRow("X", 0, () -> offsetX));
        panel.addChild(offsetRow("Y", 1, () -> offsetY));
        panel.addChild(offsetRow("Z", 2, () -> offsetZ));

        panel.addChild(sectionTitle("ui.quantum_chromodynamic_charge.structure.boundary"));
        panel.addChild(infoBlock(() -> {
            if (StructureTemplateRegistry.presetCount() <= 0) {
                return Component.translatable("ui.quantum_chromodynamic_charge.structure.no_presets");
            }
            StructureTransform.BlockPosBounds b = placementBounds();
            return Component.translatable(
                    "ui.quantum_chromodynamic_charge.structure.bounds_compact",
                    b.min().getX(), b.min().getY(), b.min().getZ(),
                    b.max().getX(), b.max().getY(), b.max().getZ());
        }));

        panel.addChild(sectionTitle("ui.quantum_chromodynamic_charge.structure.place_options_section"));
        var placeOpts = new UIElement().layout(l -> l
                .width(UI_PANEL_WIDTH - 12)
                .flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP)
                .gapAll(4));
        placeOpts.addChild(switchCell("ui.quantum_chromodynamic_charge.structure.skip_air", skipAir, "rpcSetSkipAir"));
        placeOpts.addChild(switchCell("ui.quantum_chromodynamic_charge.structure.skip_occupied", skipOccupied, "rpcSetSkipOccupied"));
        placeOpts.addChild(switchCell("ui.quantum_chromodynamic_charge.structure.update_light", updateLight, "rpcSetUpdateLight"));
        placeOpts.addChild(switchCell("ui.quantum_chromodynamic_charge.structure.update_heightmap", updateHeightmap, "rpcSetUpdateHeightmap"));
        panel.addChild(placeOpts);

        panel.addChild(sectionTitle("ui.quantum_chromodynamic_charge.structure.transform_section"));
        var switches = new UIElement().layout(l -> l
                .width(UI_PANEL_WIDTH - 12)
                .flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP)
                .gapAll(4));
        switches.addChild(switchCell("ui.quantum_chromodynamic_charge.structure.x_mirror", xMirror, "rpcSetXMirror"));
        switches.addChild(switchCell("ui.quantum_chromodynamic_charge.structure.z_mirror", zMirror, "rpcSetZMirror"));
        panel.addChild(switches);

        // 旋转 + 速度同一行
        var rotSpeed = row();
        rotSpeed.addChild(fixedLabel("ui.quantum_chromodynamic_charge.structure.rotation", 50));
        rotSpeed.addChild(valueLabel(() -> Component.literal(rotation + "°"), 32));
        rotSpeed.addChild(new Button()
                .setText(Component.literal("+90°"))
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true))
                .setOnServerClick(e -> {
                    if (e.button == 0) {
                        rotation = (rotation + 90) % 360;
                        markDirty("rotation");
                    }
                }));
        rotSpeed.addChild(fixedLabel("ui.quantum_chromodynamic_charge.structure.speed", 36));
        rotSpeed.addChild(valueLabel(() -> Component.literal(String.valueOf(speed)), 24));
        rotSpeed.addChild(new Button()
                .setText(Component.literal("-"))
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true))
                .setOnServerClick(e -> {
                    if (e.button == 0) {
                        speed = Mth.clamp(speed - 1, 10, 100);
                        markDirty("speed");
                    }
                }));
        rotSpeed.addChild(new Button().setText(Component.literal("+"))
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true))
                .setOnServerClick(e -> {
                    if (e.button == 0) {
                        speed = Mth.clamp(speed + 1, 10, 100);
                        markDirty("speed");
                    }
                }));
        panel.addChild(rotSpeed);

        scroller.addScrollViewChild(panel);
        return scroller;
    }

    // -------------------------------------------------------------------------
    // UI 组件工具
    // -------------------------------------------------------------------------

    private static ScrollerView createScrollerView() {
        ScrollerView list = new ScrollerView()
                .scrollerStyle(s -> s
                        .horizontalScrollDisplay(ScrollDisplay.NEVER)
                        .verticalScrollDisplay(ScrollDisplay.AUTO)
                        .scrollerViewStyle(0)
                        .mode(ScrollerMode.VERTICAL));
        list.layout(l -> l.width(UI_PANEL_WIDTH).height(UI_CONTENT_HEIGHT - 12))
                .style(s -> s.background(IGuiTexture.EMPTY));
        list.viewContainer.layout(l -> l.paddingAll(0)).style(s -> s.background(IGuiTexture.EMPTY));
        list.viewPort.layout(l -> l.paddingAll(0)).style(s -> s.background(IGuiTexture.EMPTY));
        return list;
    }

    private static UIElement columnPanel() {
        return new UIElement().layout(l -> l
                .width(UI_PANEL_WIDTH - 6)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(2)
                .alignItems(AlignItems.FLEX_START)
                .paddingAll(2));
    }

    private static List<Integer> indexList(int size) {
        List<Integer> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) ids.add(i);
        return ids;
    }

    private UIElement offsetRow(String axis, int axisId, java.util.function.IntSupplier value) {
        var r = row();
        r.addChild(new Label()
                .setText(Component.translatable("ui.quantum_chromodynamic_charge.structure.offset", axis))
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true)));
        r.addChild(valueLabel(() -> Component.literal(String.valueOf(value.getAsInt())), 28));
        for (int d : new int[] { -5, -1, 1, 5 }) {
            final int delta = d;
            String t = (delta > 0 ? "+" : "") + delta;
            r.addChild(new Button().setText(Component.literal(t))
                    .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true))
                    .setOnServerClick(e -> {
                        if (e.button == 0) applyOffset(axisId, delta);
                    }));
        }
        return r;
    }

    private void applyOffset(int axis, int delta) {
        switch (axis) {
            case 0 -> {
                offsetX += delta;
                markDirty("offsetX");
            }
            case 1 -> {
                offsetY += delta;
                markDirty("offsetY");
            }
            case 2 -> {
                offsetZ += delta;
                markDirty("offsetZ");
            }
        }
    }

    private UIElement switchCell(String key, boolean value, String rpc) {
        var cell = new UIElement().layout(l -> l
                .width(120)
                .flexDirection(FlexDirection.ROW)
                .gapAll(2)
                .alignItems(AlignItems.CENTER));
        cell.addChild(new Label()
                .setText(Component.translatable(key))
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(8))
                .layout(l -> l.width(88)));
        cell.addChild(new Switch()
                .setOn(value, false)
                .setOnSwitchChanged(on -> rpcToServer(rpc, on)));
        return cell;
    }

    private static UIElement row() {
        return new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(2)
                .alignItems(AlignItems.CENTER));
    }

    private static Label fixedLabel(String key, int width) {
        return (Label) new Label()
                .setText(Component.translatable(key))
                .textStyle(s -> s.adaptiveHeight(true).textWrap(TextWrap.WRAP))
                .layout(l -> l.width(width));
    }

    private static Label valueLabel(java.util.function.Supplier<Component> supplier, int width) {
        return (Label) new Label()
                .bindDataSource(SupplierDataSource.of(supplier))
                .textStyle(s -> s.adaptiveHeight(true).textWrap(TextWrap.WRAP))
                .layout(l -> l.width(width));
    }

    private static TextElement sectionTitle(String key) {
        return (TextElement) new TextElement()
                .setText(Component.translatable(key))
                .textStyle(s -> s.adaptiveHeight(true).textWrap(TextWrap.WRAP))
                .layout(l -> l.width(UI_PANEL_WIDTH - 14));
    }

    private static TextElement muted(String key) {
        return (TextElement) new TextElement()
                .setText(Component.translatable(key))
                .textStyle(s -> s.adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(7))
                .layout(l -> l.width(UI_PANEL_WIDTH - 14));
    }

    private static Label infoBlock(java.util.function.Supplier<Component> supplier) {
        return (Label) new Label()
                .bindDataSource(SupplierDataSource.of(supplier))
                .textStyle(s -> s.adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(8))
                .layout(l -> l.width(UI_PANEL_WIDTH - 14));
    }

    private static UIElement slotGrid(ObservableItemResourceHandler handler, int size, int cols) {
        var grid = new UIElement().layout(l -> l.width(18 * cols)
                .flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP));
        for (int i = 0; i < size; i++) {
            grid.addChild(new ItemSlot().bind(handler, i));
        }
        return grid;
    }
}
