package com.agustinbenitez.indexer.block;

import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.init.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
// NetworkHooks import removed

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import com.mojang.serialization.MapCodec;

public class IndexerControllerBlock extends BaseEntityBlock {
    public static final MapCodec<IndexerControllerBlock> CODEC = simpleCodec(IndexerControllerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    
    // Mapa para rastrear si un bloque fue roto en modo creativo
    private static final Map<BlockPos, Boolean> creativeModeBreaks = new ConcurrentHashMap<>();

    public IndexerControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        
        // Restaurar el estado de mejoras si existe en el NBT del item
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag nbt = customData.copyTag();
            if (nbt.contains("UpgradeLevel") && nbt.contains("ItemsPerTransfer")) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof IndexerControllerBlockEntity controller) {
                    int upgradeLevel = nbt.getInt("UpgradeLevel");
                    int itemsPerTransfer = nbt.getInt("ItemsPerTransfer");
                    
                    controller.setCurrentUpgradeLevel(upgradeLevel);
                    controller.setItemsPerTransfer(itemsPerTransfer);
                    controller.setChanged();
                }
            }
        }
        

    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Registrar si el jugador está en modo creativo
        creativeModeBreaks.put(pos, player.getAbilities().instabuild);
        return super.playerWillDestroy(level, pos, state, player);
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            // Verificar si el bloque fue roto en modo creativo
            Boolean wasCreativeBreak = creativeModeBreaks.remove(pos);
            boolean isCreativeBreak = wasCreativeBreak != null && wasCreativeBreak;
            
            // Solo dropear items si NO fue roto en modo creativo
            if (!isCreativeBreak) {
                // Obtener la BlockEntity para guardar el estado de mejoras
                BlockEntity blockEntity = level.getBlockEntity(pos);
                net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(this);
                
                if (blockEntity instanceof IndexerControllerBlockEntity controller) {
                    // Guardar el estado de mejoras en el NBT del item
                    int upgradeLevel = controller.getCurrentUpgradeLevel();
                    int itemsPerTransfer = controller.getItemsPerTransfer();
                    
                    if (upgradeLevel > 0) {
                        CustomData.update(DataComponents.CUSTOM_DATA, itemStack, (tag) -> {
                            tag.putInt("UpgradeLevel", upgradeLevel);
                            tag.putInt("ItemsPerTransfer", itemsPerTransfer);
                        });
                    }
                }
                
                // Dropear el ítem del controlador cuando se rompe el bloque
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemStack);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof IndexerControllerBlockEntity controller) {
                // Abrir directamente la GUI de red con clic derecho normal
                controller.openNetworkScreen((ServerPlayer) player, pos);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IndexerControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.INDEXER_CONTROLLER, IndexerControllerBlockEntity::tick);
    }
}
