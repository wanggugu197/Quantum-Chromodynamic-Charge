package com.maple.quantum_chromodynamic_charge.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.syncdata.holder.IPersistManagedHolder;
import com.mapleutillib.api.baseBlock.BaseRotatedBlock;
import com.mojang.serialization.MapCodec;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 结构放置器方块。库存与设置通过 {@code @DropSaved} 存入掉落物。
 */
public class StructurePlacerBlock extends BaseRotatedBlock implements BlockUIMenuType.BlockUI {

    public static final MapCodec<StructurePlacerBlock> CODEC = simpleCodec(StructurePlacerBlock::new);

    public StructurePlacerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseRotatedBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new StructurePlacerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockUIMenuType.openUI(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof IPersistManagedHolder holder) {
                if (stack.has(DataComponents.CUSTOM_DATA)) {
                    CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
                    if (tag != null && !tag.isEmpty()) {
                        holder.loadManagedPersistentData(level.registryAccess(), tag);
                    }
                }
            }
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Optional<BlockEntity> be = Optional.ofNullable(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY));
        if (be.isPresent() && be.get() instanceof IPersistManagedHolder holder && be.get().getLevel() instanceof Level level) {
            ItemStack drop = new ItemStack(this);
            CompoundTag tag = new CompoundTag();
            holder.saveManagedPersistentData(level.registryAccess(), tag, true);
            if (!tag.isEmpty()) {
                drop.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
            return List.of(drop);
        }
        return super.getDrops(state, params);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof IPersistManagedHolder holder) {
            ItemStack clone = new ItemStack(this);
            if (level instanceof Level lvl) {
                CompoundTag tag = new CompoundTag();
                holder.saveManagedPersistentData(lvl.registryAccess(), tag, true);
                if (!tag.isEmpty()) {
                    clone.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
            }
            return clone;
        }
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        if (holder.player.level().getBlockEntity(holder.pos) instanceof StructurePlacerBlockEntity be) {
            return be.createUI(holder);
        }
        return null;
    }
}
