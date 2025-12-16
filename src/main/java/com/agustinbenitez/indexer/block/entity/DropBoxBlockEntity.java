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
import net.minecraft.world.level.block.Block;
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
        // Initialize the hadItemsLastTick variable based on current inventory state
        this.hadItemsLastTick = hasItems();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
    }
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack oldStack = this.items.get(slot);
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
        
        // Notificar a los controladores si se añadió un ítem (antes vacío, ahora no)
        if (this.level != null && !this.level.isClientSide() &&
            (oldStack.isEmpty() && !stack.isEmpty()) || (!oldStack.isEmpty() && stack.isEmpty())) {
            notifyNearbyControllers(this.level, this.worldPosition);
        }
    }

    // Variable para rastrear si había ítems en el tick anterior
    private boolean hadItemsLastTick = false;
    
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        
        // Verificar si el estado de los ítems ha cambiado
        boolean hasItemsNow = hasItems();
        
        // Si el estado cambió de tener ítems a no tenerlos, notificar a los controladores
        if (hadItemsLastTick && !hasItemsNow) {
            notifyNearbyControllers(level, pos);
        }
        // Si hay ítems nuevos, notificar a los controladores
        else if (!hadItemsLastTick && hasItemsNow) {
            notifyNearbyControllers(level, pos);
        }
        
        // Actualizar el estado para el próximo tick
        hadItemsLastTick = hasItemsNow;
    }
    
    private void notifyNearbyControllers(Level level, BlockPos pos) {
        if (level == null) return;
        
        // Primero, buscar controladores en posiciones adyacentes para una respuesta rápida
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockEntity blockEntity = level.getBlockEntity(adjacentPos);
            
            if (blockEntity instanceof IndexerControllerBlockEntity controller) {
                // Marcar que la red ha cambiado
                controller.markNetworkChanged();
                
                // Si el controlador está habilitado, programar una transferencia inmediata
                if (controller.isEnabled()) {
                    level.scheduleTick(adjacentPos, level.getBlockState(adjacentPos).getBlock(), 1);
                }
                
                // Ya encontramos un controlador adyacente, no necesitamos buscar más
                return;
            }
        }
        
        // Si no encontramos controladores adyacentes, buscar en un radio más amplio
        int searchRadius = 16;
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    // Saltar la posición central que ya verificamos
                    if (x == 0 && y == 0 && z == 0) continue;
                    
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockEntity blockEntity = level.getBlockEntity(checkPos);
                    
                    if (blockEntity instanceof IndexerControllerBlockEntity controller) {
                        // Marcar que la red ha cambiado
                        controller.markNetworkChanged();
                        // Solo necesitamos notificar a un controlador, ya que cada uno gestionará su propia red
                        return;
                    }
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
    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) {
            this.setChanged();
            // Notify controllers when items are removed
            if (this.level != null && !this.level.isClientSide()) {
                notifyNearbyControllers(this.level, this.worldPosition);
            }
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
    
    // Method to find the connected IndexerController
    @Nullable
    public IndexerControllerBlockEntity findConnectedController() {
        if (this.level == null) return null;
        
        // Use BFS to find a connected controller through pipes
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
        
        queue.add(this.worldPosition);
        visited.add(this.worldPosition);
        
        int maxSearch = 1000; // Safety limit
        int count = 0;
        
        while (!queue.isEmpty() && count < maxSearch) {
            BlockPos currentPos = queue.poll();
            count++;
            
            // Check all 6 directions
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(direction);
                if (visited.contains(neighborPos)) continue;
                
                BlockState neighborState = this.level.getBlockState(neighborPos);
                Block neighborBlock = neighborState.getBlock();
                
                // If we found a controller directly connected
                if (neighborBlock instanceof com.agustinbenitez.indexer.block.IndexerControllerBlock) {
                    // If we are at the DropBox (start), we can connect to any adjacent controller
                    // If we are at a pipe, the pipe must be connected to this side
                    boolean connected = false;
                    if (currentPos.equals(this.worldPosition)) {
                        connected = true;
                    } else {
                        BlockState currentState = this.level.getBlockState(currentPos);
                        if (currentState.getBlock() instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock) {
                            connected = currentState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction));
                        }
                    }
                    
                    if (connected) {
                        BlockEntity blockEntity = this.level.getBlockEntity(neighborPos);
                        if (blockEntity instanceof IndexerControllerBlockEntity controller) {
                            return controller;
                        }
                    }
                }
                // If it's a pipe, check connections
                else if (neighborBlock instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock) {
                    boolean connected = false;
                    
                    // Logic for connection between current block and neighbor pipe
                    if (currentPos.equals(this.worldPosition)) {
                        // From DropBox to Pipe: Check if pipe connects to DropBox
                        // Pipe connects to DropBox if its property for opposite direction is true
                        connected = neighborState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()));
                    } else {
                        // From Pipe to Pipe: Check both ends
                        BlockState currentState = this.level.getBlockState(currentPos);
                        if (currentState.getBlock() instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock) {
                            boolean mySide = currentState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction));
                            boolean otherSide = neighborState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()));
                            connected = mySide && otherSide;
                        }
                    }
                    
                    if (connected) {
                        visited.add(neighborPos);
                        queue.add(neighborPos);
                    }
                }
            }
        }
        
        return null;
    }
    

}