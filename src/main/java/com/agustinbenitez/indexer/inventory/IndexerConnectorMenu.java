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

    // Base filter slots (3x3 grid)
    private static final int BASE_FILTER_SLOTS = 9;
    private static final int FILTER_START_X = 44;
    private static final int FILTER_START_Y = 18;

    // Constants for player inventory position
    private static final int INVENTORY_START_X = 8;
    private static final int INVENTORY_START_Y = 84;
    private static final int HOTBAR_START_Y = 142;
    private static final int SLOT_SIZE = 18;

    public IndexerConnectorMenu(int id, Inventory playerInventory, Container container,
            IndexerConnectorBlockEntity blockEntity) {
        super(ModMenuTypes.INDEXER_CONNECTOR_MENU.get(), id);
        this.container = container;
        this.blockEntity = blockEntity;
        this.access = blockEntity != null
                ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                : ContainerLevelAccess.NULL;

        // Usar coordenadas originales para que coincidan con la textura, incluso en Lvl
        // 2
        int filterX = FILTER_START_X;
        int invX = INVENTORY_START_X;

        // Primeros 9 slots (3x3 izquierda)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = col + row * 3;
                this.addSlot(new Slot(container, slotIndex,
                        filterX + col * SLOT_SIZE,
                        FILTER_START_Y + row * SLOT_SIZE) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return true;
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }

        // Slots adicionales (3x3 derecha)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = 9 + col + row * 3; // Indices 9-17
                this.addSlot(new Slot(container, slotIndex,
                        filterX + (col + 3) * SLOT_SIZE,
                        FILTER_START_Y + row * SLOT_SIZE) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return blockEntity != null && blockEntity.getConnectorLevel() >= 2;
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }

                    @Override
                    public boolean isActive() {
                        return blockEntity == null || blockEntity.getConnectorLevel() >= 2;
                    }
                });
            }
        }

        // Añadir slots del inventario del jugador
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        invX + col * SLOT_SIZE,
                        INVENTORY_START_Y + row * SLOT_SIZE));
            }
        }

        // Añadir slots de la barra de acceso rápido (hotbar)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    invX + col * SLOT_SIZE,
                    HOTBAR_START_Y));
        }
    }

    public IndexerConnectorMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(BASE_FILTER_SLOTS), null);
    }

    public int getFilterSlots() {
        return (this.blockEntity != null) ? this.blockEntity.getCurrentFilterSlots() : BASE_FILTER_SLOTS;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            // Obtener el número real de slots de filtro (9 o 18)
            int filterSlots = (this.blockEntity != null) ? this.blockEntity.getCurrentFilterSlots() : BASE_FILTER_SLOTS;

            if (index < filterSlots) {
                // Si es un slot de filtro, mover al inventario del jugador
                // Los slots del inventario del jugador empiezan después de todos los slots de
                // filtro
                if (!this.moveItemStackTo(slotStack, filterSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Si es un slot del inventario del jugador, intentar mover a los slots de
                // filtro
                if (!this.moveItemStackTo(slotStack, 0, filterSlots, false)) {
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
    }
}
