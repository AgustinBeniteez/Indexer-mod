package com.agustinbenitez.indexer.block;

import com.agustinbenitez.indexer.block.entity.DropBoxBlockEntity;
import com.agustinbenitez.indexer.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DropBoxBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    
    // Mapa para rastrear si un bloque fue roto en modo creativo
    private static final Map<BlockPos, Boolean> creativeModeBreaks = new ConcurrentHashMap<>();

    public DropBoxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof DropBoxBlockEntity dropBox) {
                NetworkHooks.openScreen((ServerPlayer) player, dropBox, pos);
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DropBoxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.DROP_BOX.get(),
                (level1, pos, state1, blockEntity) -> blockEntity.tick(level1, pos, state1));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Registrar si el jugador está en modo creativo
        creativeModeBreaks.put(pos, player.getAbilities().instabuild);
        super.playerWillDestroy(level, pos, state, player);
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            // Verificar si el bloque fue roto en modo creativo
            Boolean wasCreativeBreak = creativeModeBreaks.remove(pos);
            boolean isCreativeBreak = wasCreativeBreak != null && wasCreativeBreak;
            
            // Solo dropear items si NO fue roto en modo creativo
            if (!isCreativeBreak) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof DropBoxBlockEntity) {
                    ((DropBoxBlockEntity) blockEntity).dropContents();
                }
                
                // Dropear el ítem del bloque DropBox
                net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(this);
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemStack);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}