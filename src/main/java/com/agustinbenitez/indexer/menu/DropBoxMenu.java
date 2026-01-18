package com.agustinbenitez.indexer.menu;

import com.agustinbenitez.indexer.block.entity.DropBoxBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.init.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DropBoxMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final DropBoxBlockEntity dropBoxEntity;

    // Client constructor
    public DropBoxMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(54), new SimpleContainerData(6), null);
    }

    // Server constructor
    public DropBoxMenu(int id, Inventory playerInventory, DropBoxBlockEntity dropBoxEntity) {
        this(id, playerInventory, dropBoxEntity, createContainerData(dropBoxEntity), dropBoxEntity);
    }

    // Private constructor
    private DropBoxMenu(int id, Inventory playerInventory, Container container, ContainerData data, DropBoxBlockEntity dropBoxEntity) {
        super(ModMenuTypes.DROP_BOX_MENU, id);
        this.container = container;
        this.data = data;
        this.dropBoxEntity = dropBoxEntity;

        // Add container data for syncing stats
        this.addDataSlots(data);

        // Add DropBox slots (6 rows of 9 slots)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Add player hotbar slots
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    private static ContainerData createContainerData(DropBoxBlockEntity dropBoxEntity) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (dropBoxEntity != null) {
                    IndexerControllerBlockEntity controller = dropBoxEntity.findConnectedController();
                    if (index == 5) return controller != null ? 1 : 0;
                    if (controller != null) {
                        switch (index) {
                            case 0: return controller.getOccupiedSlots();
                            case 1: return controller.getTotalCapacity();
                            case 2: return controller.getConnectedContainersCount();
                            case 3: return controller.isEnabled() ? 1 : 0;
                            case 4: return controller.getCurrentUpgradeLevel();
                        }
                    }
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {
                // Not used for read-only data
            }

            @Override
            public int getCount() {
                return 6;
            }
        };
    }

    // Getter methods for the screen to access stats
    public int getOccupiedSlots() {
        return this.data.get(0);
    }

    public int getTotalCapacity() {
        return this.data.get(1);
    }

    public int getConnectedContainersCount() {
        return this.data.get(2);
    }
    
    public boolean isControllerConnected() {
        return this.data.get(5) == 1;
    }

    public boolean isControllerEnabled() {
        return this.data.get(3) == 1;
    }

    public int getUpgradeLevel() {
        return this.data.get(4);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 54) {
                // Moving from DropBox to player inventory
                if (!this.moveItemStackTo(itemstack1, 54, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to DropBox
                if (!this.moveItemStackTo(itemstack1, 0, 54, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public void transferAllItemsToDropBox(Player player) {
        // Transfer items from player inventory to DropBox
        for (int i = 54; i < this.slots.size(); i++) {
            Slot slot = this.slots.get(i);
            if (slot.hasItem()) {
                ItemStack itemStack = slot.getItem();
                if (!this.moveItemStackTo(itemStack, 0, 54, false)) {
                    break; // Stop if we can't move any more items
                }
                if (itemStack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            }
        }
    }
}