package com.agustinbenitez.indexer.block.entity;

import com.agustinbenitez.indexer.init.ModBlockEntities;
import com.agustinbenitez.indexer.menu.IndexerManagerMenu;
import com.agustinbenitez.indexer.network.ModNetworking;
import com.agustinbenitez.indexer.network.ManagerItemsUpdatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

public class IndexerManagerBlockEntity extends RandomizableContainerBlockEntity {
    private boolean networkChanged = true;
    private List<IndexerConnectorBlockEntity> connectorCache = null;
    private static final int CONTAINER_SIZE = 9;
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final Map<ResourceLocation, Integer> pendingExtractions = new HashMap<>();

    public IndexerManagerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDEXER_MANAGER.get(), pos, state);
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
    public Component getDisplayName() {
        return Component.translatable("container.indexer.manager");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new IndexerManagerMenu(id, inventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.indexer.manager");
    }
    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, this.items);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, this.items);
    }

    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    public Map<String, Integer> getAggregatedItems() {
        Map<String, Integer> totals = new HashMap<>();
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        if (connectors.isEmpty()) {
            return totals;
        }
        for (IndexerConnectorBlockEntity connector : connectors) {
            BlockPos containerPos = connector.getConnectedContainerPos();
            if (containerPos == null || level == null) continue;
            BlockEntity be = level.getBlockEntity(containerPos);
            if (!(be instanceof Container container)) continue;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) continue;
                ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (key == null) continue;
                String itemKey = key.toString();
                totals.put(itemKey, totals.getOrDefault(itemKey, 0) + stack.getCount());
            }
        }
        return totals;
    }

    public void sendItemsTo(ServerPlayer player) {
        Map<String, Integer> data = getAggregatedItems();
        ModNetworking.sendToPlayer(new ManagerItemsUpdatePacket(data), player);
    }

    public void queueExtraction(ResourceLocation itemId, int amount) {
        if (amount <= 0) return;
        int current = pendingExtractions.getOrDefault(itemId, 0);
        pendingExtractions.put(itemId, current + amount);
        this.setChanged();
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        if (pendingExtractions.isEmpty()) return;

        ResourceLocation nextId = pendingExtractions.keySet().iterator().next();
        int remainingRequest = pendingExtractions.getOrDefault(nextId, 0);
        if (remainingRequest <= 0) {
            pendingExtractions.remove(nextId);
            return;
        }

        int perTick = getItemsPerTransferFromNearestController();
        int toTransferThisTick = Math.min(perTick, remainingRequest);
        int moved = moveFromNetworkIntoInventory(nextId, toTransferThisTick);
        if (moved > 0) {
            pendingExtractions.put(nextId, remainingRequest - moved);
            if (pendingExtractions.get(nextId) <= 0) {
                pendingExtractions.remove(nextId);
            }
            this.setChanged();
        } else {
            pendingExtractions.remove(nextId);
        }
    }

    private int moveFromNetworkIntoInventory(ResourceLocation itemId, int maxAmount) {
        if (this.level == null || maxAmount <= 0) return 0;
        int remaining = maxAmount;
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        for (IndexerConnectorBlockEntity connector : connectors) {
            if (remaining <= 0) break;
            BlockPos containerPos = connector.getConnectedContainerPos();
            if (containerPos == null) continue;
            BlockEntity be = level.getBlockEntity(containerPos);
            if (!(be instanceof Container container)) continue;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slot = container.getItem(i);
                if (slot.isEmpty()) continue;
                ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(slot.getItem());
                if (key == null || !key.equals(itemId)) continue;
                int take = Math.min(remaining, slot.getCount());
                if (take <= 0) continue;
                ItemStack toMove = slot.copy();
                toMove.setCount(take);
                slot.shrink(take);
                container.setItem(i, slot.isEmpty() ? ItemStack.EMPTY : slot);
                remaining -= take;
                insertIntoSelf(toMove);
                if (be instanceof BlockEntity) {
                    ((BlockEntity) be).setChanged();
                }
                if (remaining <= 0) break;
            }
        }
        return maxAmount - remaining;
    }

    private void insertIntoSelf(ItemStack stack) {
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack existing = this.getItem(i);
            if (existing.isEmpty()) {
                this.setItem(i, stack.copy());
                return;
            } else if (ItemStack.isSameItem(existing, stack) &&
                    existing.getCount() < existing.getMaxStackSize()) {
                int canAdd = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(canAdd);
                stack.shrink(canAdd);
                this.setItem(i, existing);
                if (stack.isEmpty()) return;
            }
        }
        if (!stack.isEmpty()) {
            for (int i = 0; i < this.getContainerSize(); i++) {
                if (this.getItem(i).isEmpty()) {
                    this.setItem(i, stack.copy());
                    return;
                }
            }
        }
    }

    private int getItemsPerTransferFromNearestController() {
        if (this.level == null) return 1;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockState adjacentState = this.level.getBlockState(adjacentPos);
            Block adjacentBlock = adjacentState.getBlock();
            if (adjacentBlock instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock) {
                if (adjacentState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()))) {
                    queue.add(adjacentPos);
                    visited.add(adjacentPos);
                }
            } else if (adjacentBlock instanceof com.agustinbenitez.indexer.block.IndexerControllerBlock) {
                BlockEntity entity = this.level.getBlockEntity(adjacentPos);
                if (entity instanceof IndexerControllerBlockEntity controller) {
                    return Math.max(1, controller.getItemsPerTransfer());
                }
                visited.add(adjacentPos);
            }
        }
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            BlockState currentState = this.level.getBlockState(currentPos);
            for (Direction direction : Direction.values()) {
                BlockPos nextPos = currentPos.relative(direction);
                if (visited.contains(nextPos)) continue;
                BlockState nextState = this.level.getBlockState(nextPos);
                Block nextBlock = nextState.getBlock();
                if (nextBlock instanceof com.agustinbenitez.indexer.block.IndexerControllerBlock) {
                    BlockEntity entity = this.level.getBlockEntity(nextPos);
                    if (entity instanceof IndexerControllerBlockEntity controller) {
                        return Math.max(1, controller.getItemsPerTransfer());
                    }
                    visited.add(nextPos);
                } else if (nextBlock instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock) {
                    boolean currentPipeConnected = currentState.getBlock() instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock &&
                            currentState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction));
                    boolean nextPipeConnected = nextState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()));
                    if (currentPipeConnected && nextPipeConnected) {
                        queue.add(nextPos);
                        visited.add(nextPos);
                    }
                }
            }
        }
        return 1;
    }

    private List<IndexerConnectorBlockEntity> findConnectors() {
        if (!networkChanged && connectorCache != null) {
            return connectorCache;
        }
        List<IndexerConnectorBlockEntity> connectors = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockState adjacentState = this.level.getBlockState(adjacentPos);
            if (adjacentState.getBlock() instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock) {
                if (adjacentState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()))) {
                    queue.add(adjacentPos);
                    visited.add(adjacentPos);
                }
            } else if (adjacentState.getBlock() instanceof com.agustinbenitez.indexer.block.IndexerConnectorBlock) {
                BlockEntity blockEntity = this.level.getBlockEntity(adjacentPos);
                if (blockEntity instanceof IndexerConnectorBlockEntity) {
                    connectors.add((IndexerConnectorBlockEntity) blockEntity);
                }
            } else if (adjacentState.getBlock() instanceof com.agustinbenitez.indexer.block.IndexerControllerBlock) {
                visited.add(adjacentPos);
                queue.add(adjacentPos);
            }
        }
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            BlockState currentState = this.level.getBlockState(currentPos);
            BlockEntity blockEntity = this.level.getBlockEntity(currentPos);
            if (blockEntity instanceof IndexerConnectorBlockEntity) {
                connectors.add((IndexerConnectorBlockEntity) blockEntity);
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos nextPos = currentPos.relative(direction);
                if (visited.contains(nextPos)) continue;
                BlockState nextState = this.level.getBlockState(nextPos);
                Block nextBlock = nextState.getBlock();
                if (nextBlock instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock) {
                    boolean currentPipeConnected = currentState.getBlock() instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock &&
                            currentState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction));
                    boolean nextPipeConnected = nextState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()));
                    if (currentPipeConnected && nextPipeConnected) {
                        queue.add(nextPos);
                        visited.add(nextPos);
                    }
                } else if (nextBlock instanceof com.agustinbenitez.indexer.block.IndexerConnectorBlock) {
                    boolean currentPipeConnected = currentState.getBlock() instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock &&
                            currentState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction));
                    if (currentPipeConnected) {
                        BlockEntity nextEntity = this.level.getBlockEntity(nextPos);
                        if (nextEntity instanceof IndexerConnectorBlockEntity) {
                            connectors.add((IndexerConnectorBlockEntity) nextEntity);
                            visited.add(nextPos);
                        }
                    }
                } else if (nextBlock instanceof com.agustinbenitez.indexer.block.IndexerControllerBlock) {
                    boolean currentPipeConnected = currentState.getBlock() instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock &&
                            currentState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction));
                    if (currentPipeConnected || !(currentState.getBlock() instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock)) {
                        queue.add(nextPos);
                        visited.add(nextPos);
                    }
                }
            }
        }
        connectorCache = connectors;
        networkChanged = false;
        return connectors;
    }
}
