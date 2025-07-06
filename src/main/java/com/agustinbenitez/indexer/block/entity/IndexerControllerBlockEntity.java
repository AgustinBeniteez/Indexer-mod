package com.agustinbenitez.indexer.block.entity;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.DropBoxBlock;
import com.agustinbenitez.indexer.block.IndexerConnectorBlock;
import com.agustinbenitez.indexer.block.IndexerPipeBlock;
import com.agustinbenitez.indexer.init.ModBlockEntities;
import com.agustinbenitez.indexer.init.ModBlocks;
import com.agustinbenitez.indexer.menu.IndexerControllerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
// ChestBlock import removed as we now use generic Container interface
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

public class IndexerControllerBlockEntity extends BlockEntity implements MenuProvider {
    private static final int TRANSFER_COOLDOWN_MAX = 8;
    private static final int DEFAULT_ITEMS_PER_TRANSFER = 1; // Valor predeterminado
    private static final int SEARCH_RANGE = 250; // Aumentado de 10 a 50 para permitir más conectores
    
    private int itemsPerTransfer = DEFAULT_ITEMS_PER_TRANSFER; // Número de items a transferir por ciclo
    
    private boolean enabled = true;
    private int transferCooldown = 0;
    private BlockPos dropContainerPos = null;
    private int previousConnectorCount = 0;
    private boolean hasNotifiedConnection = false;
    
    // Datos para sincronizar con el cliente
    protected final ContainerData data;
    
