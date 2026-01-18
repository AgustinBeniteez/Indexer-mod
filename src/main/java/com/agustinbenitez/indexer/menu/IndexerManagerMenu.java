package com.agustinbenitez.indexer.menu;

import com.agustinbenitez.indexer.block.entity.IndexerManagerBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.block.IndexerPipeBlock;
import com.agustinbenitez.indexer.block.IndexerControllerBlock;
import com.agustinbenitez.indexer.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Container;
import java.util.*;
import net.minecraft.world.inventory.SimpleContainerData;

public class IndexerManagerMenu extends AbstractContainerMenu {
    private final IndexerManagerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private static final int INVENTORY_START_X = 8;
    private static final int INVENTORY_START_Y = 104;
    private static final int HOTBAR_START_Y = 162;
    private static final int SLOT_SIZE = 18;
    private static final int MANAGER_GRID_START_X = 8;
    private static final int MANAGER_GRID_START_Y = 86;

    public IndexerManagerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(ModMenuTypes.INDEXER_MANAGER_MENU, id);
        BlockEntity be = inv.player.level().getBlockEntity(extraData.readBlockPos());
        this.blockEntity = (IndexerManagerBlockEntity) be;
        this.level = inv.player.level();
        this.data = new SimpleContainerData(6);
        this.addDataSlots(this.data);
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

    public IndexerManagerMenu(int id, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.INDEXER_MANAGER_MENU, id);
        this.blockEntity = (IndexerManagerBlockEntity) entity;
        this.level = inv.player.level();
        this.data = createContainerData();
        this.addDataSlots(this.data);
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
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            int managerSlots = 9;
            int invStart = managerSlots;
            int invEnd = invStart + 27;
            int hotbarStart = invEnd;
            int hotbarEnd = hotbarStart + 9;
            if (index < managerSlots) {
                if (!this.moveItemStackTo(stackInSlot, invStart, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stackInSlot, 0, managerSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            slot.onTake(player, stackInSlot);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }

    public IndexerManagerBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    private ContainerData createContainerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                IndexerControllerBlockEntity controller = findNearestController();
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
                return 0;
            }
            @Override
            public void set(int index, int value) {}
            @Override
            public int getCount() {
                return 6;
            }
        };
    }
    
    private IndexerControllerBlockEntity findNearestController() {
        if (this.level == null) return null;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        BlockPos origin = this.blockEntity.getBlockPos();
        for (var direction : net.minecraft.core.Direction.values()) {
            BlockPos adjacentPos = origin.relative(direction);
            BlockState adjacentState = this.level.getBlockState(adjacentPos);
            if (adjacentState.getBlock() instanceof IndexerPipeBlock) {
                if (adjacentState.getValue(IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()))) {
                    queue.add(adjacentPos);
                    visited.add(adjacentPos);
                }
            } else if (adjacentState.getBlock() instanceof IndexerControllerBlock) {
                BlockEntity entity = this.level.getBlockEntity(adjacentPos);
                if (entity instanceof IndexerControllerBlockEntity) {
                    return (IndexerControllerBlockEntity) entity;
                }
                visited.add(adjacentPos);
            }
        }
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            BlockState currentState = this.level.getBlockState(currentPos);
            for (var direction : net.minecraft.core.Direction.values()) {
                BlockPos nextPos = currentPos.relative(direction);
                if (visited.contains(nextPos)) continue;
                BlockState nextState = this.level.getBlockState(nextPos);
                if (nextState.getBlock() instanceof IndexerControllerBlock) {
                    BlockEntity entity = this.level.getBlockEntity(nextPos);
                    if (entity instanceof IndexerControllerBlockEntity) {
                        return (IndexerControllerBlockEntity) entity;
                    }
                    visited.add(nextPos);
                } else if (nextState.getBlock() instanceof IndexerPipeBlock) {
                    boolean currentPipeConnected = currentState.getBlock() instanceof IndexerPipeBlock &&
                            currentState.getValue(IndexerPipeBlock.getPropertyForDirection(direction));
                    boolean nextPipeConnected = nextState.getValue(IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()));
                    if (currentPipeConnected && nextPipeConnected) {
                        queue.add(nextPos);
                        visited.add(nextPos);
                    }
                }
            }
        }
        return null;
    }
    
    public int getOccupiedSlots() {
        return this.data.get(0);
    }
    public int getTotalCapacity() {
        return this.data.get(1);
    }
    public int getConnectedContainersCount() {
        return this.data.get(2);
    }
    public boolean isControllerEnabled() {
        return this.data.get(3) == 1;
    }
    public int getUpgradeLevel() {
        return this.data.get(4);
    }
    public boolean isControllerConnected() {
        return this.data.get(5) == 1;
    }
}
