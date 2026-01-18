package com.agustinbenitez.indexer.block.entity;

import com.agustinbenitez.indexer.block.IndexerConnectorBlock;
import com.agustinbenitez.indexer.init.ModBlockEntities;
import com.agustinbenitez.indexer.init.ModItems;
import com.agustinbenitez.indexer.init.ModMenuTypes;
import com.agustinbenitez.indexer.inventory.IndexerConnectorMenu;
import com.agustinbenitez.indexer.util.FilterUtils;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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

public class IndexerConnectorBlockEntity extends RandomizableContainerBlockEntity implements ExtendedScreenHandlerFactory<ModMenuTypes.BlockPosPayload> {
    private static final int BASE_FILTER_SLOTS = 9; // 3x3 grid of filter slots
    private static final int UPGRADED_FILTER_SLOTS = 18; // 6x3 when upgraded
    private int connectorLevel = 1;
    private int tickCounter = 0;
    private List<ItemStack> filterItems = new ArrayList<>();
    private BlockPos connectedContainerPos = null;
    private net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(BASE_FILTER_SLOTS, ItemStack.EMPTY);

    public IndexerConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDEXER_CONNECTOR, pos, state);
        this.tickCounter = (int)(Math.random() * 20);
        for (int i = 0; i < BASE_FILTER_SLOTS; i++) {
            filterItems.add(ItemStack.EMPTY);
        }
    }

    @Override
    public ModMenuTypes.BlockPosPayload getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
        return new ModMenuTypes.BlockPosPayload(this.worldPosition);
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
        return getCurrentFilterSlots(); // Multiple slots for filters
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Load connector level
        if (tag.contains("ConnectorLevel")) {
            this.connectorLevel = tag.getInt("ConnectorLevel");
        } else {
            this.connectorLevel = 1;
        }
        ensureFilterCapacity();
        
        // Load filter items
        this.filterItems.clear();
        if (tag.contains("FilterItems")) {
            CompoundTag filterItemsTag = tag.getCompound("FilterItems");
            int maxRead = UPGRADED_FILTER_SLOTS;
            for (int i = 0; i < maxRead; i++) {
                if (filterItemsTag.contains("Item" + i)) {
                    CompoundTag itemTag = filterItemsTag.getCompound("Item" + i);
                    if (itemTag.contains("id")) {
                        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse(itemTag.getString("id"));
                        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                        if (item != null) {
                            ItemStack stack = new ItemStack(item, 1);
                            if (itemTag.contains("custom_data")) {
                                CompoundTag customDataTag = itemTag.getCompound("custom_data");
                                net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, nbt -> {
                                    nbt.merge(customDataTag);
                                });
                            }
                            this.filterItems.add(stack);
                        } else {
                            this.filterItems.add(ItemStack.EMPTY);
                        }
                    } else {
                        this.filterItems.add(ItemStack.EMPTY);
                    }
                } else {
                    this.filterItems.add(ItemStack.EMPTY);
                }
            }
        } else {
            // Initialize with empty stacks if no data
            for (int i = 0; i < getCurrentFilterSlots(); i++) {
                this.filterItems.add(ItemStack.EMPTY);
            }
        }
        ensureFilterCapacity();
        
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ConnectorLevel", this.connectorLevel);
        
        // Save filter items
        CompoundTag filterItemsTag = new CompoundTag();
        for (int i = 0; i < this.filterItems.size() && i < getCurrentFilterSlots(); i++) {
            ItemStack filterItem = this.filterItems.get(i);
            if (!filterItem.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(filterItem.getItem());
                if (id != null) {
                    itemTag.putString("id", id.toString());
                }
                net.minecraft.world.item.component.CustomData customData = filterItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    itemTag.put("custom_data", customData.copyTag());
                }
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

        // Actualizar estado de conexión periódicamente (cada 1 segundo/20 ticks)
        entity.tickCounter++;
        if (entity.tickCounter >= 20) {
            entity.tickCounter = 0;
            
            boolean isConnected = IndexerConnectorBlock.isConnectedToController(level, pos);
            if (state.getValue(IndexerConnectorBlock.CONNECTED) != isConnected) {
                level.setBlock(pos, state.setValue(IndexerConnectorBlock.CONNECTED, isConnected), net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
        }

        // Verificar si hay un contenedor conectado
        BlockPos previousContainerPos = entity.connectedContainerPos;
        entity.updateConnectedContainer();
        
        // Ya no enviamos mensajes de notificación al chat cuando se conecta un contenedor
    }

    public void updateConnectedContainer() {
        if (this.level == null) return;

        BlockPos oldContainerPos = this.connectedContainerPos;
        this.connectedContainerPos = null;
        
        // Buscar cualquier tipo de inventario adyacente (solo laterales)
        for (Direction direction : Direction.values()) {
            if (!direction.getAxis().isHorizontal()) continue; // excluir arriba/abajo
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockEntity adjacentEntity = this.level.getBlockEntity(adjacentPos);
            
            // Verificar si es cualquier tipo de contenedor (barril, horno, etc.)
            // Pero excluir específicamente otros conectores
            if (adjacentEntity instanceof Container && !(adjacentEntity instanceof IndexerConnectorBlockEntity)) {
                BlockPos targetContainerPos;
                // Verificar si es un cofre y si forma parte de un cofre doble
                if (isChestBlockEntity(adjacentEntity)) {
                    BlockPos doubleChestPos = findDoubleChestPartner(adjacentPos);
                    if (doubleChestPos != null) {
                        // Es un cofre doble, usar la posición del cofre "principal" (el de menor coordenada)
                        targetContainerPos = getMainChestPosition(adjacentPos, doubleChestPos);
                    } else {
                        // Es un cofre simple
                        targetContainerPos = adjacentPos;
                    }
                } else {
                    // No es un cofre, usar comportamiento normal
                    targetContainerPos = adjacentPos;
                }
                // Impedir conectar si ya hay otro conector enlazado a este contenedor
                if (isContainerAlreadyConnected(targetContainerPos)) {
                    continue; // probar otra dirección
                }
                this.connectedContainerPos = targetContainerPos;
                
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

    // Verifica si un contenedor ya está conectado a otro conector adyacente
    private boolean isContainerAlreadyConnected(BlockPos containerPos) {
        if (this.level == null || containerPos == null) return false;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = containerPos.relative(dir);
            BlockEntity be = this.level.getBlockEntity(neighborPos);
            if (be instanceof IndexerConnectorBlockEntity) {
                IndexerConnectorBlockEntity other = (IndexerConnectorBlockEntity) be;
                if (other != this && containerPos.equals(other.getConnectedContainerPos())) {
                    return true;
                }
            }
        }
        return false;
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

        // Verificar si es combustible válido y si el contenedor es un horno
        boolean isCoalOrCharcoal = stack.getItem().getDescriptionId().equals("item.minecraft.coal") || 
                                  stack.getItem().getDescriptionId().equals("item.minecraft.charcoal");
        boolean isLavaBucket = stack.getItem().getDescriptionId().equals("item.minecraft.lava_bucket");
        boolean isFurnace = containerEntity.getClass().getName().contains("FurnaceBlockEntity");
        
        // Si es combustible válido (carbón, carbón vegetal o cubo de lava) y el contenedor es un horno, permitir siempre
        if ((isCoalOrCharcoal || isLavaBucket) && isFurnace) {
            return true;
        }

        // PRIORIDAD ABSOLUTA: Verificar si el item está bloqueado por un filtro de bloqueo
        if (isItemBlocked(stack)) {
            return false;
        }

        // Verificar si hay filtros positivos (no de bloqueo) configurados
        boolean hasPositiveFilters = false;
        for (ItemStack filterItem : this.filterItems) {
            if (!filterItem.isEmpty() && filterItem.getItem() != ModItems.CUSTOM_TAG_BLOCKER) {
                hasPositiveFilters = true;
                break;
            }
        }
        
        // Si solo hay filtros de bloqueo (sin filtros positivos), acepta todo lo que no esté bloqueado
        if (!hasPositiveFilters) {
            return true;
        }

        // Sistema de filtros múltiples con lógica OR dentro de cada tipo
        // y prioridad entre tipos de filtros
        
        // 1. Filtros de nombre (segunda prioridad más alta) - OR lógico entre múltiples nombres
        boolean hasNameFilters = false;
        boolean passesNameFilter = false;
        for (ItemStack filterItem : this.filterItems) {
            if (!filterItem.isEmpty() && filterItem.getItem() == ModItems.NAME_FILTER) {
                hasNameFilters = true;
                System.out.println("[DEBUG] Evaluando filtro de nombre: " + filterItem);
                System.out.println("[DEBUG] Item a evaluar: " + stack.getDisplayName().getString());
                boolean passes = FilterUtils.passesFilter(stack, filterItem);
                System.out.println("[DEBUG] ¿Pasa el filtro? " + passes);
                if (passes) {
                    passesNameFilter = true;
                    break; // Si pasa uno, ya es suficiente (OR lógico)
                }
            }
        }
        
        if (hasNameFilters) {
            System.out.println("[DEBUG] Resultado final filtros de nombre: " + passesNameFilter);
            return passesNameFilter;
        }

        // 2. Filtros de atributos/encantamientos (tercera prioridad) - OR lógico entre múltiples
        boolean hasAttributeFilters = false;
        boolean passesAttributeFilter = false;
        for (ItemStack filterItem : this.filterItems) {
            if (!filterItem.isEmpty() && filterItem.getItem() == ModItems.ATTRIBUTE_FILTER) {
                hasAttributeFilters = true;
                if (FilterUtils.passesFilter(stack, filterItem)) {
                    passesAttributeFilter = true;
                    break; // Si pasa uno, ya es suficiente (OR lógico)
                }
            }
        }
        
        if (hasAttributeFilters) {
            return passesAttributeFilter;
        }
        
        // 3. Filtros específicos (herramientas, comida, combustible, mod) - OR lógico entre múltiples
        boolean hasSpecificFilters = false;
        boolean passesSpecificFilter = false;
        for (ItemStack filterItem : this.filterItems) {
            if (!filterItem.isEmpty() && 
                (filterItem.getItem() == ModItems.TOOLS_FILTER ||
                 filterItem.getItem() == ModItems.FOOD_FILTER ||
                 filterItem.getItem() == ModItems.FUEL_FILTER)) {
                hasSpecificFilters = true;
                System.out.println("[CONNECTOR_DEBUG] Evaluando filtro específico: " + filterItem.getItem().getDescriptionId() + " para item: " + stack.getItem().getDescriptionId());
                if (FilterUtils.passesFilter(stack, filterItem)) {
                    passesSpecificFilter = true;
                    System.out.println("[CONNECTOR_DEBUG] Item PASA el filtro específico!");
                    break; // Si pasa uno, ya es suficiente (OR lógico)
                } else {
                    System.out.println("[CONNECTOR_DEBUG] Item NO pasa el filtro específico");
                }
            }
        }
        
        if (hasSpecificFilters) {
            System.out.println("[CONNECTOR_DEBUG] Resultado final filtros específicos: " + passesSpecificFilter);
            return passesSpecificFilter;
        }
        
        // 4. Filtros exactos (menor prioridad) - OR lógico entre múltiples ítems exactos
        boolean hasExactFilters = false;
        boolean passesExactFilter = false;
        for (ItemStack filterItem : this.filterItems) {
            if (!filterItem.isEmpty() && 
                filterItem.getItem() != ModItems.NAME_FILTER &&
                filterItem.getItem() != ModItems.ATTRIBUTE_FILTER &&
                filterItem.getItem() != ModItems.TOOLS_FILTER &&
                filterItem.getItem() != ModItems.FOOD_FILTER &&
                filterItem.getItem() != ModItems.FUEL_FILTER &&
                filterItem.getItem() != ModItems.CUSTOM_TAG_BLOCKER) {
                hasExactFilters = true;
                if (FilterUtils.passesFilter(stack, filterItem)) {
                    passesExactFilter = true;
                    break; // Si pasa uno, ya es suficiente (OR lógico)
                }
            }
        }
        
        if (hasExactFilters) {
            return passesExactFilter;
        }
        
        return false;
    }
    
    /**
     * Verifica si un item está bloqueado por algún filtro de bloqueo en este conector
     * @param stack el ItemStack a verificar
     * @return true si el item está bloqueado, false en caso contrario
     */
    public boolean isItemBlocked(ItemStack stack) {
        for (ItemStack filterItem : this.filterItems) {
            if (!filterItem.isEmpty() && FilterUtils.isBlockingFilter(filterItem)) {
                // Para filtros de bloqueo: si passesFilter devuelve false, significa que el item está bloqueado
                if (!FilterUtils.passesFilter(stack, filterItem)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canAcceptBuckets() {
        // Verificar si hay un contenedor conectado
        if (this.connectedContainerPos == null) {
            updateConnectedContainer();
            if (this.connectedContainerPos == null) {
                return false;
            }
        }
        
        // Verificar que el contenedor exista y sea accesible
        if (this.level == null) {
            return false;
        }
        
        BlockEntity containerEntity = this.level.getBlockEntity(this.connectedContainerPos);
        if (!(containerEntity instanceof Container) || containerEntity instanceof IndexerConnectorBlockEntity) {
            this.connectedContainerPos = null;
            return false;
        }

        // Verificar si hay filtros configurados
        boolean hasAnyFilter = false;
        boolean hasBucketFilter = false;
        
        for (ItemStack filterItem : this.filterItems) {
            if (!filterItem.isEmpty()) {
                hasAnyFilter = true;
                // Verificar si hay un filtro específico para buckets vacíos
                if (filterItem.getItem().getDescriptionId().equals("item.minecraft.bucket")) {
                    hasBucketFilter = true;
                    break;
                }
            }
        }
        
        // Si no hay filtros, puede aceptar buckets
        if (!hasAnyFilter) {
            // Verificar si hay espacio disponible en el contenedor
            Container container = (Container) containerEntity;
            ItemStack bucketStack = new ItemStack(net.minecraft.world.item.Items.BUCKET, 1);
            
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slotStack = container.getItem(i);
                if (slotStack.isEmpty()) {
                    return true; // Hay un slot vacío
                }
                if (slotStack.getItem() == bucketStack.getItem() && 
                    slotStack.getCount() < slotStack.getMaxStackSize()) {
                    return true; // Hay espacio en un stack existente
                }
            }
            return false;
        }
        
        // Si hay filtros, solo acepta si hay un filtro específico para buckets vacíos
        if (hasBucketFilter) {
            // Verificar si hay espacio disponible en el contenedor
            Container container = (Container) containerEntity;
            ItemStack bucketStack = new ItemStack(net.minecraft.world.item.Items.BUCKET, 1);
            
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slotStack = container.getItem(i);
                if (slotStack.isEmpty()) {
                    return true; // Hay un slot vacío
                }
                if (slotStack.getItem() == bucketStack.getItem() && 
                    slotStack.getCount() < slotStack.getMaxStackSize()) {
                    return true; // Hay espacio en un stack existente
                }
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
        
        // Si tenemos más de 64 items, dividir en múltiples operaciones
        if (remainder.getCount() > 64) {
            return insertItemInBatches(remainder, container, containerEntity);
        }

        // Verificar si es un horno y el ítem es combustible válido
        boolean isCoalOrCharcoal = stack.getItem().getDescriptionId().equals("item.minecraft.coal") || 
                                  stack.getItem().getDescriptionId().equals("item.minecraft.charcoal");
        boolean isLavaBucket = stack.getItem().getDescriptionId().equals("item.minecraft.lava_bucket");
        boolean isFurnace = containerEntity.getClass().getName().contains("FurnaceBlockEntity");
        
        // Si es un horno (AbstractFurnaceBlockEntity) y el ítem es combustible válido (carbón, carbón vegetal o cubo de lava)
        if (isFurnace && (isCoalOrCharcoal || isLavaBucket)) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Detected furnace and fuel item (coal/charcoal/lava bucket), attempting to insert into fuel slot");
            
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
                } else if (ItemStack.isSameItemSameComponents(fuelSlotStack, remainder)) {
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
                
                // Si llegamos aquí, significa que no pudimos insertar todo el combustible en este horno
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
            
            // Si es un horno y combustible válido, SOLO intentamos insertar en el slot de combustible
            // No continuamos con el comportamiento normal para otros slots
            if (containerEntity instanceof BlockEntity) {
                ((BlockEntity) containerEntity).setChanged();
            }
            return remainder;
        }
        
        // Si es un horno pero NO es combustible válido, solo permitir inserción en el slot superior (ingredientes)
        if (isFurnace && !(isCoalOrCharcoal || isLavaBucket)) {
            com.agustinbenitez.indexer.IndexerMod.LOGGER.info("Detected furnace and non-fuel item, attempting to insert into input slot");
            
            // El slot de ingredientes en AbstractFurnaceBlockEntity es 0
            final int FURNACE_INPUT_SLOT = 0;
            
            // Exigir filtro: si no hay filtro configurado en este conector, no insertar
            ItemStack filterItem = getFilterItem(0);
            if (filterItem.isEmpty()) {
                return remainder; // no permitir inserción sin filtro
            }
            // Solo insertar si el ítem pasa el filtro
            if (!com.agustinbenitez.indexer.util.FilterUtils.passesFilter(remainder, filterItem)) {
                return remainder;
            }

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
                } else if (ItemStack.isSameItemSameComponents(inputSlotStack, remainder)) {
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

        /* Capability support removed for Fabric port - relying on Container interface below */

        /* Bloque legacy eliminado: La lógica manual de cofre doble causaba problemas de posicionamiento */

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
            } else if (ItemStack.isSameItemSameComponents(slotStack, remainder)) {
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
        if (this.level == null) {
            return stack;
        }

        BlockEntity chest1Entity = this.level.getBlockEntity(chest1Pos);
        BlockEntity chest2Entity = this.level.getBlockEntity(chest2Pos);

        if (!(chest1Entity instanceof Container) || !(chest2Entity instanceof Container)) {
            return stack;
        }

        Container chest1 = (Container) chest1Entity;
        Container chest2 = (Container) chest2Entity;
        ItemStack remainder = stack.copy();

        // Intentar insertar en el primer cofre
        for (int i = 0; i < chest1.getContainerSize(); i++) {
            ItemStack slotStack = chest1.getItem(i);
            
            if (slotStack.isEmpty()) {
                // Slot vacío, insertar todo lo que podamos
                int maxStackSize = Math.min(chest1.getMaxStackSize(), remainder.getMaxStackSize());
                int toInsert = Math.min(remainder.getCount(), maxStackSize);
                
                ItemStack newStack = remainder.copy();
                newStack.setCount(toInsert);
                chest1.setItem(i, newStack);
                
                remainder.shrink(toInsert);
                
                if (remainder.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            } else if (ItemStack.isSameItemSameComponents(slotStack, remainder)) {
                // Mismo ítem, intentar apilar
                int maxStackSize = Math.min(chest1.getMaxStackSize(), slotStack.getMaxStackSize());
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

        // Si todavía quedan items, intentar insertar en el segundo cofre
        if (!remainder.isEmpty()) {
            for (int i = 0; i < chest2.getContainerSize(); i++) {
                ItemStack slotStack = chest2.getItem(i);
                
                if (slotStack.isEmpty()) {
                    // Slot vacío, insertar todo lo que podamos
                    int maxStackSize = Math.min(chest2.getMaxStackSize(), remainder.getMaxStackSize());
                    int toInsert = Math.min(remainder.getCount(), maxStackSize);
                    
                    ItemStack newStack = remainder.copy();
                    newStack.setCount(toInsert);
                    chest2.setItem(i, newStack);
                    
                    remainder.shrink(toInsert);
                    
                    if (remainder.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                } else if (ItemStack.isSameItemSameComponents(slotStack, remainder)) {
                    // Mismo ítem, intentar apilar
                    int maxStackSize = Math.min(chest2.getMaxStackSize(), slotStack.getMaxStackSize());
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
        }

        return remainder;
    }
    
    /**
     * Método auxiliar para insertar items en lotes cuando hay más de 64 items
     */
    private ItemStack insertItemInBatches(ItemStack stack, Container container, BlockEntity containerEntity) {
        ItemStack remainder = stack.copy();
        
        while (!remainder.isEmpty()) {
            // Crear un lote de máximo 64 items
            int batchSize = Math.min(remainder.getCount(), 64);
            ItemStack batch = remainder.copy();
            batch.setCount(batchSize);
            
            // Intentar insertar el lote usando el método original
            ItemStack batchRemainder = insertItemSingle(batch, container, containerEntity);
            
            // Calcular cuántos items se insertaron en este lote
            int inserted = batchSize - batchRemainder.getCount();
            remainder.shrink(inserted);
            
            // Si no se pudo insertar nada en este lote, no podemos continuar
            if (inserted == 0) {
                break;
            }
        }
        
        return remainder;
    }
    
    /**
     * Método original de inserción para un solo lote (máximo 64 items)
     */
    private ItemStack insertItemSingle(ItemStack stack, Container container, BlockEntity containerEntity) {
        ItemStack remainder = stack.copy();
        int initialCount = remainder.getCount();

        // Verificar si es un horno y el ítem es combustible válido
        boolean isCoalOrCharcoal = stack.getItem().getDescriptionId().equals("item.minecraft.coal") || 
                                  stack.getItem().getDescriptionId().equals("item.minecraft.charcoal");
        boolean isLavaBucket = stack.getItem().getDescriptionId().equals("item.minecraft.lava_bucket");
        boolean isFurnace = containerEntity.getClass().getName().contains("FurnaceBlockEntity");
        
        // Si es un horno (AbstractFurnaceBlockEntity) y el ítem es combustible válido (carbón, carbón vegetal o cubo de lava)
        if (isFurnace && (isCoalOrCharcoal || isLavaBucket)) {
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
                } else if (ItemStack.isSameItemSameComponents(fuelSlotStack, remainder)) {
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
            }
            
            // Si es un horno y combustible válido, SOLO intentamos insertar en el slot de combustible
            if (containerEntity instanceof BlockEntity) {
                ((BlockEntity) containerEntity).setChanged();
            }
            return remainder;
        }
        
        // Si es un horno pero NO es combustible válido, solo permitir inserción en el slot superior (ingredientes)
        if (isFurnace && !(isCoalOrCharcoal || isLavaBucket)) {
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
                } else if (ItemStack.isSameItemSameComponents(inputSlotStack, remainder)) {
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
            }
            
            if (containerEntity instanceof BlockEntity) {
                ((BlockEntity) containerEntity).setChanged();
            }
            return remainder;
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
            } else if (ItemStack.isSameItemSameComponents(slotStack, remainder)) {
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
        if (slot >= 0 && slot < getCurrentFilterSlots()) {
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
        if (slot >= 0 && slot < getCurrentFilterSlots() && slot < this.filterItems.size()) {
            return this.filterItems.get(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot >= 0 && slot < getCurrentFilterSlots() && slot < this.filterItems.size() && !this.filterItems.get(slot).isEmpty()) {
            ItemStack result = this.filterItems.get(slot).copy();
            this.filterItems.set(slot, ItemStack.EMPTY);
            this.setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot >= 0 && slot < getCurrentFilterSlots() && slot < this.filterItems.size()) {
            ItemStack result = this.filterItems.get(slot);
            this.filterItems.set(slot, ItemStack.EMPTY);
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < getCurrentFilterSlots()) {
            while (this.filterItems.size() <= slot) {
                this.filterItems.add(ItemStack.EMPTY);
            }
            
            if (!stack.isEmpty()) {
                // Si el slot ya tiene un item, permitir intercambio directo
                if (!this.filterItems.get(slot).isEmpty()) {
                    // Intercambio directo en la posición específica
                    this.filterItems.set(slot, stack.copy());
                    this.filterItems.get(slot).setCount(1);
                } else {
                    // Buscar el primer slot disponible más cercano al inicio solo si el slot está vacío
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
                }
            } else {
                // Si el stack está vacío, simplemente limpiar el slot
                this.filterItems.set(slot, ItemStack.EMPTY);
            }
            
            this.setChanged();
        }
    }
    
    /**
     * Verifica si un item ya existe en el filtro
     * @param stack el ItemStack a verificar
     * @return true si el item ya existe en el filtro, false en caso contrario
     */
    private boolean isItemAlreadyInFilter(ItemStack stack) {
        return isItemAlreadyInFilter(stack, -1);
    }
    
    /**
     * Verifica si un item ya existe en el filtro, excluyendo un slot específico
     * @param stack el ItemStack a verificar
     * @param excludeSlot el slot a excluir de la verificación (-1 para no excluir ninguno)
     * @return true si el item ya existe en el filtro, false en caso contrario
     */
    private boolean isItemAlreadyInFilter(ItemStack stack, int excludeSlot) {
        // Asegurar que la lista tenga el tamaño correcto
        while (this.filterItems.size() < getCurrentFilterSlots()) {
            this.filterItems.add(ItemStack.EMPTY);
        }
        
        // Verificar cada slot del filtro
        for (int i = 0; i < getCurrentFilterSlots(); i++) {
            if (i == excludeSlot) {
                continue; // Saltar el slot excluido
            }
            ItemStack filterItem = this.filterItems.get(i);
            if (!filterItem.isEmpty() && ItemStack.isSameItem(filterItem, stack)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Encuentra el primer slot vacío más cercano al inicio del filtro
     * @return el índice del slot vacío más cercano al inicio, o -1 si no hay slots vacíos
     */
    private int findNearestEmptySlot() {
        // Asegurar que la lista tenga el tamaño correcto
        while (this.filterItems.size() < getCurrentFilterSlots()) {
            this.filterItems.add(ItemStack.EMPTY);
        }
        
        // Buscar desde el slot 0 hacia adelante
        for (int i = 0; i < getCurrentFilterSlots(); i++) {
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < getCurrentFilterSlots()) {
            // No permitir colocar items vacíos
            if (stack.isEmpty()) {
                return true; // Permitir limpiar slots
            }
            
            // Verificar si el item ya existe en el filtro, excluyendo el slot actual
            if (isItemAlreadyInFilter(stack, slot)) {
                return false; // No permitir colocar items duplicados
            }
            
            return true; // Permitir colocar el item si no es duplicado o es intercambio en el mismo slot
        }
        return false;
    }

    @Override
    public void clearContent() {
        this.filterItems.clear();
        for (int i = 0; i < getCurrentFilterSlots(); i++) {
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

    // Upgrade API
    public int getConnectorLevel() {
        return this.connectorLevel;
    }

    public void setConnectorLevel(int level) {
        this.connectorLevel = Math.max(1, level);
        ensureFilterCapacity();
        this.setChanged();
        // Sync to client so GUI knows the new level/slot count
        if (this.level != null) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public int getCurrentFilterSlots() {
        return this.connectorLevel >= 2 ? UPGRADED_FILTER_SLOTS : BASE_FILTER_SLOTS;
    }

    // --- Client sync overrides ---
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    // Removed @Override as onDataPacket is not a vanilla method
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        net.minecraft.nbt.CompoundTag tag = pkt.getTag();
        if (tag != null && this.level != null) {
            this.loadAdditional(tag, this.level.registryAccess());
        }
    }

    public void ensureFilterCapacity() {
        int desired = getCurrentFilterSlots();
        // Resize items NonNullList to desired
        if (this.items.size() != desired) {
            net.minecraft.core.NonNullList<ItemStack> newItems = net.minecraft.core.NonNullList.withSize(desired, ItemStack.EMPTY);
            for (int i = 0; i < Math.min(this.items.size(), desired); i++) {
                newItems.set(i, this.items.get(i));
            }
            this.items = newItems;
        }
        // Ensure filterItems size
        while (this.filterItems.size() < desired) {
            this.filterItems.add(ItemStack.EMPTY);
        }
        if (this.filterItems.size() > desired) {
            this.filterItems = new ArrayList<>(this.filterItems.subList(0, desired));
        }
    }
}
