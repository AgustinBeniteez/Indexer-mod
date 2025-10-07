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
import java.util.ArrayList;

import javax.annotation.Nullable;

public class IndexerConnectorBlockEntity extends RandomizableContainerBlockEntity {
    private static final int FILTER_SLOTS = 9; // 3x3 grid of filter slots
    private List<ItemStack> filterItems = new ArrayList<>();
    private BlockPos connectedContainerPos = null;
    private net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);

    public IndexerConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDEXER_CONNECTOR.get(), pos, state);
        // Initialize filter items list with empty stacks
        for (int i = 0; i < FILTER_SLOTS; i++) {
            filterItems.add(ItemStack.EMPTY);
        }
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
        return FILTER_SLOTS; // Multiple slots for filters
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        // Load filter items
        this.filterItems.clear();
        if (tag.contains("FilterItems")) {
            CompoundTag filterItemsTag = tag.getCompound("FilterItems");
            for (int i = 0; i < FILTER_SLOTS; i++) {
                if (filterItemsTag.contains("Item" + i)) {
                    this.filterItems.add(ItemStack.of(filterItemsTag.getCompound("Item" + i)));
                } else {
                    this.filterItems.add(ItemStack.EMPTY);
                }
            }
        } else {
            // Initialize with empty stacks if no data
            for (int i = 0; i < FILTER_SLOTS; i++) {
                this.filterItems.add(ItemStack.EMPTY);
            }
        }
        
        // Load connected container position
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
        
        // Save filter items
        CompoundTag filterItemsTag = new CompoundTag();
        for (int i = 0; i < this.filterItems.size() && i < FILTER_SLOTS; i++) {
            ItemStack filterItem = this.filterItems.get(i);
            if (!filterItem.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                filterItem.save(itemTag);
                filterItemsTag.put("Item" + i, itemTag);
            }
        }
        tag.put("FilterItems", filterItemsTag);
        
        // Save connected container position
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
                // Verificar si es un cofre y si forma parte de un cofre doble
                if (isChestBlockEntity(adjacentEntity)) {
                    BlockPos doubleChestPos = findDoubleChestPartner(adjacentPos);
                    if (doubleChestPos != null) {
                        // Es un cofre doble, usar la posición del cofre "principal" (el de menor coordenada)
                        this.connectedContainerPos = getMainChestPosition(adjacentPos, doubleChestPos);
                    } else {
                        // Es un cofre simple
                        this.connectedContainerPos = adjacentPos;
                    }
                } else {
                    // No es un cofre, usar comportamiento normal
                    this.connectedContainerPos = adjacentPos;
                }
                
                this.setChanged();
                return;
            }
        }
        
        // Si se perdió la conexión, marcar como cambiado
        if (oldContainerPos != null && this.connectedContainerPos == null) {
            this.setChanged();
        }
    }
    
    // Método auxiliar para verificar si una BlockEntity es un cofre
    private boolean isChestBlockEntity(BlockEntity entity) {
        return entity.getClass().getName().contains("ChestBlockEntity");
    }
    
    // Método auxiliar para encontrar el cofre compañero en un cofre doble
    private BlockPos findDoubleChestPartner(BlockPos chestPos) {
        if (this.level == null) return null;
        
        // Los cofres dobles solo se forman horizontalmente (norte, sur, este, oeste)
        Direction[] horizontalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        
        for (Direction direction : horizontalDirections) {
            BlockPos adjacentPos = chestPos.relative(direction);
            BlockEntity adjacentEntity = this.level.getBlockEntity(adjacentPos);
            
            // Verificar si hay otro cofre adyacente
            if (adjacentEntity != null && isChestBlockEntity(adjacentEntity)) {
                // Verificar que ambos cofres estén orientados en la misma dirección
                BlockState chestState = this.level.getBlockState(chestPos);
                BlockState adjacentState = this.level.getBlockState(adjacentPos);
                
                // Ambos deben ser cofres y tener la misma orientación
                if (chestState.getBlock().getClass().equals(adjacentState.getBlock().getClass())) {
                    // Verificar orientación si tienen la propiedad FACING
                    try {
                        if (chestState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING) &&
                            adjacentState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                            
                            Direction chestFacing = chestState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
                            Direction adjacentFacing = adjacentState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
                            
                            if (chestFacing.equals(adjacentFacing)) {
                                return adjacentPos;
                            }
                        } else {
                            // Si no tienen orientación, asumir que pueden formar cofre doble
                            return adjacentPos;
                        }
                    } catch (Exception e) {
                        // Si hay algún error con las propiedades, asumir que pueden formar cofre doble
                        return adjacentPos;
                    }
                }
            }
        }
        
        return null;
    }
    
    // Método auxiliar para obtener la posición "principal" del cofre doble
    private BlockPos getMainChestPosition(BlockPos pos1, BlockPos pos2) {
        // Usar el cofre con menor coordenada como principal
        // Prioridad: X menor, luego Z menor
        if (pos1.getX() < pos2.getX()) {
            return pos1;
        } else if (pos1.getX() > pos2.getX()) {
            return pos2;
        } else {
            // Misma X, comparar Z
            return pos1.getZ() < pos2.getZ() ? pos1 : pos2;
        }
    }

    public boolean canAcceptItem(ItemStack stack) {
        // Verificar si hay un contenedor conectado
        if (this.connectedContainerPos == null) {
            updateConnectedContainer(); // Intentar encontrar un contenedor
            if (this.connectedContainerPos == null) {
                return false; // No hay contenedor conectado
            }
        }
        
        // Verificar que el contenedor exista y sea accesible
        if (this.level == null) {
            return false;
        }
        
        BlockEntity containerEntity = this.level.getBlockEntity(this.connectedContainerPos);
        if (!(containerEntity instanceof Container) || containerEntity instanceof IndexerConnectorBlockEntity) {
            this.connectedContainerPos = null; // Resetear la conexión si el contenedor ya no existe o es otro conector
            return false;
        }

        // Verificar si es carbón o carbón vegetal y si el contenedor es un horno
        boolean isCoalOrCharcoal = stack.getItem().getDescriptionId().equals("item.minecraft.coal") || 
                                  stack.getItem().getDescriptionId().equals("item.minecraft.charcoal");
        boolean isFurnace = containerEntity.getClass().getName().contains("FurnaceBlockEntity");
        
        // Si es carbón/carbón vegetal y el contenedor es un horno, permitir siempre
        if (isCoalOrCharcoal && isFurnace) {
            return true;
        }

        // Si no hay filtros configurados, acepta cualquier ítem
        boolean hasAnyFilter = false;
        for (ItemStack filterItem : this.filterItems) {
            if (!filterItem.isEmpty()) {
                hasAnyFilter = true;
                break;
            }
        }
        
        if (!hasAnyFilter) {
            return true;
        }

        // Verificar si el ítem coincide con alguno de los filtros
        for (ItemStack filterItem : this.filterItems) {
            if (!filterItem.isEmpty() && filterItem.getItem() == stack.getItem()) {
                return true;
            }
        }
        
        return false;
    }

    public ItemStack insertItem(ItemStack stack) {
        if (!canAcceptItem(stack) || this.level == null) {
            return stack;
        }

        BlockEntity containerEntity = this.level.getBlockEntity(this.connectedContainerPos);
        if (!(containerEntity instanceof Container) || containerEntity instanceof IndexerConnectorBlockEntity) {
            return stack;
        }

        Container container = (Container) containerEntity;
        ItemStack remainder = stack.copy();
        int initialCount = remainder.getCount();

        // Verificar si es un horno y el ítem es carbón o carbón vegetal
        boolean isCoalOrCharcoal = stack.getItem().getDescriptionId().equals("item.minecraft.coal") || 
                                  stack.getItem().getDescriptionId().equals("item.minecraft.charcoal");
        boolean isFurnace = containerEntity.getClass().getName().contains("FurnaceBlockEntity");
        
        // Si es un horno (AbstractFurnaceBlockEntity) y el ítem es carbón/carbón vegetal
        if (isFurnace && isCoalOrCharcoal) {
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
        
        // Si es un horno pero NO es carbón, solo permitir inserción en el slot superior (ingredientes)
        if (isFurnace && !isCoalOrCharcoal) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Detected furnace and non-fuel item, attempting to insert into input slot");
            
            // El slot de ingredientes en AbstractFurnaceBlockEntity es 0
            final int FURNACE_INPUT_SLOT = 0;
            
            if (FURNACE_INPUT_SLOT < container.getContainerSize()) {
                ItemStack inputSlotStack = container.getItem(FURNACE_INPUT_SLOT);
                
                if (inputSlotStack.isEmpty()) {
                    // Slot de ingredientes vacío, insertar todo lo que podamos
                    int maxStackSize = Math.min(container.getMaxStackSize(), remainder.getMaxStackSize());
                    int toInsert = Math.min(remainder.getCount(), maxStackSize);
                    
                    ItemStack newStack = remainder.copy();
                    newStack.setCount(toInsert);
                    container.setItem(FURNACE_INPUT_SLOT, newStack);
                    
                    remainder.shrink(toInsert);
                    
                    if (remainder.isEmpty()) {
                        if (containerEntity instanceof BlockEntity) {
                            ((BlockEntity) containerEntity).setChanged();
                        }
                        return ItemStack.EMPTY;
                    }
                } else if (ItemStack.isSameItemSameTags(inputSlotStack, remainder)) {
                    // Mismo ítem en el slot de ingredientes, intentar apilar
                    int maxStackSize = Math.min(container.getMaxStackSize(), inputSlotStack.getMaxStackSize());
                    int space = maxStackSize - inputSlotStack.getCount();
                    
                    if (space > 0) {
                        int toInsert = Math.min(remainder.getCount(), space);
                        inputSlotStack.grow(toInsert);
                        remainder.shrink(toInsert);
                        
                        if (remainder.isEmpty()) {
                            if (containerEntity instanceof BlockEntity) {
                                ((BlockEntity) containerEntity).setChanged();
                            }
                            return ItemStack.EMPTY;
                        }
                    }
                }
                
                // Si llegamos aquí, no pudimos insertar todo en el slot de ingredientes
                if (containerEntity instanceof BlockEntity) {
                    ((BlockEntity) containerEntity).setChanged();
                }
                return remainder;
            }
        }

        // Verificar si es un cofre doble y manejar como inventario unificado
        if (isChestBlockEntity(containerEntity)) {
            BlockPos partnerPos = findDoubleChestPartner(this.connectedContainerPos);
            if (partnerPos != null) {
                // Es un cofre doble, determinar el orden correcto (cofre principal primero)
                BlockPos mainChestPos = getMainChestPosition(this.connectedContainerPos, partnerPos);
                BlockPos secondChestPos = mainChestPos.equals(this.connectedContainerPos) ? partnerPos : this.connectedContainerPos;
                
                // Usar inventario unificado con el orden correcto
                remainder = insertIntoDoubleChest(remainder, mainChestPos, secondChestPos);
                
                // Marcar ambos cofres como cambiados
                if (containerEntity instanceof BlockEntity) {
                    ((BlockEntity) containerEntity).setChanged();
                }
                BlockEntity partnerEntity = this.level.getBlockEntity(partnerPos);
                if (partnerEntity instanceof BlockEntity) {
                    ((BlockEntity) partnerEntity).setChanged();
                }
                
                int inserted = initialCount - remainder.getCount();
                if (inserted > 0) {
                    // Ya no enviamos mensajes de notificación al chat
                } else {
                    // No se pudo insertar nada
                }
                
                return remainder;
            }
        }

        // Comportamiento normal para otros contenedores o cofres simples
        for (int i = 0; i < container.getContainerSize(); i++) {
            // Si es un horno, no permitir inserción en el slot de salida (slot 2)
            if (isFurnace && i == 2) {
                continue; // Saltar el slot de salida del horno
            }
            
            ItemStack slotStack = container.getItem(i);
            
            if (slotStack.isEmpty()) {
                // Slot vacío, insertar todo lo que podamos
                int maxStackSize = Math.min(container.getMaxStackSize(), remainder.getMaxStackSize());
                int toInsert = Math.min(remainder.getCount(), maxStackSize);
                
                ItemStack newStack = remainder.copy();
                newStack.setCount(toInsert);
                container.setItem(i, newStack);
                
                remainder.shrink(toInsert);

                
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

            // Ya no enviamos mensajes de notificación al chat
        } else {

        }

        return remainder;
    }
    
    // Método auxiliar para insertar items en un cofre doble como inventario unificado
    private ItemStack insertIntoDoubleChest(ItemStack stack, BlockPos chest1Pos, BlockPos chest2Pos) {
        if (this.level == null) return stack;
        
        BlockEntity chest1Entity = this.level.getBlockEntity(chest1Pos);
        BlockEntity chest2Entity = this.level.getBlockEntity(chest2Pos);
        
        if (!(chest1Entity instanceof Container) || !(chest2Entity instanceof Container)) {
            return stack;
        }
        
        Container chest1 = (Container) chest1Entity;
        Container chest2 = (Container) chest2Entity;
        ItemStack remainder = stack.copy();
        
        // Tratar como inventario unificado de 54 slots (0-53)
        // Slots 0-26 corresponden al primer cofre, slots 27-53 al segundo cofre
        int totalSlots = chest1.getContainerSize() + chest2.getContainerSize();
        
        // Primero intentar llenar slots existentes con el mismo item (en orden secuencial)
        for (int globalSlot = 0; globalSlot < totalSlots; globalSlot++) {
            Container currentChest;
            int localSlot;
            
            if (globalSlot < chest1.getContainerSize()) {
                currentChest = chest1;
                localSlot = globalSlot;
            } else {
                currentChest = chest2;
                localSlot = globalSlot - chest1.getContainerSize();
            }
            
            ItemStack slotStack = currentChest.getItem(localSlot);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, remainder)) {
                int maxStackSize = Math.min(currentChest.getMaxStackSize(), slotStack.getMaxStackSize());
                int space = maxStackSize - slotStack.getCount();
                
                if (space > 0) {
                    int toInsert = Math.min(remainder.getCount(), space);
                    slotStack.grow(toInsert);
                    remainder.shrink(toInsert);
                    
                    if (remainder.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        
        // Luego llenar slots vacíos (en orden secuencial desde el slot 0)
        for (int globalSlot = 0; globalSlot < totalSlots; globalSlot++) {
            Container currentChest;
            int localSlot;
            
            if (globalSlot < chest1.getContainerSize()) {
                currentChest = chest1;
                localSlot = globalSlot;
            } else {
                currentChest = chest2;
                localSlot = globalSlot - chest1.getContainerSize();
            }
            
            ItemStack slotStack = currentChest.getItem(localSlot);
            if (slotStack.isEmpty()) {
                int maxStackSize = Math.min(currentChest.getMaxStackSize(), remainder.getMaxStackSize());
                int toInsert = Math.min(remainder.getCount(), maxStackSize);
                
                ItemStack newStack = remainder.copy();
                newStack.setCount(toInsert);
                currentChest.setItem(localSlot, newStack);
                
                remainder.shrink(toInsert);
                
                if (remainder.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        
        return remainder;
    }

    public List<ItemStack> getFilterItems() {
        return this.filterItems;
    }
    
    public ItemStack getFilterItem(int slot) {
        if (slot >= 0 && slot < this.filterItems.size()) {
            return this.filterItems.get(slot);
        }
        return ItemStack.EMPTY;
    }

    public void setFilterItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < FILTER_SLOTS) {
            while (this.filterItems.size() <= slot) {
                this.filterItems.add(ItemStack.EMPTY);
            }
            this.filterItems.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            if (!this.filterItems.get(slot).isEmpty()) {
                this.filterItems.get(slot).setCount(1); // Solo guardamos 1 para el filtro
            }
            this.setChanged();
        }
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
        if (slot >= 0 && slot < FILTER_SLOTS && slot < this.filterItems.size()) {
            return this.filterItems.get(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot >= 0 && slot < FILTER_SLOTS && slot < this.filterItems.size() && !this.filterItems.get(slot).isEmpty()) {
            ItemStack result = this.filterItems.get(slot).copy();
            this.filterItems.set(slot, ItemStack.EMPTY);
            this.setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot >= 0 && slot < FILTER_SLOTS && slot < this.filterItems.size()) {
            ItemStack result = this.filterItems.get(slot);
            this.filterItems.set(slot, ItemStack.EMPTY);
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < FILTER_SLOTS) {
            while (this.filterItems.size() <= slot) {
                this.filterItems.add(ItemStack.EMPTY);
            }
            
            if (!stack.isEmpty()) {
                // Buscar el primer slot disponible más cercano al inicio
                int targetSlot = findNearestEmptySlot();
                if (targetSlot != -1 && targetSlot != slot) {
                    // Mover el item al slot más cercano al inicio
                    while (this.filterItems.size() <= targetSlot) {
                        this.filterItems.add(ItemStack.EMPTY);
                    }
                    this.filterItems.set(targetSlot, stack.copy());
                    this.filterItems.get(targetSlot).setCount(1);
                    
                    // Limpiar el slot original si es diferente
                    this.filterItems.set(slot, ItemStack.EMPTY);
                } else {
                    // Si no hay slot más cercano o ya estamos en el correcto, colocar normalmente
                    this.filterItems.set(slot, stack.copy());
                    this.filterItems.get(slot).setCount(1);
                }
            } else {
                // Si el stack está vacío, simplemente limpiar el slot
                this.filterItems.set(slot, ItemStack.EMPTY);
            }
            
            this.setChanged();
        }
    }
    
    /**
     * Encuentra el primer slot vacío más cercano al inicio del filtro
     * @return el índice del slot vacío más cercano al inicio, o -1 si no hay slots vacíos
     */
    private int findNearestEmptySlot() {
        // Asegurar que la lista tenga el tamaño correcto
        while (this.filterItems.size() < FILTER_SLOTS) {
            this.filterItems.add(ItemStack.EMPTY);
        }
        
        // Buscar desde el slot 0 hacia adelante
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (this.filterItems.get(i).isEmpty()) {
                return i;
            }
        }
        return -1; // No hay slots vacíos
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
        this.filterItems.clear();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            this.filterItems.add(ItemStack.EMPTY);
        }
        this.items.clear();
    }
    
    @Override
    protected net.minecraft.core.NonNullList<ItemStack> getItems() {
        return this.items;
    }
    
    @Override
    protected void setItems(net.minecraft.core.NonNullList<ItemStack> items) {
        this.items = items;
        // Los filtros se manejan por separado, no necesitamos actualizar filterItems aquí
    }
}