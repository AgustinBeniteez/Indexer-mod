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
import net.minecraft.server.level.ServerLevel;
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
    private final Map<String, Integer> pendingExtractions = new HashMap<>();
    private final Map<String, ItemStack> pendingVariantByKey = new HashMap<>();
    private static final int EXTRACTION_COOLDOWN_MAX = 8;
    private int extractionCooldown = 0;
    private int syncTicker = 0;

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
        this.extractionCooldown = tag.getInt("ExtractionCooldown");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, this.items);
        tag.putInt("ExtractionCooldown", this.extractionCooldown);
    }

    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    public java.util.List<com.agustinbenitez.indexer.network.ManagerItemsUpdatePacket.Entry> getAggregatedItemVariants() {
        Map<String, com.agustinbenitez.indexer.network.ManagerItemsUpdatePacket.Entry> variants = new HashMap<>();
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        if (connectors.isEmpty()) {
            return new ArrayList<>();
        }
        for (IndexerConnectorBlockEntity connector : connectors) {
            BlockPos containerPos = connector.getConnectedContainerPos();
            if (containerPos == null || level == null) continue;
            BlockEntity be = level.getBlockEntity(containerPos);
            if (!(be instanceof Container container)) continue;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) continue;
                String key = buildVariantKey(stack);
                com.agustinbenitez.indexer.network.ManagerItemsUpdatePacket.Entry entry = variants.get(key);
                if (entry == null) {
                    ItemStack icon = stack.copy();
                    icon.setCount(1);
                    boolean pend = pendingExtractions.getOrDefault(key, 0) > 0;
                    entry = new com.agustinbenitez.indexer.network.ManagerItemsUpdatePacket.Entry(icon, 0, pend);
                    variants.put(key, entry);
                }
                int newCount = entry.count + stack.getCount();
                boolean pend = pendingExtractions.getOrDefault(key, 0) > 0;
                variants.put(key, new com.agustinbenitez.indexer.network.ManagerItemsUpdatePacket.Entry(entry.stackVariant, newCount, pend));
            }
        }
        return new ArrayList<>(variants.values());
    }
    
    private String buildVariantKey(ItemStack stack) {
        ResourceLocation base = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (base == null) return "unknown";
        StringBuilder sb = new StringBuilder(base.toString());
        if (stack.hasTag() && stack.getTag() != null) {
            var tag = stack.getTag();
            java.util.List<String> parts = new java.util.ArrayList<>();
            if (tag.contains("Enchantments")) {
                var list = tag.getList("Enchantments", 10);
                for (int i = 0; i < list.size(); i++) {
                    var ench = list.getCompound(i);
                    String id = ench.getString("id");
                    int lvl = ench.getInt("lvl");
                    parts.add(id + ":" + lvl);
                }
            }
            if (tag.contains("StoredEnchantments")) {
                var list = tag.getList("StoredEnchantments", 10);
                for (int i = 0; i < list.size(); i++) {
                    var ench = list.getCompound(i);
                    String id = ench.getString("id");
                    int lvl = ench.getInt("lvl");
                    parts.add(id + ":" + lvl);
                }
            }
            java.util.Collections.sort(parts);
            if (!parts.isEmpty()) {
                sb.append("|E:");
                for (String p : parts) {
                    sb.append(p).append(",");
                }
            }
            if (tag.contains("BlockEntityTag")) {
                sb.append("|BET:").append(tag.getCompound("BlockEntityTag").toString());
            }
        }
        return sb.toString();
    }
    
    private ResourceLocation parseBaseIdFromKey(String key) {
        int idx = key.indexOf("|E:");
        String base = idx >= 0 ? key.substring(0, idx) : key;
        try {
            return new ResourceLocation(base);
        } catch (Exception e) {
            return net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(net.minecraft.world.item.Items.AIR);
        }
    }

    public void sendItemsTo(ServerPlayer player) {
        java.util.List<com.agustinbenitez.indexer.network.ManagerItemsUpdatePacket.Entry> data = getAggregatedItemVariants();
        ModNetworking.sendToPlayer(new com.agustinbenitez.indexer.network.ManagerItemsUpdatePacket(data), player);
    }
    
    private void sendItemsToOpenPlayers() {
        if (!(this.level instanceof ServerLevel serverLevel)) return;
        java.util.List<com.agustinbenitez.indexer.network.ManagerItemsUpdatePacket.Entry> data = getAggregatedItemVariants();
        for (ServerPlayer sp : serverLevel.players()) {
            AbstractContainerMenu menu = sp.containerMenu;
            if (menu instanceof com.agustinbenitez.indexer.menu.IndexerManagerMenu managerMenu) {
                if (managerMenu.getBlockEntity() == this) {
                    ModNetworking.sendToPlayer(new com.agustinbenitez.indexer.network.ManagerItemsUpdatePacket(data), sp);
                }
            }
        }
    }
    
    public void syncOpenPlayers() {
        sendItemsToOpenPlayers();
    }

    public void queueExtraction(ResourceLocation itemId, int amount, ItemStack variantStack) {
        if (amount <= 0) return;
        ItemStack icon = variantStack.isEmpty() ? new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId)) : variantStack.copy();
        if (!icon.isEmpty()) icon.setCount(1);
        String key = buildVariantKey(icon);
        int current = pendingExtractions.getOrDefault(key, 0);
        pendingExtractions.put(key, current + amount);
        pendingVariantByKey.put(key, icon);
        this.setChanged();
    }
    
    public void cancelPendingExtraction(ItemStack variantStack) {
        if (variantStack.isEmpty()) return;
        ItemStack icon = variantStack.copy();
        icon.setCount(1);
        String key = buildVariantKey(icon);
        if (pendingExtractions.containsKey(key)) {
            pendingExtractions.remove(key);
            pendingVariantByKey.remove(key);
            this.setChanged();
            sendItemsToOpenPlayers();
        }
    }
    
    public int extractImmediately(ResourceLocation itemId, int amount, ItemStack variantStack) {
        int perTransfer = Math.max(1, getItemsPerTransferFromNearestController());
        int limit = Math.max(0, Math.min(amount, perTransfer));
        int moved = moveFromNetworkIntoInventory(itemId, limit, variantStack);
        if (moved > 0) {
            this.setChanged();
        }
        return moved;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        
        // Sincronizar con los clientes abiertos cada 10 ticks (0.5 segundos)
        // Esto permite ver cambios en tiempo real (ej: items entrando desde hornos/extractores)
        this.syncTicker++;
        if (this.syncTicker >= 10) {
            this.syncTicker = 0;
            // Solo sincronizar si hay jugadores viendo este inventario
            if (this.level instanceof ServerLevel serverLevel) {
                boolean hasOpenPlayers = false;
                for (ServerPlayer sp : serverLevel.players()) {
                    if (sp.containerMenu instanceof com.agustinbenitez.indexer.menu.IndexerManagerMenu managerMenu && 
                        managerMenu.getBlockEntity() == this) {
                        hasOpenPlayers = true;
                        break;
                    }
                }
                if (hasOpenPlayers) {
                    sendItemsToOpenPlayers();
                }
            }
        }

        if (pendingExtractions.isEmpty()) return;
        if (this.extractionCooldown > 0) {
            this.extractionCooldown--;
            return;
        }

        String nextKey = pendingExtractions.keySet().iterator().next();
        int remainingRequest = pendingExtractions.getOrDefault(nextKey, 0);
        if (remainingRequest <= 0) {
            pendingExtractions.remove(nextKey);
            pendingVariantByKey.remove(nextKey);
            return;
        }

        int perTick = getItemsPerTransferFromNearestController();
        int toTransferThisTick = Math.min(perTick, remainingRequest);
        ItemStack var = pendingVariantByKey.getOrDefault(nextKey, ItemStack.EMPTY);
        ResourceLocation nextId = var.isEmpty() ? parseBaseIdFromKey(nextKey) : net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(var.getItem());
        int moved = moveFromNetworkIntoInventory(nextId, toTransferThisTick, var);
        if (moved > 0) {
            pendingExtractions.put(nextKey, remainingRequest - moved);
            if (pendingExtractions.get(nextKey) <= 0) {
                pendingExtractions.remove(nextKey);
                pendingVariantByKey.remove(nextKey);
            }
            this.setChanged();
            this.extractionCooldown = EXTRACTION_COOLDOWN_MAX;
            sendItemsToOpenPlayers();
        } else {
            pendingExtractions.remove(nextKey);
            pendingVariantByKey.remove(nextKey);
        }
    }
    
    public boolean isInventoryFull() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack s = this.getItem(i);
            if (s.isEmpty()) return false;
            if (s.getCount() < s.getMaxStackSize()) return false;
        }
        return true;
    }

    private int moveFromNetworkIntoInventory(ResourceLocation itemId, int maxAmount, ItemStack variantStack) {
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
                if (!variantMatches(slot, variantStack)) continue;
                int take = Math.min(remaining, slot.getCount());
                if (take <= 0) continue;
                ItemStack toMove = slot.copy();
                toMove.setCount(take);
                int inserted = insertIntoSelf(toMove);
                if (inserted > 0) {
                    slot.shrink(inserted);
                    container.setItem(i, slot.isEmpty() ? ItemStack.EMPTY : slot);
                    remaining -= inserted;
                }
                if (be instanceof BlockEntity) {
                    ((BlockEntity) be).setChanged();
                }
                if (remaining <= 0) break;
            }
        }
        return maxAmount - remaining;
    }
    
    private boolean variantMatches(ItemStack a, ItemStack variant) {
        if (variant.isEmpty()) return true;
        var ta = a.hasTag() ? a.getTag() : null;
        var tv = variant.hasTag() ? variant.getTag() : null;
        
        // Check BlockEntityTag (Shulker Box content)
        boolean aHasBet = ta != null && ta.contains("BlockEntityTag");
        boolean vHasBet = tv != null && tv.contains("BlockEntityTag");
        if (aHasBet != vHasBet) return false;
        if (aHasBet) {
            if (!ta.getCompound("BlockEntityTag").equals(tv.getCompound("BlockEntityTag"))) return false;
        }

        boolean aHas = ta != null && (ta.contains("Enchantments") || ta.contains("StoredEnchantments"));
        boolean vHas = tv != null && (tv.contains("Enchantments") || tv.contains("StoredEnchantments"));
        if (!aHas && !vHas) return true;
        if (aHas != vHas) return false;
        java.util.List<String> pa = new java.util.ArrayList<>();
        java.util.List<String> pv = new java.util.ArrayList<>();
        if (ta != null && ta.contains("Enchantments")) {
            var la = ta.getList("Enchantments", 10);
            for (int i = 0; i < la.size(); i++) {
                var ench = la.getCompound(i);
                pa.add(ench.getString("id") + ":" + ench.getInt("lvl"));
            }
        }
        if (ta != null && ta.contains("StoredEnchantments")) {
            var la2 = ta.getList("StoredEnchantments", 10);
            for (int i = 0; i < la2.size(); i++) {
                var ench = la2.getCompound(i);
                pa.add(ench.getString("id") + ":" + ench.getInt("lvl"));
            }
        }
        if (tv != null && tv.contains("Enchantments")) {
            var lv = tv.getList("Enchantments", 10);
            for (int i = 0; i < lv.size(); i++) {
                var ench = lv.getCompound(i);
                pv.add(ench.getString("id") + ":" + ench.getInt("lvl"));
            }
        }
        if (tv != null && tv.contains("StoredEnchantments")) {
            var lv2 = tv.getList("StoredEnchantments", 10);
            for (int i = 0; i < lv2.size(); i++) {
                var ench = lv2.getCompound(i);
                pv.add(ench.getString("id") + ":" + ench.getInt("lvl"));
            }
        }
        java.util.Collections.sort(pa);
        java.util.Collections.sort(pv);
        return pa.equals(pv);
    }

    private int insertIntoSelf(ItemStack stack) {
        int originalCount = stack.getCount();
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack existing = this.getItem(i);
            if (existing.isEmpty()) {
                this.setItem(i, stack.copy());
                return originalCount;
            } else if (ItemStack.isSameItem(existing, stack) &&
                    existing.getCount() < existing.getMaxStackSize()) {
                int canAdd = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(canAdd);
                stack.shrink(canAdd);
                this.setItem(i, existing);
                if (stack.isEmpty()) return originalCount;
            }
        }
        if (!stack.isEmpty()) {
            for (int i = 0; i < this.getContainerSize(); i++) {
                if (this.getItem(i).isEmpty()) {
                    this.setItem(i, stack.copy());
                    return originalCount;
                }
            }
        }
        return originalCount - stack.getCount();
    }

    private int getItemsPerTransferFromNearestController() {
        IndexerControllerBlockEntity controller = findNearestController();
        return controller != null ? Math.max(1, controller.getItemsPerTransfer()) : 1;
    }

    @Nullable
    private IndexerControllerBlockEntity findNearestController() {
        if (this.level == null) return null;
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
                    return controller;
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
                        return controller;
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
        return null;
    }

    private List<IndexerConnectorBlockEntity> findConnectors() {
        if (!networkChanged && connectorCache != null) {
            return connectorCache;
        }

        // Intenta usar el controlador conectado para obtener la lista de conectores
        // Esto asegura que el Manager vea exactamente lo mismo que el Controller
        IndexerControllerBlockEntity controller = findNearestController();
        if (controller != null) {
            List<IndexerConnectorBlockEntity> controllerConnectors = controller.findConnectors();
            this.connectorCache = controllerConnectors;
            this.networkChanged = false;
            return controllerConnectors;
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
                
                boolean isCurrentPipe = currentState.getBlock() instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock;
                boolean isCurrentController = currentState.getBlock() instanceof com.agustinbenitez.indexer.block.IndexerControllerBlock;
                
                if (nextBlock instanceof com.agustinbenitez.indexer.block.IndexerPipeBlock) {
                    boolean validSource = isCurrentController;
                    if (isCurrentPipe) {
                        validSource = currentState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction));
                    }
                    
                    boolean nextPipeConnected = nextState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()));
                    if (validSource && nextPipeConnected) {
                        queue.add(nextPos);
                        visited.add(nextPos);
                    }
                } else if (nextBlock instanceof com.agustinbenitez.indexer.block.IndexerConnectorBlock) {
                    boolean validSource = isCurrentController;
                    if (isCurrentPipe) {
                        validSource = currentState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction));
                    }
                    
                    if (validSource) {
                        BlockEntity nextEntity = this.level.getBlockEntity(nextPos);
                        if (nextEntity instanceof IndexerConnectorBlockEntity) {
                            connectors.add((IndexerConnectorBlockEntity) nextEntity);
                            visited.add(nextPos);
                        }
                    }
                } else if (nextBlock instanceof com.agustinbenitez.indexer.block.IndexerControllerBlock) {
                    boolean validSource = isCurrentController;
                    if (isCurrentPipe) {
                        validSource = currentState.getValue(com.agustinbenitez.indexer.block.IndexerPipeBlock.getPropertyForDirection(direction));
                    }
                    
                    if (validSource) {
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
    
    public void markNetworkChanged() {
        this.networkChanged = true;
        this.connectorCache = null;
    }
}
