package com.danilon4ig.wt_quests.block;

import com.danilon4ig.wt_quests.block.entity.ModBlockEntities;
import com.danilon4ig.wt_quests.block.entity.TreasureBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TreasureBoxBlock extends BaseEntityBlock {
    public static final VoxelShape TREASURE_SHAPE = Block.box(0, 0, 2, 16, 12, 14);

    public TreasureBoxBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState p_60555_, @NotNull BlockGetter p_60556_, @NotNull BlockPos p_60557_, @NotNull CollisionContext p_60558_) {
        return TREASURE_SHAPE;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState p_49232_) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand interactionHand, @NotNull BlockHitResult result) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof TreasureBoxBlockEntity box) {
                if (!box.isOwner(player)) {
                    player.displayClientMessage(Component.translatable("block.wt_quests.starter_treasure.not_owner"), true);
                    return InteractionResult.FAIL;
                }
                NetworkHooks.openScreen(((ServerPlayer)player), box, pos);
            } else {
              throw new IllegalStateException("Container provider is missing!");
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean canHarvestBlock(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Player player) {
        return false;
    }

    @Override
    public float getExplosionResistance() {
        return Float.MAX_VALUE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new TreasureBoxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.TREASURE_BOX_BE.get(), TreasureBoxBlockEntity::tick);
    }
}
