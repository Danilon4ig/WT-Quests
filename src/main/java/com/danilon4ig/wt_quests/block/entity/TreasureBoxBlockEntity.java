package com.danilon4ig.wt_quests.block.entity;

import com.danilon4ig.wt_quests.command.QuestCommand;
import com.danilon4ig.wt_quests.gui.TreasureBoxMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TreasureBoxBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemStackHandler = new ItemStackHandler(9);
    private UUID owner;

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData containerData;

    public TreasureBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TREASURE_BOX_BE.get(), pos, state);
        this.containerData = new ContainerData() {
            @Override
            public int get(int p_39284_) {
                return 0;
            }

            @Override
            public void set(int p_39285_, int p_39286_) {

            }

            @Override
            public int getCount() {
                return 0;
            }
        };
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemStackHandler);
        if (level != null && !level.isClientSide() && owner != null && !isInventoryEmpty()) {
            QuestCommand.markChestActive(owner, worldPosition);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("gui.wt_quests.starter_treasue_title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new TreasureBoxMenu(containerId, inventory, this, this.containerData);
    }

    public ItemStackHandler getItemStackHandler() {
        return itemStackHandler;
    }

    public void setOwner(UUID uuid) {
        this.owner = uuid;
        setChanged();
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public boolean isOwner(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        tag.put("inventory", itemStackHandler.serializeNBT());
        if (owner != null) tag.putUUID("Owner", owner);
        super.saveAdditional(tag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        itemStackHandler.deserializeNBT(tag.getCompound("inventory"));
        if (tag.contains("Owner")) owner = tag.getUUID("Owner");
        super.load(tag);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TreasureBoxBlockEntity blockEntity) {
        if (blockEntity.isInventoryEmpty()) {
            blockEntity.destroyTreasure(level, pos);
        }
    }

    private void destroyTreasure(Level level, BlockPos pos) {
        if (level != null && !level.isClientSide()) {
            if (owner != null) QuestCommand.markChestLooted(owner);
            level.destroyBlock(pos, true);
        }
    }

    public boolean isInventoryEmpty() {
        return itemStackHandler.serializeNBT().getList("Items", 10).isEmpty();
    }
}
