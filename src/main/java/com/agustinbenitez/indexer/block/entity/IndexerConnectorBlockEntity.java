package com.agustinbenitez.indexer.block.entity;

import com.agustinbenitez.indexer.block.IndexerConnectorBlock;
import com.agustinbenitez.indexer.init.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class IndexerConnectorBlockEntity extends RandomizableContainerBlockEntity {
    private ItemStack filterItem = ItemStack.EMPTY;
    private BlockPos connectedChestPos = null;
    private net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(1, ItemStack.EMPTY);

    public IndexerConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDEXER_CONNECTOR.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.indexer.connector");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, id, inventory, this, 1);
    }

    @Override
    public int getContainerSize() {
        return 1; // Solo un slot para el filtro
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("FilterItem")) {
            this.filterItem = ItemStack.of(tag.getCompound("FilterItem"));
        }
        if (tag.contains("ChestX") && tag.contains("ChestY") && tag.contains("ChestZ")) {
            this.connectedChestPos = new BlockPos(
                    tag.getInt("ChestX"),
                    tag.getInt("ChestY"),
                    tag.getInt("ChestZ")
            );
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!this.filterItem.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            this.filterItem.save(itemTag);
            tag.put("FilterItem", itemTag);
        }
        if (this.connectedChestPos != null) {
            tag.putInt("ChestX", this.connectedChestPos.getX());
            tag.putInt("ChestY", this.connectedChestPos.getY());
            tag.putInt("ChestZ", this.connectedChestPos.getZ());
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IndexerConnectorBlockEntity entity) {
        if (level.isClientSide()) return;

        // Verificar si hay un cofre conectado
        if (entity.connectedChestPos == null) {
            entity.updateConnectedChest();
        }
    }

    public void updateConnectedChest() {
        if (this.level == null) return;

        this.connectedChestPos = null;
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockState adjacentState = this.level.getBlockState(adjacentPos);
            if (adjacentState.getBlock() instanceof ChestBlock) {
                this.connectedChestPos = adjacentPos;
                this.setChanged();
                return;
            }
        }
    }

    public boolean canAcceptItem(ItemStack stack) {
        if (this.filterItem.isEmpty()) {
            return false; // No hay filtro configurado
        }

        if (this.connectedChestPos == null) {
            return false; // No hay cofre conectado
        }

        // Verificar si el ítem coincide con el filtro
        return this.filterItem.getItem() == stack.getItem();
    }

    public ItemStack insertItem(ItemStack stack) {
        if (!canAcceptItem(stack) || this.level == null) {
            return stack;
        }

        BlockEntity chestEntity = this.level.getBlockEntity(this.connectedChestPos);
        if (!(chestEntity instanceof Container)) {
            return stack;
        }

        Container container = (Container) chestEntity;
        ItemStack remainder = stack.copy();

        // Intentar insertar en el cofre
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotStack = container.getItem(i);
            
            if (slotStack.isEmpty()) {
                // Slot vacío, insertar todo lo que podamos
                int maxStackSize = Math.min(container.getMaxStackSize(), remainder.getMaxStackSize());
                int toInsert = Math.min(remainder.getCount(), maxStackSize);
                
                ItemStack newStack = remainder.copy();
                newStack.setCount(toInsert);
                container.setItem(i, newStack);
                
                remainder.shrink(toInsert);
                if (remainder.isEmpty()) {
                    break;
                }
            } else if (ItemStack.isSameItemSameTags(slotStack, remainder)) {
                // Mismo ítem, intentar apilar
                int maxStackSize = Math.min(container.getMaxStackSize(), slotStack.getMaxStackSize());
                int space = maxStackSize - slotStack.getCount();
                
                if (space > 0) {
                    int toInsert = Math.min(remainder.getCount(), space);
                    slotStack.grow(toInsert);
                    remainder.shrink(toInsert);
                    
                    if (remainder.isEmpty()) {
                        break;
                    }
                }
            }
        }

        if (chestEntity instanceof ChestBlockEntity) {
            ((ChestBlockEntity) chestEntity).setChanged();
        }

        return remainder;
    }

    public ItemStack getFilterItem() {
        return this.filterItem;
    }

    public void setFilterItem(ItemStack stack) {
        this.filterItem = stack.copy();
        this.filterItem.setCount(1); // Solo guardamos 1 para el filtro
        this.setChanged();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? this.filterItem : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == 0 && !this.filterItem.isEmpty()) {
            ItemStack result = this.filterItem.copy();
            this.filterItem = ItemStack.EMPTY;
            this.setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == 0) {
            ItemStack result = this.filterItem;
            this.filterItem = ItemStack.EMPTY;
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            this.filterItem = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            if (!this.filterItem.isEmpty()) {
                this.filterItem.setCount(1);
            }
            this.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public void clearContent() {
        this.filterItem = ItemStack.EMPTY;
        this.items.clear();
    }
    
    @Override
    protected net.minecraft.core.NonNullList<ItemStack> getItems() {
        return this.items;
    }
    
    @Override
    protected void setItems(net.minecraft.core.NonNullList<ItemStack> items) {
        this.items = items;
        if (!items.isEmpty()) {
            this.filterItem = items.get(0);
        } else {
            this.filterItem = ItemStack.EMPTY;
        }
    }
}