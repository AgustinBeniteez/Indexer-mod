package com.agustinbenitez.indexer.block.entity;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.DropBoxBlock;
import com.agustinbenitez.indexer.block.IndexerConnectorBlock;
import com.agustinbenitez.indexer.block.IndexerPipeBlock;
import com.agustinbenitez.indexer.init.ModBlockEntities;
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
    private static final int SEARCH_RANGE = 250; // Aumentado de 10 a 50 para permitir más conectores
    
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
                return 4;
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
    
    public boolean hasDropContainer() {
        if (this.dropContainerPos == null) {
            updateDropContainer();
        }
        return this.dropContainerPos != null;
    }
    
    public void updateDropContainer() {
        if (this.level == null) return;
        
        this.dropContainerPos = null;
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockState adjacentState = this.level.getBlockState(adjacentPos);
            BlockEntity blockEntity = this.level.getBlockEntity(adjacentPos);
            // Detectar cualquier tipo de contenedor
            if (blockEntity instanceof Container) {
                this.dropContainerPos = adjacentPos;
                this.setChanged();
                return;
            }
        }
    }
    
    public void setDropContainerPos(BlockPos pos) {
        this.dropContainerPos = pos;
        this.setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IndexerControllerBlockEntity entity) {
        if (level.isClientSide()) return;

        // Verificar conexiones y notificar cambios
        entity.checkConnectionStatus(level);

        if (!entity.enabled) return;

        if (entity.transferCooldown > 0) {
            entity.transferCooldown--;
            return;
        }

        // Intentar transferir ítems desde el contenedor de drop
        boolean didTransfer = entity.transferItemsFromDropContainer();

        if (didTransfer) {
            entity.transferCooldown = TRANSFER_COOLDOWN_MAX;
            entity.setChanged();
        }
    }

    private boolean transferItemsFromDropContainer() {
        if (this.level == null || !hasDropContainer()) {
            return false;
        }

        BlockEntity dropContainerEntity = this.level.getBlockEntity(this.dropContainerPos);
        if (!(dropContainerEntity instanceof Container dropContainer)) {
            return false;
        }

        // Buscar conectores en el rango
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        if (connectors.isEmpty()) {
            IndexerMod.LOGGER.info("No connectors found for controller at " + worldPosition);
            return false;
        }

        IndexerMod.LOGGER.info("Found " + connectors.size() + " connectors for controller at " + worldPosition);
        boolean transferred = false;

        // Intentar transferir cada ítem del contenedor de drop a un conector apropiado
        for (int i = 0; i < dropContainer.getContainerSize(); i++) {
            ItemStack stack = dropContainer.getItem(i);
            if (stack.isEmpty()) continue;

            IndexerMod.LOGGER.info("Trying to transfer item: " + stack.getItem().getDescriptionId() + " x" + stack.getCount());
            
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
            
            IndexerMod.LOGGER.info("  Found " + connectorsWithFilter.size() + " connectors with specific filter and " + 
                                 connectorsWithoutFilter.size() + " connectors without filter for this item");
            
            // Primero intentar con conectores que tienen filtro específico
            boolean itemTransferred = false;
            ItemStack remainder = stack.copy();
            
            // Intentar primero con los conectores que tienen filtro específico
            for (IndexerConnectorBlockEntity connector : connectorsWithFilter) {
                IndexerMod.LOGGER.info("  Trying connector with filter at " + connector.getBlockPos());
                remainder = connector.insertItem(remainder);
                
                if (remainder.getCount() < stack.getCount()) {
                    IndexerMod.LOGGER.info("  Transferred " + (stack.getCount() - remainder.getCount()) + " items to filtered connector");
                    dropContainer.setItem(i, remainder);
                    transferred = true;
                    itemTransferred = true;
                    
                    if (remainder.isEmpty()) {
                        break;
                    }
                }
            }
            
            // Si todavía quedan ítems, intentar con los conectores sin filtro
            if (!remainder.isEmpty() && !connectorsWithoutFilter.isEmpty()) {
                for (IndexerConnectorBlockEntity connector : connectorsWithoutFilter) {
                    IndexerMod.LOGGER.info("  Trying connector without filter at " + connector.getBlockPos());
                    ItemStack newRemainder = connector.insertItem(remainder);
                    
                    if (newRemainder.getCount() < remainder.getCount()) {
                        IndexerMod.LOGGER.info("  Transferred " + (remainder.getCount() - newRemainder.getCount()) + " items to non-filtered connector");
                        dropContainer.setItem(i, newRemainder);
                        transferred = true;
                        itemTransferred = true;
                        remainder = newRemainder;
                        
                        if (remainder.isEmpty()) {
                            break;
                        }
                    }
                }
            }
            
            // Notificar a los jugadores cercanos sobre la transferencia
            if (itemTransferred) {
                List<Player> nearbyPlayers = level.getEntitiesOfClass(
                    Player.class, 
                    new net.minecraft.world.phys.AABB(worldPosition).inflate(32.0D)
                );
                
                for (Player player : nearbyPlayers) {
                    player.sendSystemMessage(Component.literal("Transferencia de items completada"));
                }
            }
        }

        return transferred;
    }
    
    public int getConnectedContainersCount() {
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        Set<BlockPos> uniqueContainers = new HashSet<>();
        
        for (IndexerConnectorBlockEntity connector : connectors) {
            BlockPos containerPos = connector.getConnectedContainerPos();
            if (containerPos != null) {
                uniqueContainers.add(containerPos);
            }
        }
        
        return uniqueContainers.size();
    }
    
    public int getTotalAvailableSlots() {
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
        
        return totalSlots;
    }

    private void checkConnectionStatus(Level level) {
        // Obtener el número actual de conectores
        List<IndexerConnectorBlockEntity> connectors = findConnectors();
        int currentConnectorCount = connectors.size();
        
        // Imprimir información de depuración en el registro del servidor
        IndexerMod.LOGGER.info("Controller at " + worldPosition + " found " + currentConnectorCount + " connectors");
        for (IndexerConnectorBlockEntity connector : connectors) {
            IndexerMod.LOGGER.info("  - Connector at " + connector.getBlockPos() + ", connected container: " + connector.getConnectedContainerPos());
        }
        
        // Verificar si hay cambios en las conexiones
        if (currentConnectorCount != previousConnectorCount) {
            // Buscar jugadores cercanos para notificar
            List<Player> nearbyPlayers = level.getEntitiesOfClass(
                Player.class, 
                new net.minecraft.world.phys.AABB(worldPosition).inflate(32.0D)
            );
            
            for (Player player : nearbyPlayers) {
                if (currentConnectorCount > previousConnectorCount) {
                    // Se agregaron nuevos conectores
                    int newConnectors = currentConnectorCount - previousConnectorCount;
                    for (int i = 0; i < newConnectors; i++) {
                        player.sendSystemMessage(Component.literal("Conector activado"));
                    }
                    hasNotifiedConnection = true;
                } else if (currentConnectorCount == 0 && hasNotifiedConnection) {
                    // Notificar conexión perdida
                    player.sendSystemMessage(Component.translatable("message.indexer.controller.disconnected"));
                    hasNotifiedConnection = false;
                } else if (currentConnectorCount < previousConnectorCount) {
                    // Se perdieron conectores
                    int lostConnectors = previousConnectorCount - currentConnectorCount;
                    player.sendSystemMessage(Component.translatable("message.indexer.controller.connection_lost", lostConnectors));
                    if (currentConnectorCount == 0) {
                        hasNotifiedConnection = false;
                    }
                }
            }
            
            // Actualizar el contador previo
            previousConnectorCount = currentConnectorCount;
            setChanged();
        }
    }
    
    private List<IndexerConnectorBlockEntity> findConnectors() {
        List<IndexerConnectorBlockEntity> connectors = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        
        IndexerMod.LOGGER.info("Starting connector search from controller at " + worldPosition);

        // Comenzar la búsqueda desde las posiciones adyacentes
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockState adjacentState = this.level.getBlockState(adjacentPos);
            if (adjacentState.getBlock() instanceof IndexerPipeBlock) {
                // Verificar que la tubería esté conectada en esta dirección
                if (adjacentState.getValue(IndexerPipeBlock.getPropertyForDirection(direction.getOpposite()))) {
                    queue.add(adjacentPos);
                    visited.add(adjacentPos);
                    IndexerMod.LOGGER.info("Pipe connected in direction " + direction + " at " + adjacentPos);
                } else {
                    IndexerMod.LOGGER.info("Pipe NOT connected in direction " + direction + " at " + adjacentPos);
                }
            } else if (adjacentState.getBlock() instanceof IndexerConnectorBlock) {
                // Si hay un conector directamente adyacente, agregarlo
                BlockEntity blockEntity = this.level.getBlockEntity(adjacentPos);
                if (blockEntity instanceof IndexerConnectorBlockEntity) {
                    connectors.add((IndexerConnectorBlockEntity) blockEntity);
                    IndexerMod.LOGGER.info("Found adjacent connector at " + adjacentPos);
                }
            }
        }

        // BFS para encontrar conectores a través de tuberías
        // Eliminamos la limitación de SEARCH_RANGE para permitir encontrar todos los conectores
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            BlockState currentState = this.level.getBlockState(currentPos);
            BlockEntity blockEntity = this.level.getBlockEntity(currentPos);

            if (blockEntity instanceof IndexerConnectorBlockEntity) {
                connectors.add((IndexerConnectorBlockEntity) blockEntity);
                IndexerMod.LOGGER.info("Found connector through pipes at " + currentPos);
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
                        IndexerMod.LOGGER.info("Following pipe connection from " + currentPos + " to " + nextPos);
                    } else {
                        IndexerMod.LOGGER.info("Pipe connection broken between " + currentPos + " and " + nextPos);
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
                            IndexerMod.LOGGER.info("Found connector at end of pipe at " + nextPos);
                        }
                    }
                }
            }
        }

        IndexerMod.LOGGER.info("Connector search completed. Found " + connectors.size() + " connectors. Visited " + visited.size() + " blocks.");
        return connectors;
    }


}