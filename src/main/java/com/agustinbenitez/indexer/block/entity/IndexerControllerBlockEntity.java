package com.agustinbenitez.indexer.block.entity;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.DropBoxBlock;
import com.agustinbenitez.indexer.block.IndexerConnectorBlock;
import com.agustinbenitez.indexer.block.IndexerControllerBlock;
import com.agustinbenitez.indexer.block.IndexerPipeBlock;
import com.agustinbenitez.indexer.init.ModBlockEntities;
import com.agustinbenitez.indexer.init.ModBlocks;
import com.agustinbenitez.indexer.menu.IndexerControllerNetworkMenu;
import com.agustinbenitez.indexer.util.FilterUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private int currentUpgradeLevel = 0; // Nivel actual de mejora (0=sin mejora, 1=básica, 2=cobre, 3=avanzada, 4=élite, 5=definitiva)
    
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
                    case 5 -> IndexerControllerBlockEntity.this.getTotalCapacity();
                    case 6 -> IndexerControllerBlockEntity.this.getOccupiedSlots();
                    case 7 -> IndexerControllerBlockEntity.this.getCurrentUpgradeLevel();
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
                return 8;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.indexer.controller.network");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new IndexerControllerNetworkMenu(id, inventory, this, this.data);
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
        
        // Cargar el nivel de mejora actual
        if (tag.contains("CurrentUpgradeLevel")) {
            this.currentUpgradeLevel = tag.getInt("CurrentUpgradeLevel");
        } else {
            this.currentUpgradeLevel = 0;
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
        tag.putInt("CurrentUpgradeLevel", this.currentUpgradeLevel);
        
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
    
    public int getCurrentUpgradeLevel() {
        return this.currentUpgradeLevel;
    }
    
    public void setCurrentUpgradeLevel(int level) {
        this.currentUpgradeLevel = Math.max(0, Math.min(5, level)); // Asegurar que esté entre 0 y 5
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
        // 2. Acabamos de verificar la red (para asegurarnos de que no nos perdemos nada), o
        // 3. Cada 40 ticks (2 segundos) para verificar hornos independientemente del dropbox
        boolean checkFurnaces = (level.getGameTime() % 40 == 0);
        
        if (entity.dropBoxHasItems || checkNetwork || checkFurnaces) {
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
            }
            return false;
        }

        if (isBeingUsed) {
        }
        boolean transferred = false;
        
        // Primero, verificar si hay hornos que necesiten rellenar su combustible
        transferred = checkAndRefillFurnaceFuel(connectors, dropContainer) || transferred;
        
        // Segundo, verificar si hay hornos que necesiten items de entrada desde DropBox o cofres conectados
        transferred = checkAndRefillFurnaceInput(connectors, dropContainer) || transferred;

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
            }
            
            // Separar conectores en dos grupos: los que tienen filtro específico y los que no tienen filtro
            // Ahora también excluimos los conectores que bloquean este item específico
            List<IndexerConnectorBlockEntity> connectorsWithFilter = new ArrayList<>();
            List<IndexerConnectorBlockEntity> connectorsWithoutFilter = new ArrayList<>();
            
            for (IndexerConnectorBlockEntity connector : connectors) {
                // Si este conector bloquea el item, no lo incluimos en ninguna lista
                if (connector.isItemBlocked(stack)) {
                    continue; // Saltar este conector específico, pero continuar con otros
                }
                
                if (connector.canAcceptItem(stack)) {
                    // Verificar si el conector tiene un filtro específico configurado
                    boolean hasSpecificFilter = false;
                    
                    // Verificar todos los slots de filtro para determinar si hay un filtro específico
                    for (ItemStack filterItem : connector.getFilterItems()) {
                        if (!filterItem.isEmpty()) {
                            hasSpecificFilter = true;
                            break;
                        }
                    }
                    
                    if (hasSpecificFilter) {
                        connectorsWithFilter.add(connector);
                    } else {
                        connectorsWithoutFilter.add(connector);
                    }
                }
            }
            
            if (isBeingUsed) {

            }
            
            // Primero intentar con conectores que tienen filtro específico
            boolean itemTransferred = false;
            ItemStack remainder = stack.copy();
            
            // Intentar primero con los conectores que tienen filtro específico
            for (IndexerConnectorBlockEntity connector : connectorsWithFilter) {
                if (isBeingUsed) {

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
            
            // COMPORTAMIENTO CORREGIDO: NO usar conectores sin filtro como fallback
            // Si hay conectores con filtros específicos, SOLO usar esos conectores
            // Los items que no puedan ir a conectores con filtros deben quedarse en el dropbox
            // Solo usar conectores sin filtro si NO HAY conectores con filtros para este item
            
            // Solo intentar con conectores sin filtro si NO había conectores con filtros específicos
            // que pudieran aceptar este item
            if (!remainder.isEmpty() && !connectorsWithoutFilter.isEmpty() && 
                connectorsWithFilter.isEmpty() && itemsTransferredThisCycle < this.itemsPerTransfer) {
                
                for (IndexerConnectorBlockEntity connector : connectorsWithoutFilter) {
                    if (isBeingUsed) {

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
    private int totalCapacityCache = -1;
    private int occupiedSlotsCache = -1;
    
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
    
    public int getTotalCapacity() {
        // Usar el cache si está disponible y la red no ha cambiado
        if (!networkChanged && totalCapacityCache >= 0) {
            return totalCapacityCache;
        }
        
        // Si necesitamos recalcular, actualizar el cache
        updateContainerCache();
        return totalCapacityCache;
    }
    
    public int getOccupiedSlots() {
        // Usar el cache si está disponible y la red no ha cambiado
        if (!networkChanged && occupiedSlotsCache >= 0) {
            return occupiedSlotsCache;
        }
        
        // Si necesitamos recalcular, actualizar el cache
        updateContainerCache();
        return occupiedSlotsCache;
    }
    
    private void updateContainerCache() {
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        Set<BlockPos> uniqueContainers = new HashSet<>();
        int totalSlots = 0;
        int totalCapacity = 0;
        int occupiedSlots = 0;
        
        for (IndexerConnectorBlockEntity connector : connectors) {
            BlockPos containerPos = connector.getConnectedContainerPos();
            if (containerPos != null && !uniqueContainers.contains(containerPos)) {
                uniqueContainers.add(containerPos);
                BlockEntity containerEntity = this.level.getBlockEntity(containerPos);
                if (containerEntity instanceof Container container) {
                    // Contar slots totales y ocupados
                    int containerSize = container.getContainerSize();
                    totalCapacity += containerSize;
                    
                    for (int i = 0; i < containerSize; i++) {
                        if (container.getItem(i).isEmpty()) {
                            totalSlots++;
                        } else {
                            occupiedSlots++;
                        }
                    }
                }
            }
        }
        
        // Actualizar los caches
        uniqueContainersCache = uniqueContainers;
        totalAvailableSlotsCache = totalSlots;
        totalCapacityCache = totalCapacity;
        occupiedSlotsCache = occupiedSlots;
    }

    /**
     * Verifica si hay hornos conectados que necesiten rellenar su combustible y los rellena con carbón o lava del DropBox
     * o de cofres conectados al sistema
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
                    
                    // Verificar si el slot de combustible está vacío o tiene combustible compatible
                    boolean needsRefill = fuelSlotStack.isEmpty() || 
                                         (fuelSlotStack.getItem().getDescriptionId().equals("item.minecraft.coal") || 
                                          fuelSlotStack.getItem().getDescriptionId().equals("item.minecraft.charcoal")) && 
                                         fuelSlotStack.getCount() < 64;
                    
                    if (needsRefill) {
                        if (isBeingUsed) {

                        }
                        
                        // Primero intentar buscar combustible en el DropBox (carbón, carbón vegetal o lava bucket)
                        boolean foundFuelInDropBox = false;
                        if (dropContainer != null) {
                            for (int i = 0; i < dropContainer.getContainerSize(); i++) {
                            ItemStack stack = dropContainer.getItem(i);
                            if (stack.isEmpty()) continue;
                            
                            boolean isCoalOrCharcoal = stack.getItem().getDescriptionId().equals("item.minecraft.coal") || 
                                                     stack.getItem().getDescriptionId().equals("item.minecraft.charcoal");
                            boolean isLavaBucket = stack.getItem().getDescriptionId().equals("item.minecraft.lava_bucket");
                            
                            if (isCoalOrCharcoal || isLavaBucket) {
                                // Para lava buckets, solo transferir uno a la vez
                                if (isLavaBucket) {
                                    // Solo transferir si el slot está vacío (lava buckets no se apilan)
                                    if (fuelSlotStack.isEmpty()) {
                                        // Transferir el lava bucket al horno
                                        ItemStack newStack = stack.copy();
                                        newStack.setCount(1);
                                        furnace.setItem(FURNACE_FUEL_SLOT, newStack);
                                        
                                        // Remover el lava bucket del DropBox
                                        stack.shrink(1);
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

                                        }
                                        transferred = true;
                                        foundFuelInDropBox = true;
                                        break; // Salir del bucle de ítems del DropBox
                                    }
                                } else if (isCoalOrCharcoal) {
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

                                        }
                                        transferred = true;
                                        foundFuelInDropBox = true;
                                        break; // Salir del bucle de ítems del DropBox
                                    }
                                }
                            }
                        }
                        }
                        
                        // Si no se encontró combustible en el DropBox, buscar en cofres conectados
                        if (!foundFuelInDropBox) {
                            // Buscar conectores que estén conectados a cofres
                            for (IndexerConnectorBlockEntity chestConnector : connectors) {
                                BlockPos chestPos = chestConnector.getConnectedContainerPos();
                                if (chestPos == null) continue;
                                
                                BlockEntity chestEntity = this.level.getBlockEntity(chestPos);
                                if (chestEntity == null) continue;
                                
                                // Verificar si es un cofre u otro contenedor (no horno)
                                if (chestEntity instanceof Container chest && 
                                    !chestEntity.getClass().getName().contains("FurnaceBlockEntity")) {
                                    
                                    // Buscar carbón, carbón vegetal o lava buckets en el cofre
                                    for (int i = 0; i < chest.getContainerSize(); i++) {
                                        ItemStack stack = chest.getItem(i);
                                        if (stack.isEmpty()) continue;
                                        
                                        boolean isCoalOrCharcoal = stack.getItem().getDescriptionId().equals("item.minecraft.coal") || 
                                                                 stack.getItem().getDescriptionId().equals("item.minecraft.charcoal");
                                        boolean isLavaBucket = stack.getItem().getDescriptionId().equals("item.minecraft.lava_bucket");
                                        
                                        if (isCoalOrCharcoal || isLavaBucket) {
                                            // Para lava buckets, solo transferir uno a la vez
                                            if (isLavaBucket) {
                                                // Solo transferir si el slot está vacío (lava buckets no se apilan)
                                                if (fuelSlotStack.isEmpty()) {
                                                    // Transferir el lava bucket al horno
                                                    ItemStack newStack = stack.copy();
                                                    newStack.setCount(1);
                                                    furnace.setItem(FURNACE_FUEL_SLOT, newStack);
                                                    
                                                    // Remover el lava bucket del cofre
                                                    stack.shrink(1);
                                                    if (stack.isEmpty()) {
                                                        chest.setItem(i, ItemStack.EMPTY);
                                                    } else {
                                                        chest.setItem(i, stack);
                                                    }
                                                    
                                                    // Marcar como cambiados
                                                    if (containerEntity instanceof BlockEntity) {
                                                        ((BlockEntity) containerEntity).setChanged();
                                                    }
                                                    if (chestEntity instanceof BlockEntity) {
                                                        ((BlockEntity) chestEntity).setChanged();
                                                    }
                                                    
                                                    if (isBeingUsed) {

                                                    }
                                                    transferred = true;
                                                    break; // Salir del bucle de items del cofre ya que encontramos y transferimos combustible
                                                }
                                            } else if (isCoalOrCharcoal) {
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
                                                    
                                                    // Actualizar el stack en el cofre
                                                    stack.shrink(toTransfer);
                                                    if (stack.isEmpty()) {
                                                        chest.setItem(i, ItemStack.EMPTY);
                                                    } else {
                                                        chest.setItem(i, stack);
                                                    }
                                                    
                                                    // Marcar como cambiados
                                                    if (containerEntity instanceof BlockEntity) {
                                                        ((BlockEntity) containerEntity).setChanged();
                                                    }
                                                    if (chestEntity instanceof BlockEntity) {
                                                        ((BlockEntity) chestEntity).setChanged();
                                                    }
                                                    
                                                    if (isBeingUsed) {

                                                    }
                                                    transferred = true;
                                                    break; // Salir del bucle de items del cofre ya que encontramos y transferimos combustible
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Si ya transferimos combustible para este horno, pasar al siguiente cofre
                                    if (transferred) {
                                        break;
                                    }
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
    
    /**
     * Verifica si hay hornos conectados que necesiten items de entrada y los rellena desde DropBox
     * y desde cofres conectados, basándose en el filtro del conector.
     * Requiere que el conector tenga un filtro configurado.
     */
    private boolean checkAndRefillFurnaceInput(List<IndexerConnectorBlockEntity> connectors, Container dropContainer) {
        if (this.level == null) return false;

        boolean transferred = false;

        for (IndexerConnectorBlockEntity furnaceConnector : connectors) {
            BlockPos furnacePos = furnaceConnector.getConnectedContainerPos();
            if (furnacePos == null) continue;

            BlockEntity furnaceEntity = this.level.getBlockEntity(furnacePos);
            if (furnaceEntity == null) continue;

            if (furnaceEntity.getClass().getName().contains("FurnaceBlockEntity") && furnaceEntity instanceof Container furnace) {
                final int FURNACE_INPUT_SLOT = 0;
                if (FURNACE_INPUT_SLOT >= furnace.getContainerSize()) continue;

                ItemStack inputSlotStack = furnace.getItem(FURNACE_INPUT_SLOT);
                ItemStack filterItem = furnaceConnector.getFilterItem(0);
                if (filterItem.isEmpty()) continue; // no input without filter

                boolean hasSpace = inputSlotStack.isEmpty() ||
                                   (FilterUtils.passesFilter(inputSlotStack, filterItem) &&
                                    inputSlotStack.getCount() < inputSlotStack.getMaxStackSize());
                if (!hasSpace) continue;

                boolean filledFromDropBox = false;
                if (dropContainer != null) {
                    for (int i = 0; i < dropContainer.getContainerSize(); i++) {
                        ItemStack stack = dropContainer.getItem(i);
                        if (stack.isEmpty()) continue;
                        if (!FilterUtils.passesFilter(stack, filterItem)) continue;

                        int spaceInFurnace = inputSlotStack.isEmpty() ? stack.getMaxStackSize()
                                : inputSlotStack.getMaxStackSize() - inputSlotStack.getCount();
                        int toTransfer = Math.min(stack.getCount(), spaceInFurnace);
                        if (toTransfer <= 0) continue;

                        if (inputSlotStack.isEmpty()) {
                            ItemStack newStack = stack.copy();
                            newStack.setCount(toTransfer);
                            furnace.setItem(FURNACE_INPUT_SLOT, newStack);
                        } else {
                            inputSlotStack.grow(toTransfer);
                        }

                        stack.shrink(toTransfer);
                        dropContainer.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);

                        if (furnaceEntity instanceof BlockEntity) {
                            ((BlockEntity) furnaceEntity).setChanged();
                        }
                        if (dropContainerEntity instanceof BlockEntity) {
                            ((BlockEntity) dropContainerEntity).setChanged();
                        }

                        transferred = true;
                        filledFromDropBox = true;
                        break;
                    }
                }

                if (!filledFromDropBox) {
                    for (IndexerConnectorBlockEntity chestConnector : connectors) {
                        BlockPos chestPos = chestConnector.getConnectedContainerPos();
                        if (chestPos == null || chestPos.equals(furnacePos)) continue;

                        BlockEntity chestEntity = this.level.getBlockEntity(chestPos);
                        if (chestEntity == null) continue;

                        if (chestEntity instanceof Container chest &&
                            !chestEntity.getClass().getName().contains("FurnaceBlockEntity")) {

                            for (int i = 0; i < chest.getContainerSize(); i++) {
                                ItemStack stack = chest.getItem(i);
                                if (stack.isEmpty()) continue;
                                if (!FilterUtils.passesFilter(stack, filterItem)) continue;

                                int spaceInFurnace = inputSlotStack.isEmpty() ? stack.getMaxStackSize()
                                        : inputSlotStack.getMaxStackSize() - inputSlotStack.getCount();
                                int toTransfer = Math.min(stack.getCount(), spaceInFurnace);
                                if (toTransfer <= 0) continue;

                                if (inputSlotStack.isEmpty()) {
                                    ItemStack newStack = stack.copy();
                                    newStack.setCount(toTransfer);
                                    furnace.setItem(FURNACE_INPUT_SLOT, newStack);
                                } else {
                                    inputSlotStack.grow(toTransfer);
                                }

                                stack.shrink(toTransfer);
                                chest.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);

                                if (furnaceEntity instanceof BlockEntity) {
                                    ((BlockEntity) furnaceEntity).setChanged();
                                }
                                if (chestEntity instanceof BlockEntity) {
                                    ((BlockEntity) chestEntity).setChanged();
                                }

                                transferred = true;
                                break;
                            }
                        }

                        if (transferred) break;
                    }
                }
            }
        }

        if (transferred && this.dropContainerEntity instanceof DropBoxBlockEntity) {
            this.dropBoxHasItems = ((DropBoxBlockEntity) this.dropContainerEntity).hasItems();
        }

        return transferred;
    }
    
    private void checkConnectionStatus(Level level) {
        // Obtener el número actual de conectores
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        int currentConnectorCount = connectors.size();
        
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
            
            // Solo registrar cambios significativos en la red
            if (isBeingUsed) {

            }
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
        
        // Comenzar la búsqueda desde las posiciones adyacentes
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockState adjacentState = this.level.getBlockState(adjacentPos);
            if (adjacentState.getBlock() instanceof IndexerPipeBlock) {
                // Verificar que la tubería esté conectada en esta dirección
                if (adjacentState.getValue(IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()))) {
                    queue.add(adjacentPos);
                    visited.add(adjacentPos);
                }
            } else if (adjacentState.getBlock() instanceof IndexerConnectorBlock) {
                // Si hay un conector directamente adyacente, agregarlo
                BlockEntity blockEntity = this.level.getBlockEntity(adjacentPos);
                if (blockEntity instanceof IndexerConnectorBlockEntity) {
                    connectors.add((IndexerConnectorBlockEntity) blockEntity);
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
                        }
                    }
                }
            }
        }

        // Actualizar el cache
        connectorCache = connectors;
        return connectors;
    }
    
    // Método para verificar solo hornos sin depender del dropbox
    private void checkFurnacesOnly() {
        if (this.level == null) {
            return;
        }

        // Buscar conectores en el rango
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        if (connectors.isEmpty()) {
            return;
        }

        // Solo verificar hornos, sin dropContainer
        checkAndRefillFurnaceFuel(connectors, null);
        checkAndRefillFurnaceInput(connectors, null);
    }
    
    // Método para detectar otros controladores en la misma red
    public List<IndexerControllerBlockEntity> findOtherControllers() {
        if (this.level == null) return new ArrayList<>();
        
        List<IndexerControllerBlockEntity> otherControllers = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        
        // Comenzar la búsqueda desde las posiciones adyacentes
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockState adjacentState = this.level.getBlockState(adjacentPos);
            
            // Si hay un controlador directamente adyacente (que no sea este mismo)
            if (adjacentState.getBlock() instanceof IndexerControllerBlock && !adjacentPos.equals(this.worldPosition)) {
                BlockEntity entity = this.level.getBlockEntity(adjacentPos);
                if (entity instanceof IndexerControllerBlockEntity) {
                    otherControllers.add((IndexerControllerBlockEntity) entity);
                }
            }
            
            // Si hay una tubería adyacente, añadirla a la cola para BFS
            if (adjacentState.getBlock() instanceof IndexerPipeBlock) {
                // Verificar que la tubería esté conectada en esta dirección
                if (adjacentState.getValue(IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()))) {
                    queue.add(adjacentPos);
                    visited.add(adjacentPos);
                }
            }
        }
        
        // BFS para encontrar controladores a través de tuberías
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            BlockState currentState = this.level.getBlockState(currentPos);
            
            // Explorar en todas las direcciones
            for (Direction direction : Direction.values()) {
                BlockPos nextPos = currentPos.relative(direction);
                if (visited.contains(nextPos) || nextPos.equals(this.worldPosition)) continue;
                
                BlockState nextState = this.level.getBlockState(nextPos);
                Block nextBlock = nextState.getBlock();
                
                // Si encontramos otro controlador, agregarlo a la lista
                if (nextBlock instanceof IndexerControllerBlock) {
                    BlockEntity entity = this.level.getBlockEntity(nextPos);
                    if (entity instanceof IndexerControllerBlockEntity) {
                        otherControllers.add((IndexerControllerBlockEntity) entity);
                        visited.add(nextPos);
                    }
                }
                
                // Si encontramos otra tubería, añadirla a la cola
                if (nextBlock instanceof IndexerPipeBlock) {
                    // Verificar que la tubería esté conectada en ambas direcciones
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
        
        return otherControllers;
    }
    
    // Métodos para la nueva GUI de red
    public void forceNetworkRefresh() {
        connectorCache = null;
        uniqueContainersCache = null;
        totalAvailableSlotsCache = -1;
        totalCapacityCache = -1;
        occupiedSlotsCache = -1;
        markNetworkChanged();
        setChanged();
    }
    
    public List<ContainerNetworkInfo> getNetworkContainers() {
        List<ContainerNetworkInfo> networkContainers = new ArrayList<>();
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        
        for (IndexerConnectorBlockEntity connector : connectors) {
            BlockPos connectedPos = connector.getConnectedContainerPos();
            if (connectedPos != null && level != null) {
                BlockEntity blockEntity = level.getBlockEntity(connectedPos);
                if (blockEntity instanceof Container container) {
                    ContainerNetworkInfo info = new ContainerNetworkInfo();
                    info.position = connectedPos;
                    info.containerType = getContainerTypeName(blockEntity);
                    info.maxSlots = container.getContainerSize();
                    info.itemCount = getOccupiedSlots(container);
                    info.filters = getContainerFilters(connector);
                    info.uniqueItems = getUniqueItemsWithQuantities(container); // Agregar items únicos con cantidades
                    networkContainers.add(info);
                }
            }
        }
        
        return networkContainers;
    }
    
    private String getContainerTypeName(BlockEntity blockEntity) {
        // Usar el ResourceLocation del bloque para obtener un nombre consistente
        ResourceLocation blockId = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(blockEntity.getBlockState().getBlock());
        if (blockId != null) {
            String path = blockId.getPath();
            // Devolver los nombres en inglés para que coincidan con las claves de traducción
            switch (path) {
                case "chest":
                    return "chest";
                case "furnace":
                    return "furnace";
                case "blast_furnace":
                    return "blast_furnace";
                case "smoker":
                    return "smoker";
                case "barrel":
                    return "barrel";
                case "shulker_box":
                    return "shulker_box";
                case "hopper":
                    return "hopper";
                case "dropper":
                    return "dropper";
                case "dispenser":
                    return "dispenser";
                case "brewing_stand":
                    return "brewing_stand";
                default:
                    // Para otros contenedores, devolver el path original
                    return path;
            }
        }
        // Fallback al método anterior si no se puede obtener el ResourceLocation
        String blockName = blockEntity.getBlockState().getBlock().getName().getString();
        return blockName.substring(blockName.lastIndexOf('.') + 1);
    }
    
    private int getOccupiedSlots(Container container) {
        int occupied = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (!container.getItem(i).isEmpty()) {
                occupied++;
            }
        }
        return occupied;
    }
    
    private Map<String, Integer> getUniqueItemsWithQuantities(Container container) {
        Map<String, Integer> uniqueItems = new HashMap<>();
        
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                // Usar el ResourceLocation del item en lugar del description ID
                ResourceLocation itemLocation = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (itemLocation != null) {
                    String itemKey = itemLocation.toString();
                    uniqueItems.put(itemKey, uniqueItems.getOrDefault(itemKey, 0) + stack.getCount());
                }
            }
        }
        
        return uniqueItems;
    }
    
    private List<ItemStack> getContainerFilters(IndexerConnectorBlockEntity connector) {
        List<ItemStack> filters = new ArrayList<>();
        // Obtener filtros del conector (implementación específica del mod)
        for (int i = 0; i < connector.getContainerSize(); i++) {
            ItemStack filterItem = connector.getItem(i);
            if (!filterItem.isEmpty()) {
                filters.add(filterItem.copy());
            }
        }
        return filters;
    }
    
    // Método para abrir la GUI de red
    public void openNetworkScreen(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        net.minecraftforge.network.NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.indexer.controller.network_title");
            }
            
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new com.agustinbenitez.indexer.menu.IndexerControllerNetworkMenu(id, inventory, IndexerControllerBlockEntity.this, data);
            }
        }, pos);
    }
    
    // Clase para almacenar información de contenedores de red
    public static class ContainerNetworkInfo {
        public BlockPos position;
        public String containerType;
        public int itemCount;
        public int maxSlots;
        public List<ItemStack> filters;
        public Map<String, Integer> uniqueItems; // Nuevo campo para items únicos con cantidades

        public ContainerNetworkInfo() {
            this.filters = new ArrayList<>();
            this.uniqueItems = new HashMap<>();
        }
    }
}