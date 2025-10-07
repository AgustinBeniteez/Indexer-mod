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
    private int extractionCooldown = 0;
    private static final int EXTRACTION_COOLDOWN_MAX = 8; // Mismo cooldown que el IndexerController

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
        this.extractionCooldown = tag.getInt("ExtractionCooldown");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.connectedContainerPos != null) {
            tag.putInt("ContainerX", this.connectedContainerPos.getX());
            tag.putInt("ContainerY", this.connectedContainerPos.getY());
            tag.putInt("ContainerZ", this.connectedContainerPos.getZ());
        }
        tag.putInt("ExtractionCooldown", this.extractionCooldown);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExtractorBlockEntity entity) {
        if (level.isClientSide()) return;

        // Actualizar conexión de contenedor
        entity.updateConnectedContainer();
        
        // Manejar cooldown de extracción
        if (entity.extractionCooldown > 0) {
            entity.extractionCooldown--;
            return;
        }
        
        // Intentar extraer items
        boolean didExtract = entity.attemptExtraction(level, pos);
        
        // Si se extrajo algo, aplicar cooldown
        if (didExtract) {
            entity.extractionCooldown = EXTRACTION_COOLDOWN_MAX;
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
            
            // Verificar si es un contenedor válido
            if (isValidContainer(adjacentEntity)) {
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
    
    /**
     * Verifica si una BlockEntity es un contenedor válido para el extractor.
     * Incluye contenedores de Minecraft vanilla, hornos, y contenedores de otros mods.
     */
    private boolean isValidContainer(BlockEntity entity) {
        if (entity == null) return false;
        
        // Excluir nuestros propios bloques del mod
        String className = entity.getClass().getName();
        if (className.contains("ExtractorBlockEntity") || 
            className.contains("IndexerConnectorBlockEntity") ||
            className.contains("DropBoxBlockEntity")) {
            return false;
        }
        
        // Verificar si implementa Container (interfaz estándar de Minecraft)
        if (entity instanceof Container) {
            return true;
        }
        
        // Verificar contenedores de otros mods que podrían no implementar Container directamente
        // pero tienen métodos de inventario comunes
        try {
            // Intentar acceder a métodos comunes de inventario usando reflexión
            if (entity.getClass().getMethod("getContainerSize") != null ||
                entity.getClass().getMethod("getSlots") != null ||
                entity.getClass().getMethod("getInventory") != null) {
                return true;
            }
        } catch (NoSuchMethodException | SecurityException e) {
            // Método no encontrado, continuar con otras verificaciones
        }
        
        // Verificar por nombres de clase comunes de mods populares
        if (className.contains("BackpackBlockEntity") ||
            className.contains("ChestBlockEntity") ||
            className.contains("BarrelBlockEntity") ||
            className.contains("ShulkerBoxBlockEntity") ||
            className.contains("StorageBlockEntity") ||
            className.contains("InventoryBlockEntity") ||
            className.toLowerCase().contains("storage") ||
            className.toLowerCase().contains("chest") ||
            className.toLowerCase().contains("container") ||
            className.toLowerCase().contains("inventory") ||
            className.toLowerCase().contains("backpack") ||
            className.toLowerCase().contains("bag")) {
            return true;
        }
        
        return false;
    }

    private boolean attemptExtraction(Level level, BlockPos extractorPos) {
        if (this.connectedContainerPos == null || this.level == null) {
            return false;
        }
        
        // Verificar si está conectado a la red de tuberías antes de extraer
        if (!isConnectedToController(level, extractorPos)) {
            return false; // No extraer si no está conectado a la red
        }
        
        // Obtener el controlador conectado para determinar la cantidad de ítems a extraer
        IndexerControllerBlockEntity controller = findConnectedController(level, extractorPos);
        if (controller == null) {
            return false; // No hay controlador conectado
        }
        
        // Obtener la cantidad de ítems a extraer según la mejora aplicada al controlador
        int itemsToExtract = controller.getItemsPerTransfer();
        
        // Debug log para verificar el valor
        System.out.println("DEBUG EXTRACTOR: itemsToExtract = " + itemsToExtract + " en posición " + extractorPos);
        
        BlockEntity containerEntity = this.level.getBlockEntity(this.connectedContainerPos);
        if (containerEntity == null) {
            this.connectedContainerPos = null;
            return false;
        }
        
        // Intentar extraer usando diferentes métodos según el tipo de contenedor
        boolean extractionSuccessful = false;
        
        // Primero intentar con la interfaz Container estándar
        if (containerEntity instanceof Container) {
            extractionSuccessful = extractFromContainer((Container) containerEntity, itemsToExtract, level, extractorPos);
        } else {
            // Intentar extraer de contenedores de otros mods usando reflexión
            extractionSuccessful = extractFromModContainer(containerEntity, itemsToExtract, level, extractorPos);
        }
        
        if (extractionSuccessful) {
            this.setChanged();
        }
        
        return extractionSuccessful;
    }
    
    /**
     * Extrae items de un contenedor estándar que implementa la interfaz Container.
     */
    private boolean extractFromContainer(Container container, int itemsToExtract, Level level, BlockPos extractorPos) {
        // Verificar si es un horno (cualquier tipo de horno)
        boolean isFurnace = container.getClass().getName().contains("FurnaceBlockEntity") ||
                           container.getClass().getName().contains("BlastFurnaceBlockEntity") ||
                           container.getClass().getName().contains("SmokerBlockEntity");
        
        if (isFurnace) {
            return extractFromFurnace(container, itemsToExtract, level, extractorPos);
        } else {
            return extractFromGenericContainer(container, itemsToExtract, level, extractorPos);
        }
    }
    
    /**
     * Extrae items de un horno (resultado y buckets vacíos del combustible).
     */
    private boolean extractFromFurnace(Container container, int itemsToExtract, Level level, BlockPos extractorPos) {
        final int FURNACE_RESULT_SLOT = 2;
        final int FURNACE_FUEL_SLOT = 1;
        
        // Primero intentar extraer buckets vacíos del slot de combustible
        if (FURNACE_FUEL_SLOT < container.getContainerSize()) {
            ItemStack fuelSlotStack = container.getItem(FURNACE_FUEL_SLOT);
            
            if (!fuelSlotStack.isEmpty() && fuelSlotStack.getItem().getDescriptionId().equals("item.minecraft.bucket")) {
                // Verificar si hay espacio disponible para buckets vacíos antes de extraer
                ItemStack testStack = fuelSlotStack.copy();
                testStack.setCount(Math.min(itemsToExtract, fuelSlotStack.getCount()));
                
                if (canSendBucketToPipeSystem(testStack, level, extractorPos)) {
                    // Hay espacio disponible, proceder con la extracción
                    ItemStack extractedStack = fuelSlotStack.copy();
                    extractedStack.setCount(Math.min(itemsToExtract, fuelSlotStack.getCount()));
                    
                    // Debug log para buckets
                    System.out.println("DEBUG EXTRACTOR: Extrayendo " + extractedStack.getCount() + " buckets del slot de combustible (máximo permitido: " + itemsToExtract + ")");
                    
                    // Remover los buckets del contenedor
                    fuelSlotStack.shrink(extractedStack.getCount());
                    container.setItem(FURNACE_FUEL_SLOT, fuelSlotStack);
                    
                    // Intentar enviar los buckets al sistema de tuberías
                    if (sendItemToPipeSystem(extractedStack, level, extractorPos)) {
                        return true;
                    } else {
                        // Si no se pudieron enviar, devolver los buckets al contenedor
                        ItemStack remainingStack = container.getItem(FURNACE_FUEL_SLOT);
                        if (remainingStack.isEmpty()) {
                            container.setItem(FURNACE_FUEL_SLOT, extractedStack);
                        } else if (remainingStack.getItem() == extractedStack.getItem() && 
                                  remainingStack.getCount() + extractedStack.getCount() <= remainingStack.getMaxStackSize()) {
                            remainingStack.grow(extractedStack.getCount());
                            container.setItem(FURNACE_FUEL_SLOT, remainingStack);
                        }
                    }
                }
            }
        }
        
        // Luego intentar extraer del slot de resultado
        if (FURNACE_RESULT_SLOT < container.getContainerSize()) {
            ItemStack stackInSlot = container.getItem(FURNACE_RESULT_SLOT);
            
            if (!stackInSlot.isEmpty()) {
                ItemStack extractedStack = stackInSlot.copy();
                extractedStack.setCount(Math.min(itemsToExtract, stackInSlot.getCount()));
                
                // Debug log para resultado del horno
                System.out.println("DEBUG EXTRACTOR: Extrayendo " + extractedStack.getCount() + " items del slot de resultado del horno (máximo permitido: " + itemsToExtract + ")");
                
                stackInSlot.shrink(extractedStack.getCount());
                container.setItem(FURNACE_RESULT_SLOT, stackInSlot);
                
                if (sendItemToPipeSystem(extractedStack, level, extractorPos)) {
                    return true;
                } else {
                    // Devolver los ítems si no se pudieron enviar
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
        
        return false;
    }
    
    /**
     * Extrae items de un contenedor genérico (cofres, barriles, etc.).
     */
    private boolean extractFromGenericContainer(Container container, int itemsToExtract, Level level, BlockPos extractorPos) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stackInSlot = container.getItem(slot);
            
            if (!stackInSlot.isEmpty()) {
                ItemStack extractedStack = stackInSlot.copy();
                extractedStack.setCount(Math.min(itemsToExtract, stackInSlot.getCount()));
                
                // Debug log para verificar la extracción
                System.out.println("DEBUG EXTRACTOR: Extrayendo " + extractedStack.getCount() + " items de " + extractedStack.getItem().getDescriptionId() + " (máximo permitido: " + itemsToExtract + ")");
                
                stackInSlot.shrink(extractedStack.getCount());
                container.setItem(slot, stackInSlot);
                
                if (sendItemToPipeSystem(extractedStack, level, extractorPos)) {
                    return true;
                } else {
                    // Devolver los ítems si no se pudieron enviar
                    ItemStack remainingStack = container.getItem(slot);
                    if (remainingStack.isEmpty()) {
                        container.setItem(slot, extractedStack);
                    } else if (remainingStack.getItem() == extractedStack.getItem() && 
                              remainingStack.getCount() + extractedStack.getCount() <= remainingStack.getMaxStackSize()) {
                        remainingStack.grow(extractedStack.getCount());
                        container.setItem(slot, remainingStack);
                    }
                }
                return false;
            }
        }
        return false;
    }
    
    /**
     * Intenta extraer items de contenedores de otros mods usando reflexión.
     */
    private boolean extractFromModContainer(BlockEntity containerEntity, int itemsToExtract, Level level, BlockPos extractorPos) {
        try {
            // Intentar obtener el inventario usando métodos comunes
            Object inventory = null;
            int containerSize = 0;
            
            // Intentar diferentes métodos para obtener el inventario
            try {
                inventory = containerEntity.getClass().getMethod("getInventory").invoke(containerEntity);
                containerSize = (Integer) inventory.getClass().getMethod("getSlots").invoke(inventory);
            } catch (Exception e1) {
                try {
                    containerSize = (Integer) containerEntity.getClass().getMethod("getContainerSize").invoke(containerEntity);
                    inventory = containerEntity; // El contenedor mismo implementa los métodos
                } catch (Exception e2) {
                    try {
                        containerSize = (Integer) containerEntity.getClass().getMethod("getSlots").invoke(containerEntity);
                        inventory = containerEntity;
                    } catch (Exception e3) {
                        return false; // No se pudo acceder al inventario
                    }
                }
            }
            
            if (inventory == null || containerSize <= 0) {
                return false;
            }
            
            // Intentar extraer del primer slot no vacío
            for (int slot = 0; slot < containerSize; slot++) {
                try {
                    ItemStack stackInSlot = null;
                    
                    // Intentar diferentes métodos para obtener el item
                    try {
                        stackInSlot = (ItemStack) inventory.getClass().getMethod("getStackInSlot", int.class).invoke(inventory, slot);
                    } catch (Exception e1) {
                        try {
                            stackInSlot = (ItemStack) inventory.getClass().getMethod("getItem", int.class).invoke(inventory, slot);
                        } catch (Exception e2) {
                            continue; // No se pudo obtener el item de este slot
                        }
                    }
                    
                    if (stackInSlot != null && !stackInSlot.isEmpty()) {
                        ItemStack extractedStack = stackInSlot.copy();
                        extractedStack.setCount(Math.min(itemsToExtract, stackInSlot.getCount()));
                        
                        // Intentar extraer el item
                        try {
                            ItemStack extracted = (ItemStack) inventory.getClass().getMethod("extractItem", int.class, int.class, boolean.class)
                                    .invoke(inventory, slot, extractedStack.getCount(), false);
                            
                            if (!extracted.isEmpty() && sendItemToPipeSystem(extracted, level, extractorPos)) {
                                return true;
                            }
                        } catch (Exception e) {
                            // Si no tiene extractItem, intentar con setItem
                            try {
                                stackInSlot.shrink(extractedStack.getCount());
                                inventory.getClass().getMethod("setStackInSlot", int.class, ItemStack.class)
                                        .invoke(inventory, slot, stackInSlot);
                                
                                if (sendItemToPipeSystem(extractedStack, level, extractorPos)) {
                                    return true;
                                } else {
                                    // Devolver el item si no se pudo enviar
                                    stackInSlot.grow(extractedStack.getCount());
                                    inventory.getClass().getMethod("setStackInSlot", int.class, ItemStack.class)
                                            .invoke(inventory, slot, stackInSlot);
                                }
                            } catch (Exception e2) {
                                // Intentar con setItem si setStackInSlot no existe
                                try {
                                    inventory.getClass().getMethod("setItem", int.class, ItemStack.class)
                                            .invoke(inventory, slot, stackInSlot);
                                    
                                    if (sendItemToPipeSystem(extractedStack, level, extractorPos)) {
                                        return true;
                                    }
                                } catch (Exception e3) {
                                    // No se pudo modificar el inventario
                                }
                            }
                        }
                        return false;
                    }
                } catch (Exception e) {
                    // Error al procesar este slot, continuar con el siguiente
                    continue;
                }
            }
        } catch (Exception e) {
            // Error general al acceder al contenedor del mod
            return false;
        }
        
        return false;
    }

    private boolean sendItemToPipeSystem(ItemStack stack, Level level, BlockPos extractorPos) {
        // Buscar conectores de indexer cercanos para enviar el item
        int searchRadius = 16;
        
        // Primero buscar conectores con filtro específico para este item
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = extractorPos.offset(x, y, z);
                    BlockEntity entity = level.getBlockEntity(checkPos);
                    
                    if (entity instanceof IndexerConnectorBlockEntity) {
                        IndexerConnectorBlockEntity connector = (IndexerConnectorBlockEntity) entity;
                        
                        // Verificar si el conector tiene un filtro específico para este ítem
                        ItemStack filterItem = connector.getFilterItem(0);
                        if (!filterItem.isEmpty() && filterItem.getItem() == stack.getItem()) {
                            // Intentar insertar el item en el conector con filtro específico
                            ItemStack remainder = connector.insertItem(stack);
                            
                            if (remainder.isEmpty() || remainder.getCount() < stack.getCount()) {
                                // Se insertó al menos parte del item
                                return true;
                            }
                        }
                    }
                }
            }
        }
        
        // Si no se encontró un conector con filtro específico, buscar conectores sin filtro
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = extractorPos.offset(x, y, z);
                    BlockEntity entity = level.getBlockEntity(checkPos);
                    
                    if (entity instanceof IndexerConnectorBlockEntity) {
                        IndexerConnectorBlockEntity connector = (IndexerConnectorBlockEntity) entity;
                        
                        // Verificar si el conector NO tiene filtro específico para este ítem
                        ItemStack filterItem = connector.getFilterItem(0);
                        if (filterItem.isEmpty() || filterItem.getItem() != stack.getItem()) {
                            // Intentar insertar el item en el conector sin filtro específico
                            ItemStack remainder = connector.insertItem(stack);
                            
                            if (remainder.isEmpty() || remainder.getCount() < stack.getCount()) {
                                // Se insertó al menos parte del item
                                return true;
                            }
                        }
                    }
                }
            }
        }
        
        return false; // No se pudo enviar el item
    }
    
    private boolean canSendBucketToPipeSystem(ItemStack stack, Level level, BlockPos extractorPos) {
        // Buscar conectores de indexer cercanos para verificar si hay espacio para buckets
        int searchRadius = 16;
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = extractorPos.offset(x, y, z);
                    BlockEntity entity = level.getBlockEntity(checkPos);
                    
                    if (entity instanceof IndexerConnectorBlockEntity) {
                        IndexerConnectorBlockEntity connector = (IndexerConnectorBlockEntity) entity;
                        
                        // Verificar si hay espacio disponible para buckets vacíos
                        if (connector.canAcceptBuckets()) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false; // No hay espacio disponible para buckets
    }

    public boolean hasConnectedContainer() {
        return this.connectedContainerPos != null;
    }

    public BlockPos getConnectedContainerPos() {
        return this.connectedContainerPos;
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