package com.agustinbenitez.indexer.menu;

import com.agustinbenitez.indexer.block.entity.IndexerManagerBlockEntity;
import com.agustinbenitez.indexer.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;

public class IndexerManagerMenu extends AbstractContainerMenu {
    private final IndexerManagerBlockEntity blockEntity;
    private final Level level;
    private static final int INVENTORY_START_X = 8;
    private static final int INVENTORY_START_Y = 104;
    private static final int HOTBAR_START_Y = 162;
    private static final int SLOT_SIZE = 18;
    private static final int MANAGER_GRID_START_X = 8;
    private static final int MANAGER_GRID_START_Y = 86;

    public IndexerManagerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public IndexerManagerMenu(int id, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.INDEXER_MANAGER_MENU.get(), id);
        this.blockEntity = (IndexerManagerBlockEntity) entity;
        this.level = inv.player.level();
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(blockEntity, col,
                    MANAGER_GRID_START_X + col * SLOT_SIZE,
                    MANAGER_GRID_START_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9,
                        INVENTORY_START_X + col * SLOT_SIZE,
                        INVENTORY_START_Y + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col,
                    INVENTORY_START_X + col * SLOT_SIZE,
                    HOTBAR_START_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }

    public IndexerManagerBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