    public IndexerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDEXER_CONTROLLER.get(), pos, state);
        
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> IndexerControllerBlockEntity.this.enabled ? 1 : 0;
                    case 1 -> IndexerControllerBlockEntity.this.hasDropContainer() ? 1 : 0;
                    case 2 -> IndexerControllerBlockEntity.this.getConnectedContainersCount();
                    case 3 -> IndexerControllerBlockEntity.this.getTotalAvailableSlots();
                    case 4 -> IndexerControllerBlockEntity.this.getItemsPerTransfer();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> IndexerControllerBlockEntity.this.enabled = value == 1;
                    // Los otros valores son de solo lectura
                }
            }

            @Override
            public int getCount() {
                return 5;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.indexer.controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new IndexerControllerMenu(id, inventory, this, this.data);
    }
    
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.enabled = tag.getBoolean("Enabled");
        this.transferCooldown = tag.getInt("TransferCooldown");
        this.previousConnectorCount = tag.getInt("PreviousConnectorCount");
        this.hasNotifiedConnection = tag.getBoolean("HasNotifiedConnection");
        
        // Cargar la velocidad de transferencia personalizada
        if (tag.contains("ItemsPerTransfer")) {
            this.itemsPerTransfer = tag.getInt("ItemsPerTransfer");
        } else {
            this.itemsPerTransfer = DEFAULT_ITEMS_PER_TRANSFER;
        }
        
        if (tag.contains("DropContainerX")) {
            this.dropContainerPos = new BlockPos(
                    tag.getInt("DropContainerX"),
                    tag.getInt("DropContainerY"),
                    tag.getInt("DropContainerZ")
            );
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Enabled", this.enabled);
        tag.putInt("TransferCooldown", this.transferCooldown);
        tag.putInt("PreviousConnectorCount", this.previousConnectorCount);
        tag.putBoolean("HasNotifiedConnection", this.hasNotifiedConnection);
        tag.putInt("ItemsPerTransfer", this.itemsPerTransfer);
        
        if (this.dropContainerPos != null) {
            tag.putInt("DropContainerX", this.dropContainerPos.getX());
            tag.putInt("DropContainerY", this.dropContainerPos.getY());
            tag.putInt("DropContainerZ", this.dropContainerPos.getZ());
        }
    }
    
    public void toggleEnabled() {
        this.enabled = !this.enabled;
        this.setChanged();
    }
    
    public boolean isEnabled() {
        return this.enabled;
    }
    
    public int getItemsPerTransfer() {
        return this.itemsPerTransfer;
    }
    
    public void setItemsPerTransfer(int value) {
        this.itemsPerTransfer = Math.max(1, value); // Asegurar que sea al menos 1
        this.setChanged();
    }
    
    public boolean hasDropContainer() {
        if (this.dropContainerPos == null) {
            updateDropContainer();
        }
        
        // Verificar si la posición existe y si realmente hay un contenedor allí
        if (this.dropContainerPos != null && this.level != null) {
            BlockEntity blockEntity = this.level.getBlockEntity(this.dropContainerPos);
            return blockEntity instanceof Container && !(blockEntity instanceof IndexerConnectorBlockEntity);
        }
        
        return false;
    }
    
    public void updateDropContainer() {
        if (this.level == null) return;
        
        BlockPos oldDropContainerPos = this.dropContainerPos;
        this.dropContainerPos = null;
        
        // Primero buscar contenedores adyacentes
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockEntity blockEntity = this.level.getBlockEntity(adjacentPos);
            // Detectar cualquier tipo de contenedor, pero excluir conectores
            if (blockEntity instanceof Container && !(blockEntity instanceof IndexerConnectorBlockEntity)) {
                this.dropContainerPos = adjacentPos;
                if (!adjacentPos.equals(oldDropContainerPos)) {
                    this.setChanged();
                    if (isBeingUsed) {
                        IndexerMod.LOGGER.info("Found adjacent container at " + adjacentPos);
                    }
                }
                return;
            }
        }
        
        // Si no hay contenedores adyacentes, buscar a través de tuberías
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        
        // Comenzar la búsqueda desde las posiciones adyacentes con tuberías
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockState adjacentState = this.level.getBlockState(adjacentPos);
            if (adjacentState.getBlock() instanceof IndexerPipeBlock) {
                // Verificar que la tubería esté conectada en esta dirección
                if (adjacentState.getValue(IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()))) {
                    queue.add(adjacentPos);
                    visited.add(adjacentPos);
                    if (isBeingUsed) {
                        IndexerMod.LOGGER.info("Pipe connected in direction " + direction + " at " + adjacentPos);
                    }
                }
            }
        }
        
        // BFS para encontrar DropBox a través de tuberías
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            BlockState currentState = this.level.getBlockState(currentPos);
            BlockEntity blockEntity = this.level.getBlockEntity(currentPos);

            // Si encontramos un DropBox, lo usamos como contenedor de drop
            if (blockEntity instanceof DropBoxBlockEntity) {
                this.dropContainerPos = currentPos;
                if (!currentPos.equals(oldDropContainerPos)) {
                    this.setChanged();
                    if (isBeingUsed) {
                        IndexerMod.LOGGER.info("Found DropBox through pipes at " + currentPos);
                    }
                }
                return;
            }

            // Explorar en todas las direcciones
            for (Direction direction : Direction.values()) {
                BlockPos nextPos = currentPos.relative(direction);
                if (visited.contains(nextPos)) continue;

                BlockState nextState = this.level.getBlockState(nextPos);
                Block nextBlock = nextState.getBlock();

                if (nextBlock instanceof IndexerPipeBlock) {
                    // Verificar que la tubería esté conectada en ambas direcciones
                    boolean currentPipeConnected = currentState.getBlock() instanceof IndexerPipeBlock && 
                                                 currentState.getValue(IndexerPipeBlock.getPropertyForDirection(direction));
                    boolean nextPipeConnected = nextState.getValue(IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()));
                    
                    if (currentPipeConnected && nextPipeConnected) {
                        queue.add(nextPos);
                        visited.add(nextPos);
                        if (isBeingUsed) {
                            IndexerMod.LOGGER.info("Following pipe connection from " + currentPos + " to " + nextPos);
                        }
                    }
                } else if (nextBlock instanceof DropBoxBlock) {
                    // Verificar que la tubería actual esté conectada al DropBox
                    boolean currentPipeConnected = currentState.getBlock() instanceof IndexerPipeBlock && 
                                                 currentState.getValue(IndexerPipeBlock.getPropertyForDirection(direction));
                    
                    if (currentPipeConnected) {
                        this.dropContainerPos = nextPos;
                        if (!nextPos.equals(oldDropContainerPos)) {
                            this.setChanged();
                            if (isBeingUsed) {
                                IndexerMod.LOGGER.info("Found DropBox at end of pipe at " + nextPos);
                            }
                        }
                        return;
                    }
                }
            }
        }
    }
    
    public void setDropContainerPos(BlockPos pos) {
        this.dropContainerPos = pos;
        this.setChanged();
    }

    // Variable para rastrear si hay jugadores interactuando con el controlador
    private boolean isBeingUsed = false;
    // Variable para rastrear si la red de tuberías ha cambiado
    private boolean networkChanged = true;
    // Variable para rastrear si el DropBox tiene ítems
    private boolean dropBoxHasItems = false;
    
    public void setBeingUsed(boolean beingUsed) {
        this.isBeingUsed = beingUsed;
        // Si se abre la interfaz, forzamos una actualización
        if (beingUsed) {
            networkChanged = true;
        }
    }
    
    public void markNetworkChanged() {
        this.networkChanged = true;
        // Invalidar los caches cuando la red cambia
        this.connectorCache = null;
        this.uniqueContainersCache = null;
        this.totalAvailableSlotsCache = -1;
    }
    
    public static void tick(Level level, BlockPos pos, BlockState state, IndexerControllerBlockEntity entity) {
        if (level.isClientSide()) return;

        // Verificar si hay cambios en la red solo cuando es necesario
        boolean checkNetwork = false;
        
        // Actualizar periódicamente solo si:
        // 1. El controlador está siendo usado por un jugador, o
        // 2. La red ha cambiado, o
        // 3. Cada 200 ticks (10 segundos) para verificaciones de mantenimiento
        if (entity.isBeingUsed || entity.networkChanged || level.getGameTime() % 200 == 0) {
            checkNetwork = true;
            entity.checkConnectionStatus(level);
            
            // Actualizar el contenedor de drop
            entity.updateDropContainer();
            
            // Verificar si el dropContainerPos sigue siendo válido
            if (entity.dropContainerPos != null) {
                BlockEntity blockEntity = level.getBlockEntity(entity.dropContainerPos);
                if (!(blockEntity instanceof Container) || blockEntity instanceof IndexerConnectorBlockEntity) {
                    // El contenedor ya no existe, no es válido, o es un conector
                    entity.dropContainerPos = null;
                    entity.setChanged();
                } else if (blockEntity instanceof DropBoxBlockEntity dropBox) {
                    // Verificar si el DropBox tiene ítems
                    entity.dropBoxHasItems = dropBox.hasItems();
                }
            }
            
            // Resetear la bandera de cambio en la red después de verificar
            entity.networkChanged = false;
        }

        if (!entity.enabled) return;

        if (entity.transferCooldown > 0) {
            entity.transferCooldown--;
            return;
        }

        // Solo intentar transferir ítems si:
        // 1. El DropBox tiene ítems, o
        // 2. Acabamos de verificar la red (para asegurarnos de que no nos perdemos nada)
        if (entity.dropBoxHasItems || checkNetwork) {
            boolean didTransfer = entity.transferItemsFromDropContainer();

            if (didTransfer) {
                entity.transferCooldown = TRANSFER_COOLDOWN_MAX;
                entity.setChanged();
            }
        }
    }

    // Variable para almacenar la referencia al BlockEntity del DropBox
    private BlockEntity dropContainerEntity;
    
    private boolean transferItemsFromDropContainer() {
        if (this.level == null || !hasDropContainer()) {
            return false;
        }

        this.dropContainerEntity = this.level.getBlockEntity(this.dropContainerPos);
        if (!(this.dropContainerEntity instanceof Container dropContainer) || this.dropContainerEntity instanceof IndexerConnectorBlockEntity) {
            return false;
        }

        // Buscar conectores en el rango
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        if (connectors.isEmpty()) {
            if (isBeingUsed) {
                IndexerMod.LOGGER.info("No connectors found for controller at " + worldPosition);
            }
            return false;
        }

        if (isBeingUsed) {
            IndexerMod.LOGGER.info("Found " + connectors.size() + " connectors for controller at " + worldPosition);
        }
        boolean transferred = false;
        
        // Primero, verificar si hay hornos que necesiten rellenar su combustible
        transferred = checkAndRefillFurnaceFuel(connectors, dropContainer) || transferred;

        // Contador para limitar la cantidad de items transferidos por ciclo
        int itemsTransferredThisCycle = 0;

        // Intentar transferir cada ítem del contenedor de drop a un conector apropiado
        for (int i = 0; i < dropContainer.getContainerSize(); i++) {
            // Si ya transferimos el máximo de items por ciclo, salir del bucle
            if (itemsTransferredThisCycle >= this.itemsPerTransfer) {
                break;
            }
            
            ItemStack stack = dropContainer.getItem(i);
            if (stack.isEmpty()) continue;

            if (isBeingUsed) {
                IndexerMod.LOGGER.info("Trying to transfer item: " + stack.getItem().getDescriptionId() + " x" + stack.getCount());
            }
            
            // Separar conectores en dos grupos: los que tienen filtro específico y los que no tienen filtro
            List<IndexerConnectorBlockEntity> connectorsWithFilter = new ArrayList<>();
            List<IndexerConnectorBlockEntity> connectorsWithoutFilter = new ArrayList<>();
            
            for (IndexerConnectorBlockEntity connector : connectors) {
                if (connector.canAcceptItem(stack)) {
                    // Verificar si el conector tiene un filtro específico para este ítem
                    if (!connector.getFilterItem().isEmpty() && connector.getFilterItem().getItem() == stack.getItem()) {
                        connectorsWithFilter.add(connector);
                    } else {
                        connectorsWithoutFilter.add(connector);
                    }
                }
            }
            
            if (isBeingUsed) {
                IndexerMod.LOGGER.info("  Found " + connectorsWithFilter.size() + " connectors with specific filter and " + 
                                     connectorsWithoutFilter.size() + " connectors without filter for this item");
            }
            
            // Primero intentar con conectores que tienen filtro específico
            boolean itemTransferred = false;
            ItemStack remainder = stack.copy();
            
            // Intentar primero con los conectores que tienen filtro específico
            for (IndexerConnectorBlockEntity connector : connectorsWithFilter) {
                if (isBeingUsed) {
                    IndexerMod.LOGGER.info("  Trying connector with filter at " + connector.getBlockPos());
                }
                
                // Transferir múltiples ítems a la vez según la velocidad configurada
                ItemStack transferStack = remainder.copy();
                // Intentamos transferir tantos ítems como sea posible, hasta el máximo por ciclo
                int itemsToTransfer = Math.min(transferStack.getCount(), this.itemsPerTransfer);
                transferStack.setCount(itemsToTransfer);
                
                ItemStack newRemainder = connector.insertItem(transferStack);
                
                if (newRemainder.getCount() < transferStack.getCount()) {
                    // Calcular cuántos items se transfirieron realmente
                    int itemsTransferred = transferStack.getCount() - newRemainder.getCount();
                    
                    // Actualizar el stack original
                    remainder.shrink(itemsTransferred);
                    dropContainer.setItem(i, remainder);
                    
                    if (isBeingUsed) {
                        IndexerMod.LOGGER.info("  Transferred " + itemsTransferred + " items to filtered connector");
                    }
                    transferred = true;
                    itemTransferred = true;
                    itemsTransferredThisCycle += itemsTransferred;
                    
                    if (remainder.isEmpty()) {
                        break;
                    }
                }
                
                // Si ya transferimos el máximo de items por ciclo, salir del bucle
                if (itemsTransferredThisCycle >= this.itemsPerTransfer) {
                    break;
                }
            }
            
            // Si todavía quedan ítems y no hemos alcanzado el límite, intentar con los conectores sin filtro
            if (!remainder.isEmpty() && !connectorsWithoutFilter.isEmpty() && itemsTransferredThisCycle < this.itemsPerTransfer) {
                for (IndexerConnectorBlockEntity connector : connectorsWithoutFilter) {
                    if (isBeingUsed) {
                        IndexerMod.LOGGER.info("  Trying connector without filter at " + connector.getBlockPos());
                    }
                    
                    // Transferir múltiples ítems a la vez según la velocidad configurada
                    ItemStack transferStack = remainder.copy();
                    // Intentamos transferir tantos ítems como sea posible, hasta el máximo por ciclo
                    int itemsToTransfer = Math.min(transferStack.getCount(), this.itemsPerTransfer);
                    transferStack.setCount(itemsToTransfer);
                    
                    ItemStack newRemainder = connector.insertItem(transferStack);
                    
                    if (newRemainder.getCount() < transferStack.getCount()) {
                        // Calcular cuántos items se transfirieron realmente
                        int itemsTransferred = transferStack.getCount() - newRemainder.getCount();
                        
                        // Actualizar el stack original
                        remainder.shrink(itemsTransferred);
                        dropContainer.setItem(i, remainder);
                        
                        if (isBeingUsed) {
                            IndexerMod.LOGGER.info("  Transferred " + itemsTransferred + " items to non-filtered connector");
                        }
                        transferred = true;
                        itemTransferred = true;
                        itemsTransferredThisCycle += itemsTransferred;
                        
                        if (remainder.isEmpty()) {
                            break;
                        }
                    }
                    
                    // Si ya transferimos el máximo de items por ciclo, salir del bucle
                    if (itemsTransferredThisCycle >= this.itemsPerTransfer) {
                        break;
                    }
                }
            }
        }

        // Actualizar el estado de dropBoxHasItems después de la transferencia
        if (transferred && this.dropContainerEntity instanceof DropBoxBlockEntity) {
            // Verificar si el DropBox todavía tiene ítems
            this.dropBoxHasItems = ((DropBoxBlockEntity) this.dropContainerEntity).hasItems();
        }

        return transferred;
    }
    
    // Cache para los contenedores conectados y slots disponibles
    private Set<BlockPos> uniqueContainersCache = null;
    private int totalAvailableSlotsCache = -1;
    
    public int getConnectedContainersCount() {
        // Usar el cache si está disponible y la red no ha cambiado
        if (!networkChanged && uniqueContainersCache != null) {
            return uniqueContainersCache.size();
        }
        
        // Si necesitamos recalcular, actualizar el cache
        updateContainerCache();
        return uniqueContainersCache.size();
    }
    
    public int getTotalAvailableSlots() {
        // Usar el cache si está disponible y la red no ha cambiado
        if (!networkChanged && totalAvailableSlotsCache >= 0) {
            return totalAvailableSlotsCache;
        }
        
        // Si necesitamos recalcular, actualizar el cache
        updateContainerCache();
        return totalAvailableSlotsCache;
    }
    
    private void updateContainerCache() {
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        Set<BlockPos> uniqueContainers = new HashSet<>();
        int totalSlots = 0;
        
        for (IndexerConnectorBlockEntity connector : connectors) {
            BlockPos containerPos = connector.getConnectedContainerPos();
            if (containerPos != null && !uniqueContainers.contains(containerPos)) {
                uniqueContainers.add(containerPos);
                BlockEntity containerEntity = this.level.getBlockEntity(containerPos);
                if (containerEntity instanceof Container container) {
                    // Contar slots vacíos
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        if (container.getItem(i).isEmpty()) {
                            totalSlots++;
                        }
                    }
                }
            }
        }
        
        // Actualizar los caches
        uniqueContainersCache = uniqueContainers;
        totalAvailableSlotsCache = totalSlots;
    }

    /**
     * Verifica si hay hornos conectados que necesiten rellenar su combustible y los rellena con carbón del DropBox
     * @param connectors Lista de conectores encontrados
     * @param dropContainer El contenedor de origen (DropBox)
     * @return true si se transfirió algún ítem, false en caso contrario
     */
    private boolean checkAndRefillFurnaceFuel(List<IndexerConnectorBlockEntity> connectors, Container dropContainer) {
        if (this.level == null) return false;
        
        boolean transferred = false;
        
        // Buscar conectores que estén conectados a hornos
        for (IndexerConnectorBlockEntity connector : connectors) {
            BlockPos containerPos = connector.getConnectedContainerPos();
            if (containerPos == null) continue;
            
            BlockEntity containerEntity = this.level.getBlockEntity(containerPos);
            if (containerEntity == null) continue;
            
            // Verificar si es un horno
            if (containerEntity.getClass().getName().contains("FurnaceBlockEntity") && containerEntity instanceof Container furnace) {
                // El slot de combustible en AbstractFurnaceBlockEntity es 1
                final int FURNACE_FUEL_SLOT = 1;
                
                if (FURNACE_FUEL_SLOT < furnace.getContainerSize()) {
                    ItemStack fuelSlotStack = furnace.getItem(FURNACE_FUEL_SLOT);
                    
                    // Verificar si el slot de combustible está vacío o tiene menos de 64 ítems
                    boolean needsRefill = fuelSlotStack.isEmpty() || 
                                         (fuelSlotStack.getItem().getDescriptionId().equals("item.minecraft.coal") || 
                                          fuelSlotStack.getItem().getDescriptionId().equals("item.minecraft.charcoal")) && 
                                         fuelSlotStack.getCount() < 64;
                    
                    if (needsRefill) {
                        if (isBeingUsed) {
                            IndexerMod.LOGGER.info("Furnace at " + containerPos + " needs fuel refill");
                        }
                        
                        // Buscar carbón o carbón vegetal en el DropBox
                        for (int i = 0; i < dropContainer.getContainerSize(); i++) {
                            ItemStack stack = dropContainer.getItem(i);
                            if (stack.isEmpty()) continue;
                            
                            boolean isCoalOrCharcoal = stack.getItem().getDescriptionId().equals("item.minecraft.coal") || 
                                                     stack.getItem().getDescriptionId().equals("item.minecraft.charcoal");
                            
                            if (isCoalOrCharcoal) {
                                // Calcular cuánto carbón necesitamos transferir
                                int spaceInFurnace = fuelSlotStack.isEmpty() ? 64 : 64 - fuelSlotStack.getCount();
                                int toTransfer = Math.min(stack.getCount(), spaceInFurnace);
                                
                                if (toTransfer > 0) {
                                    // Transferir el carbón al horno
                                    if (fuelSlotStack.isEmpty()) {
                                        // Slot vacío, crear nuevo stack
                                        ItemStack newStack = stack.copy();
                                        newStack.setCount(toTransfer);
                                        furnace.setItem(FURNACE_FUEL_SLOT, newStack);
                                    } else {
                                        // Añadir al stack existente
                                        fuelSlotStack.grow(toTransfer);
                                    }
                                    
                                    // Actualizar el stack en el DropBox
                                    stack.shrink(toTransfer);
                                    if (stack.isEmpty()) {
                                        dropContainer.setItem(i, ItemStack.EMPTY);
                                    } else {
                                        dropContainer.setItem(i, stack);
                                    }
                                    
                                    // Marcar como cambiados
                                    if (containerEntity instanceof BlockEntity) {
                                        ((BlockEntity) containerEntity).setChanged();
                                    }
                                    if (dropContainerEntity instanceof BlockEntity) {
                                        ((BlockEntity) dropContainerEntity).setChanged();
                                    }
                                    
                                    if (isBeingUsed) {
                                        IndexerMod.LOGGER.info("Refilled furnace at " + containerPos + " with " + toTransfer + " coal/charcoal");
                                    }
                                    transferred = true;
                                    break; // Salir del bucle de ítems del DropBox
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Actualizar el estado de dropBoxHasItems después de la transferencia
        if (transferred && this.dropContainerEntity instanceof DropBoxBlockEntity) {
            // Verificar si el DropBox todavía tiene ítems
            this.dropBoxHasItems = ((DropBoxBlockEntity) this.dropContainerEntity).hasItems();
        }
        
        return transferred;
    }
    
    private void checkConnectionStatus(Level level) {
        // Obtener el número actual de conectores
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        int currentConnectorCount = connectors.size();
        
        // Imprimir información de depuración solo si el controlador está siendo usado
        if (isBeingUsed) {
            IndexerMod.LOGGER.info("Controller at " + worldPosition + " found " + currentConnectorCount + " connectors");
            for (IndexerConnectorBlockEntity connector : connectors) {
                IndexerMod.LOGGER.info("  - Connector at " + connector.getBlockPos() + ", connected container: " + connector.getConnectedContainerPos());
            }
        }
        
        // Verificar si hay cambios en las conexiones
        if (currentConnectorCount != previousConnectorCount) {
            // Actualizar el estado de notificación sin enviar mensajes al chat
            if (currentConnectorCount > previousConnectorCount) {
                hasNotifiedConnection = true;
            } else if (currentConnectorCount == 0) {
                hasNotifiedConnection = false;
            }
            
            // Actualizar el contador previo
            previousConnectorCount = currentConnectorCount;
            setChanged();
            
            // Marcar que la red ha cambiado para forzar una actualización completa
            networkChanged = true;
        }
    }
    
    // Cache de conectores para evitar búsquedas repetidas
    private List<IndexerConnectorBlockEntity> connectorCache = null;
    
    private List<IndexerConnectorBlockEntity> findConnectors() {
        // Si la red no ha cambiado y tenemos un cache válido, devolver el cache
        if (!networkChanged && connectorCache != null) {
            return connectorCache;
        }
        
        List<IndexerConnectorBlockEntity> connectors = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        
        // Reducir la verbosidad de los logs para mejorar el rendimiento
        if (isBeingUsed) {
            IndexerMod.LOGGER.info("Starting connector search from controller at " + worldPosition);
        }

        // Comenzar la búsqueda desde las posiciones adyacentes
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockState adjacentState = this.level.getBlockState(adjacentPos);
            if (adjacentState.getBlock() instanceof IndexerPipeBlock) {
                // Verificar que la tubería esté conectada en esta dirección
                if (adjacentState.getValue(IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()))) {
                    queue.add(adjacentPos);
                    visited.add(adjacentPos);
                    if (isBeingUsed) {
                        IndexerMod.LOGGER.info("Pipe connected in direction " + direction + " at " + adjacentPos);
                    }
                }
            } else if (adjacentState.getBlock() instanceof IndexerConnectorBlock) {
                // Si hay un conector directamente adyacente, agregarlo
                BlockEntity blockEntity = this.level.getBlockEntity(adjacentPos);
                if (blockEntity instanceof IndexerConnectorBlockEntity) {
                    connectors.add((IndexerConnectorBlockEntity) blockEntity);
                    if (isBeingUsed) {
                        IndexerMod.LOGGER.info("Found adjacent connector at " + adjacentPos);
                    }
                }
            }
        }

        // BFS para encontrar conectores a través de tuberías
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            BlockState currentState = this.level.getBlockState(currentPos);
            BlockEntity blockEntity = this.level.getBlockEntity(currentPos);

            if (blockEntity instanceof IndexerConnectorBlockEntity) {
                connectors.add((IndexerConnectorBlockEntity) blockEntity);
                if (isBeingUsed) {
                    IndexerMod.LOGGER.info("Found connector through pipes at " + currentPos);
                }
                continue;
            }

            // Explorar en todas las direcciones
            for (Direction direction : Direction.values()) {
                BlockPos nextPos = currentPos.relative(direction);
                if (visited.contains(nextPos)) continue;

                BlockState nextState = this.level.getBlockState(nextPos);
                Block nextBlock = nextState.getBlock();

                if (nextBlock instanceof IndexerPipeBlock) {
                    // Verificar que la tubería esté conectada en ambas direcciones
                    boolean currentPipeConnected = currentState.getBlock() instanceof IndexerPipeBlock && 
                                                 currentState.getValue(IndexerPipeBlock.getPropertyForDirection(direction));
                    boolean nextPipeConnected = nextState.getValue(IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()));
                    
                    if (currentPipeConnected && nextPipeConnected) {
                        queue.add(nextPos);
                        visited.add(nextPos);
                        if (isBeingUsed) {
                            IndexerMod.LOGGER.info("Following pipe connection from " + currentPos + " to " + nextPos);
                        }
                    }
                } else if (nextBlock instanceof IndexerConnectorBlock) {
                    // Verificar que la tubería actual esté conectada al conector
                    boolean currentPipeConnected = currentState.getBlock() instanceof IndexerPipeBlock && 
                                                 currentState.getValue(IndexerPipeBlock.getPropertyForDirection(direction));
                    
                    if (currentPipeConnected) {
                        BlockEntity nextEntity = this.level.getBlockEntity(nextPos);
                        if (nextEntity instanceof IndexerConnectorBlockEntity) {
                            connectors.add((IndexerConnectorBlockEntity) nextEntity);
                            visited.add(nextPos);
                            if (isBeingUsed) {
                                IndexerMod.LOGGER.info("Found connector at end of pipe at " + nextPos);
                            }
                        }
                    }
                }
            }
        }

        if (isBeingUsed) {
            IndexerMod.LOGGER.info("Connector search completed. Found " + connectors.size() + " connectors. Visited " + visited.size() + " blocks.");
        }
        
        // Actualizar el cache
        connectorCache = connectors;
        return connectors;
    }


}