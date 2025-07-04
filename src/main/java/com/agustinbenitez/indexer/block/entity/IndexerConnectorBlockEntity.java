package com.agustinbenitez.indexer.block.entity;

import com.agustinbenitez.indexer.block.IndexerConnectorBlock;
import com.agustinbenitez.indexer.init.ModBlockEntities;
import com.agustinbenitez.indexer.inventory.IndexerConnectorMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.List;

import javax.annotation.Nullable;

public class IndexerConnectorBlockEntity extends RandomizableContainerBlockEntity {
    private ItemStack filterItem = ItemStack.EMPTY;
    private BlockPos connectedChestPos = null;
    private net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(1, ItemStack.EMPTY);

    public IndexerConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDEXER_CONNECTOR.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.indexer.connector");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new IndexerConnectorMenu(id, inventory, this, this);
    }

    @Override
    public int getContainerSize() {
        return 1; // Solo un slot para el filtro
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("FilterItem")) {
            this.filterItem = ItemStack.of(tag.getCompound("FilterItem"));
        }
        if (tag.contains("ChestX") && tag.contains("ChestY") && tag.contains("ChestZ")) {
            this.connectedChestPos = new BlockPos(
                    tag.getInt("ChestX"),
                    tag.getInt("ChestY"),
                    tag.getInt("ChestZ")
            );
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!this.filterItem.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            this.filterItem.save(itemTag);
            tag.put("FilterItem", itemTag);
        }
        if (this.connectedChestPos != null) {
            tag.putInt("ChestX", this.connectedChestPos.getX());
            tag.putInt("ChestY", this.connectedChestPos.getY());
            tag.putInt("ChestZ", this.connectedChestPos.getZ());
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IndexerConnectorBlockEntity entity) {
        if (level.isClientSide()) return;

        // Verificar si hay un cofre conectado
        BlockPos previousChestPos = entity.connectedChestPos;
        entity.updateConnectedChest();
        
        // Notificar si se estableció una nueva conexión
        if (previousChestPos == null && entity.connectedChestPos != null) {
            // Se conectó un nuevo cofre
            List<net.minecraft.world.entity.player.Player> nearbyPlayers = level.getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class, 
                new net.minecraft.world.phys.AABB(pos).inflate(16.0D)
            );
            
            for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Conector conectado a cofre"));
            }
        }
    }

    public void updateConnectedChest() {
        if (this.level == null) return;

        BlockPos oldChestPos = this.connectedChestPos;
        this.connectedChestPos = null;
        
        // Buscar cofres adyacentes
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockState adjacentState = this.level.getBlockState(adjacentPos);
            if (adjacentState.getBlock() instanceof ChestBlock) {
                this.connectedChestPos = adjacentPos;
                this.setChanged();
                
                // Imprimir información de depuración
                com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " connected to chest at " + adjacentPos);
                return;
            }
        }
        
        // Si se perdió la conexión, marcar como cambiado
        if (oldChestPos != null && this.connectedChestPos == null) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " lost connection to chest at " + oldChestPos);
            this.setChanged();
        } else if (this.connectedChestPos == null) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " could not find a chest to connect to");
        }
    }

    public boolean canAcceptItem(ItemStack stack) {
        // Verificar si hay un cofre conectado
        if (this.connectedChestPos == null) {
            updateConnectedChest(); // Intentar encontrar un cofre
            if (this.connectedChestPos == null) {
                com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " cannot accept items: no chest connected");
                return false; // No hay cofre conectado
            }
        }
        
        // Verificar que el cofre exista y sea accesible
        if (this.level == null || !(this.level.getBlockEntity(this.connectedChestPos) instanceof Container)) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " cannot accept items: chest not accessible");
            this.connectedChestPos = null; // Resetear la conexión si el cofre ya no existe
            return false;
        }

        // Si no hay filtro configurado, acepta cualquier ítem
        if (this.filterItem.isEmpty()) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " can accept any item (no filter)");
            return true;
        }

        // Verificar si el ítem coincide con el filtro
        boolean matches = this.filterItem.getItem() == stack.getItem();
        com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " filter check: " + 
                                                       (matches ? "accepted" : "rejected") + " item " + 
                                                       stack.getItem().getDescriptionId());
        return matches;
    }

    public ItemStack insertItem(ItemStack stack) {
        if (!canAcceptItem(stack) || this.level == null) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " cannot insert item: item not accepted or level is null");
            return stack;
        }

        BlockEntity chestEntity = this.level.getBlockEntity(this.connectedChestPos);
        if (!(chestEntity instanceof Container)) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " cannot insert item: chest is not a container");
            return stack;
        }

        Container container = (Container) chestEntity;
        ItemStack remainder = stack.copy();
        int initialCount = remainder.getCount();
        
        com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " attempting to insert " + 
                                                       initialCount + " x " + stack.getItem().getDescriptionId() + 
                                                       " into chest at " + this.connectedChestPos);

        // Intentar insertar en el cofre
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotStack = container.getItem(i);
            
            if (slotStack.isEmpty()) {
                // Slot vacío, insertar todo lo que podamos
                int maxStackSize = Math.min(container.getMaxStackSize(), remainder.getMaxStackSize());
                int toInsert = Math.min(remainder.getCount(), maxStackSize);
                
                ItemStack newStack = remainder.copy();
                newStack.setCount(toInsert);
                container.setItem(i, newStack);
                
                remainder.shrink(toInsert);
                com.agustinbenitez.indexer.IndexerMod.LOGGER.info("  Inserted " + toInsert + " items into empty slot " + i);
                
                if (remainder.isEmpty()) {
                    break;
                }
            } else if (ItemStack.isSameItemSameTags(slotStack, remainder)) {
                // Mismo ítem, intentar apilar
                int maxStackSize = Math.min(container.getMaxStackSize(), slotStack.getMaxStackSize());
                int space = maxStackSize - slotStack.getCount();
                
                if (space > 0) {
                    int toInsert = Math.min(remainder.getCount(), space);
                    slotStack.grow(toInsert);
                    remainder.shrink(toInsert);
                    
                    com.agustinbenitez.indexer.IndexerMod.LOGGER.info("  Added " + toInsert + " items to existing stack in slot " + i);
                    
                    if (remainder.isEmpty()) {
                        break;
                    }
                }
            }
        }

        if (chestEntity instanceof ChestBlockEntity) {
            ((ChestBlockEntity) chestEntity).setChanged();
        }
        
        int inserted = initialCount - remainder.getCount();
        if (inserted > 0) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " successfully inserted " + 
                                                           inserted + " items, " + remainder.getCount() + " items remaining");
            
            // Notificar a los jugadores cercanos sobre la inserción
            List<net.minecraft.world.entity.player.Player> nearbyPlayers = level.getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class, 
                new net.minecraft.world.phys.AABB(this.worldPosition).inflate(16.0D)
            );
            
            for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Items transferidos al cofre: " + inserted + " x " + stack.getItem().getDescriptionId()
                ));
            }
        } else {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " could not insert any items");
        }

        return remainder;
    }

    public ItemStack getFilterItem() {
        return this.filterItem;
    }

    public void setFilterItem(ItemStack stack) {
        this.filterItem = stack.copy();
        this.filterItem.setCount(1); // Solo guardamos 1 para el filtro
        this.setChanged();
    }

    public void setConnectedChestPos(BlockPos pos) {
        this.connectedChestPos = pos;
        this.setChanged();
    }

    public BlockPos getConnectedChestPos() {
        return this.connectedChestPos;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? this.filterItem : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == 0 && !this.filterItem.isEmpty()) {
            ItemStack result = this.filterItem.copy();
            this.filterItem = ItemStack.EMPTY;
            this.setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == 0) {
            ItemStack result = this.filterItem;
            this.filterItem = ItemStack.EMPTY;
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            this.filterItem = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            if (!this.filterItem.isEmpty()) {
                this.filterItem.setCount(1);
            }
            this.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public void clearContent() {
        this.filterItem = ItemStack.EMPTY;
        this.items.clear();
    }
    
    @Override
    protected net.minecraft.core.NonNullList<ItemStack> getItems() {
        return this.items;
    }
    
    @Override
    protected void setItems(net.minecraft.core.NonNullList<ItemStack> items) {
        this.items = items;
        if (!items.isEmpty()) {
            this.filterItem = items.get(0);
        } else {
            this.filterItem = ItemStack.EMPTY;
        }
    }
}