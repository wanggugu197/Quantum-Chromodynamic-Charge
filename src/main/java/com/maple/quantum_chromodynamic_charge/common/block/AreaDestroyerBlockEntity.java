package com.maple.quantum_chromodynamic_charge.common.block;

import com.maple.quantum_chromodynamic_charge.common.QCCDataComponent;
import com.maple.quantum_chromodynamic_charge.common.QCCRegistration;
import com.maple.quantum_chromodynamic_charge.config.QuantumChromodynamicChargeConfig;
import com.maple.quantum_chromodynamic_charge.explosion.AreaExplosion;
import com.maple.quantum_chromodynamic_charge.explosion.ChunkExplosion;
import com.maple.quantum_chromodynamic_charge.explosion.SphereExplosion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
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
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * 区域破坏器 BE：注解同步 + 装药/坐标库存 + Switch/Selector UI。
 */
public class AreaDestroyerBlockEntity extends DirectionBlockEntity implements ISyncPersistRPCBlockEntity {

    public static final int EXPLOSIVE_SLOTS = 8;
    public static final int COORD_SLOTS = 2;

    public enum ExplosionMode {

        SPHERE,
        CHUNK,
        AREA;

        Component label() {
            return Component.translatable(
                    "ui.quantum_chromodynamic_charge.area_destroyer.mode." + name().toLowerCase());
        }

        static ExplosionMode of(int ordinal) {
            ExplosionMode[] v = values();
            return ordinal >= 0 && ordinal < v.length ? v[ordinal] : SPHERE;
        }
    }

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    @DropSaved
    @Getter
    private final ObservableItemResourceHandler explosiveInventory = new ObservableItemResourceHandler(EXPLOSIVE_SLOTS);

    @Persisted
    @DescSynced
    @DropSaved
    @Getter
    private final ObservableItemResourceHandler coordinateInventory = new ObservableItemResourceHandler(COORD_SLOTS);

    @Persisted
    @DescSynced
    private boolean enabled;

    @Persisted
    @DescSynced
    private ExplosionMode mode = ExplosionMode.SPHERE;

    @Persisted
    @DescSynced
    private boolean updateHeightmap = true;

    @Persisted
    @DescSynced
    private boolean updateLight = true;

    /** UI 用当量；由装药/模式推导，不持久化。 */
    @DescSynced
    private int explosiveYield;

    public AreaDestroyerBlockEntity(BlockPos pos, BlockState state) {
        this(QCCRegistration.AREA_DESTROYER_ENTITY.get(), pos, state);
    }

