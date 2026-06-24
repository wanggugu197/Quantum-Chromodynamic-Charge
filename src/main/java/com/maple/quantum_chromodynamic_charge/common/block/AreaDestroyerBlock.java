package com.maple.quantum_chromodynamic_charge.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import com.lowdragmc.lowdraglib2.LDLib2;
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
 * 区域破坏器方块。
 * <p>
 * 库存通过 LDLib2 {@code @DropSaved} 写入掉落物 / 中键复制，放置时再读回 BE。
 */
public class AreaDestroyerBlock extends BaseRotatedBlock implements BlockUIMenuType.BlockUI {

    public static final MapCodec<AreaDestroyerBlock> CODEC = simpleCodec(AreaDestroyerBlock::new);

    public AreaDestroyerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseRotatedBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new AreaDestroyerBlockEntity(pos, state);
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
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        if (level.getBlockEntity(pos) instanceof IPersistManagedHolder holder) {
            Optional.ofNullable(stack.get(DataComponents.CUSTOM_DATA)).ifPresent(data -> {
                try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                    var input = TagValueInput.create(reporter, level.registryAccess(), data.copyTag());
                    holder.loadManagedPersistentData(input);
                }
            });
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof IPersistManagedHolder holder && be.getLevel() instanceof Level level) {
            ItemStack drop = new ItemStack(this);
            try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                var output = TagValueOutput.createWithContext(reporter, level.registryAccess());
                holder.saveManagedPersistentData(output, true);
                drop.set(DataComponents.CUSTOM_DATA, CustomData.of(output.buildResult()));
            }
            return List.of(drop);
        }
        return super.getDrops(state, params);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state,
                                       boolean includeData, Player player) {
        if (level.getBlockEntity(pos) instanceof IPersistManagedHolder holder) {
            ItemStack clone = new ItemStack(this);
            if (includeData) {
                try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                    var output = TagValueOutput.createWithContext(reporter, level.registryAccess());
                    holder.saveManagedPersistentData(output, true);
                    clone.set(DataComponents.CUSTOM_DATA, CustomData.of(output.buildResult()));
                }
            }
            return clone;
        }
        return super.getCloneItemStack(level, pos, state, includeData, player);
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        if (holder.player.level().getBlockEntity(holder.pos) instanceof AreaDestroyerBlockEntity be) {
            return be.createUI(holder);
        }
        return null;
    }
}
