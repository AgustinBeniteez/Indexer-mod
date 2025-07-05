package com.agustinbenitez.indexer.inventory;

import com.agustinbenitez.indexer.block.entity.IndexerConnectorBlockEntity;
import com.agustinbenitez.indexer.init.ModMenuTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerLevelAccess;

public class IndexerConnectorMenu extends AbstractContainerMenu {
    private final Container container;
    private final IndexerConnectorBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    // Constants for player inventory position
    private static final int INVENTORY_START_X = 8;
    private static final int INVENTORY_START_Y = 84;
    private static final int HOTBAR_START_Y = 142;
    private static final int SLOT_SIZE = 18;

    public IndexerConnectorMenu(int id, Inventory playerInventory, Container container, IndexerConnectorBlockEntity blockEntity) {
        super(ModMenuTypes.INDEXER_CONNECTOR_MENU.get(), id);
        this.container = container;
        this.blockEntity = blockEntity;
        this.access = blockEntity != null ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()) : ContainerLevelAccess.NULL;

        // Slot for the filter in the center
        this.addSlot(new Slot(container, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Permitir colocar cualquier ítem como filtro
                return true;
            }
            
            @Override
            public int getMaxStackSize() {
                return 1; // Solo permitir un ítem como filtro
            }
        });
        
        // Añadir slots del inventario del jugador (3 filas x 9 columnas)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 
                        INVENTORY_START_X + col * SLOT_SIZE, 
                        INVENTORY_START_Y + row * SLOT_SIZE));
            }
        }
        
        // Añadir slots de la barra de acceso rápido (hotbar)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 
                    INVENTORY_START_X + col * SLOT_SIZE, 
                    HOTBAR_START_Y));  
        }
    }

    public IndexerConnectorMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(1), null);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            
            if (index == 0) {
                // Si es el slot del filtro, mover al inventario del jugador
                if (!this.moveItemStackTo(slotStack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Si es un slot del inventario del jugador, mover al slot del filtro
                if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.blockEntity != null) {
            return stillValid(this.access, player, this.blockEntity.getBlockState().getBlock());
        }
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        
        // Mostrar mensaje en el chat cuando se cierre el menú
        if (!player.level().isClientSide && this.blockEntity != null) {
            ItemStack filterItem = this.blockEntity.getFilterItem();
            if (!filterItem.isEmpty()) {
                Component itemName = filterItem.getDisplayName();
                player.sendSystemMessage(Component.translatable("message.indexer.connector.filter_set", itemName));
            } else {
                player.sendSystemMessage(Component.translatable("message.indexer.connector.filter_cleared"));
            }
        }
    }
}