package com.agustinbenitez.indexer.block;

import com.agustinbenitez.indexer.block.entity.IndexerConnectorBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.init.ModBlockEntities;
import com.agustinbenitez.indexer.block.IndexerControllerBlock;
import com.agustinbenitez.indexer.block.IndexerPipeBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
// ChestBlock import removed as we now use generic Container interface
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;

public class IndexerConnectorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public IndexerConnectorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(CONNECTED, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean isConnected = isConnectedToController(level, pos);
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(CONNECTED, isConnected);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONNECTED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        
        // Verificar si hay un contenedor adyacente
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof IndexerConnectorBlockEntity) {
            ((IndexerConnectorBlockEntity) entity).updateConnectedContainer();
            
            // Notificar a los controladores cercanos sobre el cambio
            notifyNearbyControllers(level, pos);
        }
        
        // Actualizar el estado de conexión con el controlador
        boolean isConnected = isConnectedToController(level, pos);
        if (state.getValue(CONNECTED) != isConnected) {
            level.setBlock(pos, state.setValue(CONNECTED, isConnected), Block.UPDATE_ALL);
        }
    }
    
    private void notifyNearbyControllers(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        
        // Buscar controladores en un radio de 16 bloques
        int searchRadius = 16;
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockEntity blockEntity = level.getBlockEntity(checkPos);
                    
                    if (blockEntity instanceof IndexerControllerBlockEntity controller) {
                        controller.markNetworkChanged();
                        // Solo necesitamos notificar a un controlador, ya que cada uno gestionará su propia red
                        return;
                    }
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof IndexerConnectorBlockEntity) {
                NetworkHooks.openScreen((ServerPlayer) player, (IndexerConnectorBlockEntity) entity, pos);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IndexerConnectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.INDEXER_CONNECTOR.get(), IndexerConnectorBlockEntity::tick);
    }

    // Método para verificar si hay un contenedor adyacente
    public static boolean hasAdjacentContainer(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockEntity adjacentEntity = level.getBlockEntity(adjacentPos);
            // Verificar si es un contenedor pero NO un conector
            if (adjacentEntity instanceof net.minecraft.world.Container && 
                !(adjacentEntity instanceof IndexerConnectorBlockEntity)) {
                return true;
            }
        }
        return false;
    }
    
    // Método para verificar si el conector está conectado a un controlador
    public static boolean isConnectedToController(Level level, BlockPos pos) {
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