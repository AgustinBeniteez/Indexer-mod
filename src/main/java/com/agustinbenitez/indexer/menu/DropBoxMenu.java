package com.agustinbenitez.indexer.menu;

import com.agustinbenitez.indexer.block.entity.DropBoxBlockEntity;
import com.agustinbenitez.indexer.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DropBoxMenu extends AbstractContainerMenu {
    private final DropBoxBlockEntity blockEntity;
    private static final int CONTAINER_SIZE = 54; // 6 filas de 9 slots

    // Constructor para el servidor
    public DropBoxMenu(int containerId, Inventory playerInventory, DropBoxBlockEntity entity) {
        super(ModMenuTypes.DROP_BOX_MENU.get(), containerId);
        this.blockEntity = entity;

        // Añadir slots del DropBox (6 filas de 9 slots)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(entity, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Añadir slots del inventario del jugador (3 filas de 9 slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Añadir slots de la hotbar del jugador
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    // Constructor para el cliente
    public DropBoxMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }

    private static DropBoxBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity entity = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (entity instanceof DropBoxBlockEntity) {
            return (DropBoxBlockEntity) entity;
        }
        throw new IllegalStateException("Block entity is not correct!");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            
            if (index < CONTAINER_SIZE) {
                // Mover del DropBox al inventario del jugador
                if (!this.moveItemStackTo(originalStack, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Mover del inventario del jugador al DropBox
                if (!this.moveItemStackTo(originalStack, 0, CONTAINER_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity != null && this.blockEntity.stillValid(player);
    }
}