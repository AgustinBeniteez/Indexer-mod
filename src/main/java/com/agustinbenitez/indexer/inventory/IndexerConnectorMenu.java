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

public class IndexerConnectorMenu extends AbstractContainerMenu {
    private final Container container;
    private final IndexerConnectorBlockEntity blockEntity;

    public IndexerConnectorMenu(int id, Inventory playerInventory, Container container, IndexerConnectorBlockEntity blockEntity) {
        super(ModMenuTypes.INDEXER_CONNECTOR_MENU.get(), id);
        this.container = container;
        this.blockEntity = blockEntity;

        // Solo un slot para el filtro en el centro
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
    }

    public IndexerConnectorMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(1), null);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No hay transferencia rápida ya que solo tenemos un slot
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
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