    protected AreaDestroyerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        bindInventory(explosiveInventory, "explosiveInventory", null);
        bindInventory(coordinateInventory, "coordinateInventory",
                new ItemAllowListFilter(Set.of(QCCRegistration.COORDINATE_POSITIONING_CARD.asItem()), Set.of()));
    }

    private void bindInventory(ObservableItemResourceHandler inv, String field,
                               @Nullable ItemAllowListFilter filter) {
        inv.setContext(this);
        if (filter != null) inv.setIOFilter(filter);
        inv.setOnChanged((slot, prev) -> {
            markDirty(field);
            refreshYield();
        });
    }

    // -------------------------------------------------------------------------
    // 当量
    // -------------------------------------------------------------------------

    private void refreshYield() {
        Level level = getLevel();
        explosiveYield = level == null ? 0 : computeYield(level);
        markDirty("explosiveYield");
    }

    private int computeYield(Level level) {
        long energy = 0L;
        for (int i = 0; i < explosiveInventory.size(); i++) {
            energy += energyOf(explosiveInventory.getStackInSlot(i));
        }
        if (energy <= 0) return 0;

        return switch (mode == null ? ExplosionMode.SPHERE : mode) {
            case SPHERE -> (int) Math.cbrt(energy / 4.0) * 10;
            case CHUNK -> (int) Math.sqrt(energy / (double) Math.max(1, level.getHeight())) * 16;
            case AREA -> {
                BlockPos[] c = coords();
                if (c[0] == null || c[1] == null) yield 0;
                int vol = axisVolume(c[0], c[1]);
                yield energy > vol ? Math.max(1, vol / 200_000) : -1;
            }
        };
    }

    private static long energyOf(ItemStack stack) {
        if (stack.isEmpty()) return 0L;
        Item item = stack.getItem();
        long u = item == QCCRegistration.POWDER_BARREL.asItem() ? 20L : item == QCCRegistration.BIGGER_TNT.asItem() ? 30L : item == QCCRegistration.NUCLEAR_BOMB.asItem() ? 2048L : item == QCCRegistration.NAQUADRIA_CHARGE.asItem() ? 3200L : item == QCCRegistration.LEPTONIC_CHARGE.asItem() ? 2_048_000L : item == QCCRegistration.QUANTUM_CHROMODYNAMIC_CHARGE.asItem() ? 32_000_000L : 0L;
        return u * stack.getCount();
    }

    /** 从坐标卡槽读取最多两个有效坐标。 */
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

    @Nullable
    private static BlockPos readCoord(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != QCCRegistration.COORDINATE_POSITIONING_CARD.asItem()) {
            return null;
        }
        Vec3i v = QCCDataComponent.COORDINATE.get(stack);
        return v == null || v.equals(Vec3i.ZERO) ? null : new BlockPos(v);
    }

    private static int axisVolume(BlockPos a, BlockPos b) {
        return Math.max(0, Math.abs(a.getX() - b.getX()) / 10) * Math.max(0, Math.abs(a.getY() - b.getY()) / 10) * Math.max(0, Math.abs(a.getZ() - b.getZ()) / 10);
    }

    // -------------------------------------------------------------------------
    // 引爆
    // -------------------------------------------------------------------------

    public void triggerExplosion() {
        Level level = getLevel();
        if (level == null || level.isClientSide() || !enabled) return;
        if (!QuantumChromodynamicChargeConfig.INSTANCE.explosion.enableAreaDestroyerClearing) return;

        refreshYield();
        if (explosiveYield <= 0) return;

        BlockPos self = getBlockPos();
        ExplosionMode m = mode == null ? ExplosionMode.SPHERE : mode;
        switch (m) {
            case SPHERE -> SphereExplosion.explosion(self, level, explosiveYield, updateHeightmap, updateLight, false, false);
            case CHUNK -> ChunkExplosion.explosion(self, level, explosiveYield, updateHeightmap, updateLight, false, false);
            case AREA -> {
                BlockPos[] c = coords();
                if (c[0] == null || c[1] == null) return;
                AreaExplosion.explosion(self, c[0], c[1], level, updateHeightmap, updateLight, false, false);
            }
        }

        for (int i = 0; i < explosiveInventory.size(); i++) {
            explosiveInventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        markDirty("explosiveInventory");
        refreshYield();
    }

    // -------------------------------------------------------------------------
    // RPC C2S
    // -------------------------------------------------------------------------

    @RPCMethod
    public void rpcSetEnabled(RPCSender sender, boolean value) {
        if (sender.isServer()) return;
        enabled = value;
    }

    @RPCMethod
    public void rpcSetMode(RPCSender sender, int ordinal) {
        if (sender.isServer()) return;
        mode = ExplosionMode.of(ordinal);
        refreshYield();
    }

    @RPCMethod
    public void rpcSetUpdateHeightmap(RPCSender sender, boolean value) {
        if (sender.isServer()) return;
        updateHeightmap = value;
    }

    @RPCMethod
    public void rpcSetUpdateLight(RPCSender sender, boolean value) {
        if (sender.isServer()) return;
        updateLight = value;
    }

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        refreshYield();

        var all = new UIElement().layout(l -> l.alignItems(AlignItems.CENTER).gapAll(2));

        var root = new UIElement()
                .style(s -> s.background(Sprites.BORDER_THICK_RT1))
                .layout(l -> l.flexDirection(FlexDirection.COLUMN).gapAll(4)
                        .alignItems(AlignItems.CENTER).paddingAll(6));
        root.addChild(label(Component.translatable("block.quantum_chromodynamic_charge.area_destroyer")));

        // 模式
        var modeRow = row();
        modeRow.addChild(label(Component.translatable("ui.quantum_chromodynamic_charge.area_destroyer.mode")));
        Selector<ExplosionMode> selector = new Selector<>();
        selector.setCandidateUIProvider(m -> new Label().setText(m == null ? Component.literal("-") : m.label()));
        selector.setCandidates(List.of(ExplosionMode.values()));
        selector.setSelected(mode == null ? ExplosionMode.SPHERE : mode, false);
        selector.setOnValueChanged(m -> { if (m != null) rpcToServer("rpcSetMode", m.ordinal()); });
        selector.layout(l -> l.height(16).width(100));
        modeRow.addChild(selector);
        root.addChild(modeRow);

        // 状态
        root.addChild(new Label()
                .bindDataSource(SupplierDataSource.of(() -> Component.translatable("ui.quantum_chromodynamic_charge.area_destroyer.status", explosiveYield)))
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(9))
                .layout(l -> l.width(170)));

        var optionsRow = row();
        optionsRow.addChild(switchCell("ui.quantum_chromodynamic_charge.area_destroyer.update_heightmap", updateHeightmap, "rpcSetUpdateHeightmap"));
        optionsRow.addChild(switchCell("ui.quantum_chromodynamic_charge.area_destroyer.update_light", updateLight, "rpcSetUpdateLight"));
        root.addChild(optionsRow);

        var explosiveRow = row();
        explosiveRow.addChild(label(Component.translatable("ui.quantum_chromodynamic_charge.area_destroyer.explosives")));
        explosiveRow.addChild(slotGrid(explosiveInventory, EXPLOSIVE_SLOTS, 8));
        root.addChild(explosiveRow);

        var cardRow = row();
        cardRow.addChild(label(Component.translatable("ui.quantum_chromodynamic_charge.area_destroyer.coordinates")));
        cardRow.addChild(slotGrid(coordinateInventory, COORD_SLOTS, 2));
        root.addChild(cardRow);

        // 开关与引爆
        var enableRow = row();
        enableRow.addChild(switchCell("ui.quantum_chromodynamic_charge.area_destroyer.enabled", enabled, "rpcSetEnabled"));
        enableRow.addChild(new Button()
                .setText(Component.translatable("ui.quantum_chromodynamic_charge.area_destroyer.detonate"))
                .textStyle(s -> s.adaptiveWidth(true))
                .setOnServerClick(e -> { if (e.button == 0) triggerExplosion(); })
                .layout(l -> l.height(16)));
        root.addChild(enableRow);

        all.addChild(root);

        all.addChild(new InventorySlots()
                .layout(l -> l.paddingAll(6))
                .style(s -> s.background(Sprites.BORDER_THICK_RT1)));

        return new ModularUI(
                UI.of(all, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP_MERGED))),
                holder.player);
    }

    private static UIElement row() {
        return new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(2)
                .alignItems(AlignItems.CENTER));
    }

    private static Label label(Component text) {
        return (Label) new Label().setText(text)
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true));
    }

    private static UIElement slotGrid(ObservableItemResourceHandler handler, int size, int cols) {
        var grid = new UIElement().layout(l -> l.width(18 * cols)
                .flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP));
        for (int i = 0; i < size; i++) {
            grid.addChild(new ItemSlot().bind(handler, i));
        }
        return grid;
    }

    private UIElement switchCell(String key, boolean value, String rpc) {
        var cell = row();
        cell.addChild(label(Component.translatable(key)));
        cell.addChild(new Switch()
                .setOn(value, false)
                .setOnSwitchChanged(on -> rpcToServer(rpc, on)));
        return cell;
    }
}
