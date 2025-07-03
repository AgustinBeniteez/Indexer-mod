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
    private static final int CONTAINER_SIZE = 54; // 6 filas de 9 slots = 54 slots (doble de un cofre normal)
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
        
        // Verificar si hay items nuevos para notificar a los controladores cercanos
        if (hasItems()) {
            notifyNearbyControllers(level, pos);
        }
    }
    
    private void notifyNearbyControllers(Level level, BlockPos pos) {
        // Buscar controladores en las posiciones adyacentes
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockEntity blockEntity = level.getBlockEntity(adjacentPos);
            
            if (blockEntity instanceof IndexerControllerBlockEntity controller) {
                // Si el controlador está habilitado, intentar transferir items inmediatamente
                if (controller.isEnabled()) {
                    // Forzar una transferencia inmediata
                    level.scheduleTick(adjacentPos, level.getBlockState(adjacentPos).getBlock(), 1);
                }
            }
        }
    }

    // Implementación de WorldlyContainer para permitir que hoppers y otros sistemas accedan
    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return true; // Permite insertar items desde cualquier lado
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true; // Permite extraer items desde cualquier lado
    }

    // Método para que el controlador pueda acceder fácilmente a los items
    public NonNullList<ItemStack> getAllItems() {
        return this.items;
    }

    // Método para verificar si el DropBox tiene items
    public boolean hasItems() {
        for (ItemStack item : this.items) {
            if (!item.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // Método para obtener el primer item no vacío
    public ItemStack getFirstNonEmptyItem() {
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack item = this.items.get(i);
            if (!item.isEmpty()) {
                return item;
            }
        }
        return ItemStack.EMPTY;
    }

    // Método para remover un item específico del inventario
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }
    
    // Método para soltar el contenido cuando se rompe el bloque
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