package com.agustinbenitez.indexer.block.entity;

import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.init.ModBlockEntities;
import com.agustinbenitez.indexer.menu.DropBoxMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class DropBoxBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    private static final int CONTAINER_SIZE = 54; // 6 rows of 9 slots = 54 slots (double the size of a normal chest)
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private static final int[] SLOTS = new int[CONTAINER_SIZE];
    
    static {
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            SLOTS[i] = i;
        }
    }

    public DropBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DROP_BOX.get(), pos, state);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.indexer.drop_box");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new DropBoxMenu(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        
        // Check if there are new items to notify nearby controllers
        if (hasItems()) {
            notifyNearbyControllers(level, pos);
        }
    }
    
    private void notifyNearbyControllers(Level level, BlockPos pos) {
        // Search for controllers in adjacent positions
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockEntity blockEntity = level.getBlockEntity(adjacentPos);
            
            if (blockEntity instanceof IndexerControllerBlockEntity controller) {
                // If the controller is enabled, try to transfer items immediately
                if (controller.isEnabled()) {
                    // Force an immediate transfer
                    level.scheduleTick(adjacentPos, level.getBlockState(adjacentPos).getBlock(), 1);
                }
            }
        }
    }

    // Implementation of WorldlyContainer to allow hoppers and other systems to access
    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return true; // Allows inserting items from any side
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true; // Allows extracting items from any side
    }

    // Method for the controller to easily access items
    public NonNullList<ItemStack> getAllItems() {
        return this.items;
    }

    // Method to check if the DropBox has items
    public boolean hasItems() {
        for (ItemStack item : this.items) {
            if (!item.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // Method to get the first non-empty item
    public ItemStack getFirstNonEmptyItem() {
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack item = this.items.get(i);
            if (!item.isEmpty()) {
                return item;
            }
        }
        return ItemStack.EMPTY;
    }

    // Method to remove a specific item from the inventory
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }
    
    // Method to drop contents when the block is broken
    public void dropContents() {
        if (this.level == null) return;
        
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemStack = this.items.get(i);
            if (!itemStack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), itemStack);
            }
        }
    }
}