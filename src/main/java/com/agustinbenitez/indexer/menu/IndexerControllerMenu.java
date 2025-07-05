package com.agustinbenitez.indexer.menu;

import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class IndexerControllerMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final IndexerControllerBlockEntity blockEntity;
    
    // Índices de datos para sincronización
    public static final int ENABLED_INDEX = 0;
    public static final int HAS_DROP_CONTAINER_INDEX = 1;
    public static final int CONNECTED_CONTAINERS_COUNT_INDEX = 2;
    public static final int TOTAL_SLOTS_INDEX = 3;
    public static final int ITEMS_PER_TRANSFER_INDEX = 4;
    public static final int DATA_COUNT = 5;
    
    // Constructor para el lado del servidor
    public IndexerControllerMenu(int containerId, Inventory inventory, IndexerControllerBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.INDEXER_CONTROLLER_MENU.get(), containerId);
        this.blockEntity = entity;
        this.data = data;
        
        // No hay slots de inventario ya que no hay inventario interno
        // Tampoco añadimos slots del inventario del jugador para simplificar la interfaz
        
        // Añadir datos para sincronización
        addDataSlots(data);
    }
    
    // Constructor para el lado del cliente
    public IndexerControllerMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, getBlockEntity(inventory, extraData), new SimpleContainerData(DATA_COUNT));
    }
    
    private static IndexerControllerBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf extraData) {
        BlockEntity entity = inventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (entity instanceof IndexerControllerBlockEntity) {
            return (IndexerControllerBlockEntity) entity;
        }
        throw new IllegalStateException("Block entity is not correct!");
    }
    
    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity != null && this.blockEntity.stillValid(player);
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No hay inventario interno, así que simplemente devolvemos el ItemStack sin cambios
        return ItemStack.EMPTY;
    }
    
    // Métodos para acceder a los datos sincronizados
    public boolean isEnabled() {
        return this.data.get(ENABLED_INDEX) == 1;
    }
    
    public boolean hasDropContainer() {
        return this.data.get(HAS_DROP_CONTAINER_INDEX) == 1;
    }
    
    public int getConnectedContainersCount() {
        return this.data.get(CONNECTED_CONTAINERS_COUNT_INDEX);
    }
    
    public int getTotalAvailableSlots() {
        return this.data.get(TOTAL_SLOTS_INDEX);
    }
    
    public int getItemsPerTransfer() {
        return this.data.get(ITEMS_PER_TRANSFER_INDEX);
    }
    
    // Método para alternar el estado de habilitado/deshabilitado
    public void toggleEnabled() {
        if (this.blockEntity != null) {
            this.blockEntity.toggleEnabled();
        }
    }
}