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
// ChestMenu import removed as we now use IndexerConnectorMenu
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
// ChestBlock import removed as we now use generic Container interface
import net.minecraft.world.level.block.entity.BlockEntity;
// ChestBlockEntity import removed as we now use generic Container interface
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.List;

import javax.annotation.Nullable;

public class IndexerConnectorBlockEntity extends RandomizableContainerBlockEntity {
    private ItemStack filterItem = ItemStack.EMPTY;
    private BlockPos connectedContainerPos = null;
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
        if (tag.contains("ContainerX") && tag.contains("ContainerY") && tag.contains("ContainerZ")) {
            this.connectedContainerPos = new BlockPos(
                    tag.getInt("ContainerX"),
                    tag.getInt("ContainerY"),
                    tag.getInt("ContainerZ")
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
        if (this.connectedContainerPos != null) {
            tag.putInt("ContainerX", this.connectedContainerPos.getX());
            tag.putInt("ContainerY", this.connectedContainerPos.getY());
            tag.putInt("ContainerZ", this.connectedContainerPos.getZ());
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IndexerConnectorBlockEntity entity) {
        if (level.isClientSide()) return;

        // Verificar si hay un contenedor conectado
        BlockPos previousContainerPos = entity.connectedContainerPos;
        entity.updateConnectedContainer();
        
        // Ya no enviamos mensajes de notificación al chat cuando se conecta un contenedor
    }

    public void updateConnectedContainer() {
        if (this.level == null) return;

        BlockPos oldContainerPos = this.connectedContainerPos;
        this.connectedContainerPos = null;
        
        // Buscar cualquier tipo de inventario adyacente
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockEntity adjacentEntity = this.level.getBlockEntity(adjacentPos);
            
            // Verificar si es cualquier tipo de contenedor (barril, horno, etc.)
            // Pero excluir específicamente otros conectores
            if (adjacentEntity instanceof Container && !(adjacentEntity instanceof IndexerConnectorBlockEntity)) {
                this.connectedContainerPos = adjacentPos;
                this.setChanged();
                
                // Obtener el nombre del bloque para los logs
                String blockName = this.level.getBlockState(adjacentPos).getBlock().getDescriptionId();
                
                // Imprimir información de depuración
                com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " connected to container (" + blockName + ") at " + adjacentPos);
                return;
            }
        }
        
        // Si se perdió la conexión, marcar como cambiado
        if (oldContainerPos != null && this.connectedContainerPos == null) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " lost connection to container at " + oldContainerPos);
            this.setChanged();
        } else if (this.connectedContainerPos == null) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " could not find any container to connect to");
        }
    }

    public boolean canAcceptItem(ItemStack stack) {
        // Verificar si hay un contenedor conectado
        if (this.connectedContainerPos == null) {
            updateConnectedContainer(); // Intentar encontrar un contenedor
            if (this.connectedContainerPos == null) {
                com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " cannot accept items: no container connected");
                return false; // No hay contenedor conectado
            }
        }
        
        // Verificar que el contenedor exista y sea accesible
        if (this.level == null) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " cannot accept items: level is null");
            return false;
        }
        
        BlockEntity containerEntity = this.level.getBlockEntity(this.connectedContainerPos);
        if (!(containerEntity instanceof Container) || containerEntity instanceof IndexerConnectorBlockEntity) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " cannot accept items: container not accessible or is another connector");
            this.connectedContainerPos = null; // Resetear la conexión si el contenedor ya no existe o es otro conector
            return false;
        }

        // Verificar si es carbón o carbón vegetal y si el contenedor es un horno
        boolean isCoalOrCharcoal = stack.getItem().getDescriptionId().equals("item.minecraft.coal") || 
                                  stack.getItem().getDescriptionId().equals("item.minecraft.charcoal");
        boolean isFurnace = containerEntity.getClass().getName().contains("FurnaceBlockEntity");
        
        // Si es carbón/carbón vegetal y el contenedor es un horno, permitir siempre
        if (isCoalOrCharcoal && isFurnace) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " accepting coal/charcoal for furnace regardless of filter");
            return true;
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

        BlockEntity containerEntity = this.level.getBlockEntity(this.connectedContainerPos);
        if (!(containerEntity instanceof Container) || containerEntity instanceof IndexerConnectorBlockEntity) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " cannot insert item: block is not a container or is another connector");
            return stack;
        }

        // Obtener el nombre del bloque para los logs
        String containerType = this.level.getBlockState(this.connectedContainerPos).getBlock().getDescriptionId();
        
        Container container = (Container) containerEntity;
        ItemStack remainder = stack.copy();
        int initialCount = remainder.getCount();
        
        com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " attempting to insert " + 
                                                       initialCount + " x " + stack.getItem().getDescriptionId() + 
                                                       " into container (" + containerType + ") at " + this.connectedContainerPos);

        // Verificar si es un horno y el ítem es carbón o carbón vegetal
        boolean isCoalOrCharcoal = stack.getItem().getDescriptionId().equals("item.minecraft.coal") || 
                                  stack.getItem().getDescriptionId().equals("item.minecraft.charcoal");
        
        // Si es un horno (AbstractFurnaceBlockEntity) y el ítem es carbón/carbón vegetal
        if (containerEntity.getClass().getName().contains("FurnaceBlockEntity") && isCoalOrCharcoal) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Detected furnace and coal/charcoal, attempting to insert into fuel slot");
            
            // El slot de combustible en AbstractFurnaceBlockEntity es 1
            final int FURNACE_FUEL_SLOT = 1;
            
            if (FURNACE_FUEL_SLOT < container.getContainerSize()) {
                ItemStack fuelSlotStack = container.getItem(FURNACE_FUEL_SLOT);
                
                if (fuelSlotStack.isEmpty()) {
                    // Slot de combustible vacío, insertar todo lo que podamos
                    int maxStackSize = Math.min(container.getMaxStackSize(), remainder.getMaxStackSize());
                    int toInsert = Math.min(remainder.getCount(), maxStackSize);
                    
                    ItemStack newStack = remainder.copy();
                    newStack.setCount(toInsert);
                    container.setItem(FURNACE_FUEL_SLOT, newStack);
                    
                    remainder.shrink(toInsert);
                    com.agustinbenitez.indexer.IndexerMod.LOGGER.info("  Inserted " + toInsert + " coal/charcoal into furnace fuel slot");
                    
                    if (remainder.isEmpty()) {
                        if (containerEntity instanceof BlockEntity) {
                            ((BlockEntity) containerEntity).setChanged();
                        }
                        return ItemStack.EMPTY;
                    }
                } else if (ItemStack.isSameItemSameTags(fuelSlotStack, remainder)) {
                    // Mismo ítem en el slot de combustible, intentar apilar
                    int maxStackSize = Math.min(container.getMaxStackSize(), fuelSlotStack.getMaxStackSize());
                    int space = maxStackSize - fuelSlotStack.getCount();
                    
                    if (space > 0) {
                        int toInsert = Math.min(remainder.getCount(), space);
                        fuelSlotStack.grow(toInsert);
                        remainder.shrink(toInsert);
                        
                        com.agustinbenitez.indexer.IndexerMod.LOGGER.info("  Added " + toInsert + " coal/charcoal to existing stack in furnace fuel slot");
                        
                        if (remainder.isEmpty()) {
                            if (containerEntity instanceof BlockEntity) {
                                ((BlockEntity) containerEntity).setChanged();
                            }
                            return ItemStack.EMPTY;
                        }
                    }
                }
                
                // Si llegamos aquí, significa que no pudimos insertar todo el carbón en este horno
                // porque el slot de combustible está lleno o casi lleno
                if (!remainder.isEmpty()) {
                    com.agustinbenitez.indexer.IndexerMod.LOGGER.info("  Fuel slot is full or nearly full, cannot insert more coal/charcoal");
                    // No continuamos con el comportamiento normal para este horno
                    // Devolvemos el remainder para que el controlador intente con otro conector
                    if (containerEntity instanceof BlockEntity) {
                        ((BlockEntity) containerEntity).setChanged();
                    }
                    return remainder;
                }
            }
            
            // Si es un horno y carbón, SOLO intentamos insertar en el slot de combustible
            // No continuamos con el comportamiento normal para otros slots
            if (containerEntity instanceof BlockEntity) {
                ((BlockEntity) containerEntity).setChanged();
            }
            return remainder;
        }

        // Comportamiento normal para otros contenedores o si no se pudo insertar todo en el slot de combustible
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

        if (containerEntity instanceof BlockEntity) {
            ((BlockEntity) containerEntity).setChanged();
        }
        
        int inserted = initialCount - remainder.getCount();
        if (inserted > 0) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Connector at " + this.worldPosition + " successfully inserted " + 
                                                           inserted + " items, " + remainder.getCount() + " items remaining");
            // Ya no enviamos mensajes de notificación al chat
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

    public void setConnectedContainerPos(BlockPos pos) {
        this.connectedContainerPos = pos;
        this.setChanged();
    }

    public BlockPos getConnectedContainerPos() {
        return this.connectedContainerPos;
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