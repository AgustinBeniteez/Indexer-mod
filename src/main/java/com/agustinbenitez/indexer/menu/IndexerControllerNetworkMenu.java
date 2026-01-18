package com.agustinbenitez.indexer.menu;

import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;

public class IndexerControllerNetworkMenu extends AbstractContainerMenu {
    public final IndexerControllerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    
    public IndexerControllerNetworkMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(8));
    }
    
    public IndexerControllerNetworkMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.INDEXER_CONTROLLER_NETWORK_MENU, id);
        checkContainerSize(inv, 0); // No slots needed for this GUI
        blockEntity = ((IndexerControllerBlockEntity) entity);
        this.level = inv.player.level();
        this.data = data;
        
        addDataSlots(data);
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // No slots to move
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
    
    public IndexerControllerBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    // Métodos de acceso a datos del controlador
    public boolean isEnabled() {
        return data.get(0) == 1;
    }
    
    public int getConnectedContainersCount() {
        return data.get(2); // Corregido: era data.get(1) que es hasDropContainer
    }
    
    public int getTotalCapacity() {
        return data.get(5); // Corregido: era data.get(2) que es connectedContainers
    }
    
    public int getOccupiedSlots() {
        return data.get(6); // Corregido: era data.get(3) que es totalAvailableSlots
    }
    
    public boolean hasDropContainer() {
        return blockEntity.hasDropContainer();
    }
    
    public int getItemsPerTransfer() {
        return data.get(4); // Obtener itemsPerTransfer desde ContainerData para sincronización
    }
    
    public int getCurrentUpgradeLevel() {
        return data.get(7); // Obtener el nivel de mejora desde ContainerData
    }
}