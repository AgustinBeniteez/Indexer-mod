package com.agustinbenitez.indexer.block.entity;

import com.agustinbenitez.indexer.init.ModBlockEntities;
import com.agustinbenitez.indexer.block.IndexerControllerBlock;
import com.agustinbenitez.indexer.block.IndexerPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashSet;
import java.util.Set;
import java.util.Queue;
import java.util.LinkedList;

public class ExtractorBlockEntity extends BlockEntity {
    private BlockPos connectedContainerPos = null;
    private int extractionTimer = 0;
    private static final int DEFAULT_EXTRACTION_INTERVAL = 40; // 2 segundos (40 ticks) sin mejoras
    private static final int ITEMS_PER_EXTRACTION = 1; // Extraer 1 item por vez

    public ExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTRACTOR.get(), pos, state);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("ContainerX") && tag.contains("ContainerY") && tag.contains("ContainerZ")) {
            this.connectedContainerPos = new BlockPos(
                    tag.getInt("ContainerX"),
                    tag.getInt("ContainerY"),
                    tag.getInt("ContainerZ")
            );
        }
        this.extractionTimer = tag.getInt("ExtractionTimer");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.connectedContainerPos != null) {
            tag.putInt("ContainerX", this.connectedContainerPos.getX());
            tag.putInt("ContainerY", this.connectedContainerPos.getY());
            tag.putInt("ContainerZ", this.connectedContainerPos.getZ());
        }
        tag.putInt("ExtractionTimer", this.extractionTimer);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExtractorBlockEntity entity) {
        if (level.isClientSide()) return;

        // Actualizar conexión de contenedor
        entity.updateConnectedContainer();
        
        // Incrementar timer de extracción
        entity.extractionTimer++;
        
        // Intentar extraer items cada cierto intervalo (dinámico basado en mejoras del controller)
        int extractionInterval = entity.getExtractionInterval(level, pos);
        if (entity.extractionTimer >= extractionInterval) {
            entity.extractionTimer = 0;
            entity.attemptExtraction(level, pos);
        }
    }

    private void updateConnectedContainer() {
        if (this.level == null) return;

        BlockPos oldContainerPos = this.connectedContainerPos;
        this.connectedContainerPos = null;
        
        // Buscar contenedores arriba y abajo únicamente
        Direction[] directions = {Direction.UP, Direction.DOWN};
        
        for (Direction direction : directions) {
            BlockPos adjacentPos = this.worldPosition.relative(direction);
            BlockEntity adjacentEntity = this.level.getBlockEntity(adjacentPos);
            
            // Verificar si es un contenedor válido (excluir otros extractores y conectores)
            if (adjacentEntity instanceof Container && 
                !adjacentEntity.getClass().getName().contains("ExtractorBlockEntity") &&
                !adjacentEntity.getClass().getName().contains("IndexerConnectorBlockEntity")) {
                this.connectedContainerPos = adjacentPos;
                this.setChanged();
                return;
            }
        }
        
        // Si se perdió la conexión, marcar como cambiado
        if (oldContainerPos != null && this.connectedContainerPos == null) {
            this.setChanged();
        }
    }

    private void attemptExtraction(Level level, BlockPos extractorPos) {
        if (this.connectedContainerPos == null || this.level == null) {
            return;
        }
        
        // Verificar si está conectado a la red de tuberías antes de extraer
        if (!isConnectedToController(level, extractorPos)) {
            return; // No extraer si no está conectado a la red
        }
        
        // Obtener el controlador conectado para determinar la cantidad de ítems a extraer
        IndexerControllerBlockEntity controller = findConnectedController(level, extractorPos);
        if (controller == null) {
            return; // No hay controlador conectado
        }
        
        // Obtener la cantidad de ítems a extraer según la mejora aplicada al controlador
        int itemsToExtract = controller.getItemsPerTransfer();
        
        BlockEntity containerEntity = this.level.getBlockEntity(this.connectedContainerPos);
        if (!(containerEntity instanceof Container)) {
            this.connectedContainerPos = null;
            return;
        }
        
        Container container = (Container) containerEntity;
        
        // Verificar si es un horno (cualquier tipo de horno)
        boolean isFurnace = containerEntity.getClass().getName().contains("FurnaceBlockEntity") ||
                           containerEntity.getClass().getName().contains("BlastFurnaceBlockEntity") ||
                           containerEntity.getClass().getName().contains("SmokerBlockEntity");
        
        if (isFurnace) {
            // Para hornos, solo extraer del slot de resultado (slot 2)
            final int FURNACE_RESULT_SLOT = 2;
            
            if (FURNACE_RESULT_SLOT < container.getContainerSize()) {
                ItemStack stackInSlot = container.getItem(FURNACE_RESULT_SLOT);
                
                if (!stackInSlot.isEmpty()) {
                    // Extraer la cantidad de ítems según la mejora aplicada
                    ItemStack extractedStack = stackInSlot.copy();
                    extractedStack.setCount(Math.min(itemsToExtract, stackInSlot.getCount()));
                    
                    // Remover los ítems del contenedor
                    stackInSlot.shrink(extractedStack.getCount());
                    container.setItem(FURNACE_RESULT_SLOT, stackInSlot);
                    
                    // Intentar enviar los ítems al sistema de tuberías
                    if (sendItemToPipeSystem(extractedStack, level, extractorPos)) {
                        // Ítems enviados exitosamente
                        this.setChanged();
                        return;
                    } else {
                        // Si no se pudieron enviar, devolver los ítems al contenedor
                        ItemStack remainingStack = container.getItem(FURNACE_RESULT_SLOT);
                        if (remainingStack.isEmpty()) {
                            container.setItem(FURNACE_RESULT_SLOT, extractedStack);
                        } else if (remainingStack.getItem() == extractedStack.getItem() && 
                                  remainingStack.getCount() + extractedStack.getCount() <= remainingStack.getMaxStackSize()) {
                            remainingStack.grow(extractedStack.getCount());
                            container.setItem(FURNACE_RESULT_SLOT, remainingStack);
                        }
                    }
                }
            }
        } else {
            // Para otros contenedores, buscar el primer item no vacío
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stackInSlot = container.getItem(slot);
                
                if (!stackInSlot.isEmpty()) {
                    // Extraer la cantidad de ítems según la mejora aplicada
                    ItemStack extractedStack = stackInSlot.copy();
                    extractedStack.setCount(Math.min(itemsToExtract, stackInSlot.getCount()));
                    
                    // Remover los ítems del contenedor
                    stackInSlot.shrink(extractedStack.getCount());
                    container.setItem(slot, stackInSlot);
                    
                    // Intentar enviar los ítems al sistema de tuberías
                    if (sendItemToPipeSystem(extractedStack, level, extractorPos)) {
                        // Ítems enviados exitosamente
                        this.setChanged();
                        return;
                    } else {
                        // Si no se pudieron enviar, devolver los ítems al contenedor
                        ItemStack remainingStack = container.getItem(slot);
                        if (remainingStack.isEmpty()) {
                            container.setItem(slot, extractedStack);
                        } else if (remainingStack.getItem() == extractedStack.getItem() && 
                                  remainingStack.getCount() + extractedStack.getCount() <= remainingStack.getMaxStackSize()) {
                            remainingStack.grow(extractedStack.getCount());
                            container.setItem(slot, remainingStack);
                        }
                    }
                    return;
                }
            }
        }
    }

    private boolean sendItemToPipeSystem(ItemStack stack, Level level, BlockPos extractorPos) {
        // Buscar conectores de indexer cercanos para enviar el item
        int searchRadius = 16;
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = extractorPos.offset(x, y, z);
                    BlockEntity entity = level.getBlockEntity(checkPos);
                    
                    if (entity instanceof IndexerConnectorBlockEntity) {
                        IndexerConnectorBlockEntity connector = (IndexerConnectorBlockEntity) entity;
                        
                        // Intentar insertar el item en el conector
                        ItemStack remainder = connector.insertItem(stack);
                        
                        if (remainder.isEmpty() || remainder.getCount() < stack.getCount()) {
                            // Se insertó al menos parte del item
                            return true;
                        }
                    }
                }
            }
        }
        
        return false; // No se pudo enviar el item
    }

    public boolean hasConnectedContainer() {
        return this.connectedContainerPos != null;
    }

    public BlockPos getConnectedContainerPos() {
        return this.connectedContainerPos;
    }
    
    // Método para obtener el intervalo de extracción basado en las mejoras del controller conectado
    private int getExtractionInterval(Level level, BlockPos pos) {
        IndexerControllerBlockEntity controller = findConnectedController(level, pos);
        if (controller != null) {
            int itemsPerTransfer = controller.getItemsPerTransfer();
            // Usar la misma velocidad que la mejora aplicada en el controller
            // Sin mejora (1 item): 40 ticks
            // Mejora básica (5 items): 5 ticks  
            // Mejora avanzada (20 items): 20 ticks
            // Mejora elite (64 items): 64 ticks
            if (itemsPerTransfer > 1) {
                return itemsPerTransfer; // Usar la misma velocidad que la mejora
            }
        }
        return DEFAULT_EXTRACTION_INTERVAL; // Sin mejoras o sin controller
    }
    
    // Método para encontrar el controller conectado a través de tuberías
    private IndexerControllerBlockEntity findConnectedController(Level level, BlockPos pos) {
        if (level == null) return null;
        
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        
        // Comenzar la búsqueda desde las posiciones adyacentes
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            
            // Si hay un controlador directamente adyacente
            if (adjacentState.getBlock() instanceof IndexerControllerBlock) {
                BlockEntity entity = level.getBlockEntity(adjacentPos);
                if (entity instanceof IndexerControllerBlockEntity) {
                    return (IndexerControllerBlockEntity) entity;
                }
            }
            
            // Si hay una tubería adyacente, añadirla a la cola para BFS
            if (adjacentState.getBlock() instanceof IndexerPipeBlock) {
                queue.add(adjacentPos);
                visited.add(adjacentPos);
            }
        }
        
        // BFS para encontrar un controlador a través de tuberías
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            BlockState currentState = level.getBlockState(currentPos);
            
            // Explorar en todas las direcciones
            for (Direction direction : Direction.values()) {
                BlockPos nextPos = currentPos.relative(direction);
                if (visited.contains(nextPos)) continue;
                
                BlockState nextState = level.getBlockState(nextPos);
                Block nextBlock = nextState.getBlock();
                
                // Si encontramos un controlador, retornarlo
                if (nextBlock instanceof IndexerControllerBlock) {
                    BlockEntity entity = level.getBlockEntity(nextPos);
                    if (entity instanceof IndexerControllerBlockEntity) {
                        return (IndexerControllerBlockEntity) entity;
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
        
        return null;
    }
    
    // Método para verificar si el extractor está conectado a un controlador a través de tuberías
    private static boolean isConnectedToController(Level level, BlockPos pos) {
        if (level == null) return false;
        
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        
        // Comenzar la búsqueda desde las posiciones adyacentes
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            
            // Si hay un controlador directamente adyacente
            if (adjacentState.getBlock() instanceof IndexerControllerBlock) {
                return true;
            }
            
            // Si hay una tubería adyacente, añadirla a la cola para BFS
            if (adjacentState.getBlock() instanceof IndexerPipeBlock) {
                queue.add(adjacentPos);
                visited.add(adjacentPos);
            }
        }
        
        // BFS para encontrar un controlador a través de tuberías
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            BlockState currentState = level.getBlockState(currentPos);
            
            // Explorar en todas las direcciones
            for (Direction direction : Direction.values()) {
                BlockPos nextPos = currentPos.relative(direction);
                if (visited.contains(nextPos)) continue;
                
                BlockState nextState = level.getBlockState(nextPos);
                Block nextBlock = nextState.getBlock();
                
                // Si encontramos un controlador, retornar true
                if (nextBlock instanceof IndexerControllerBlock) {
                    return true;
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
        
        return false;
    }
}