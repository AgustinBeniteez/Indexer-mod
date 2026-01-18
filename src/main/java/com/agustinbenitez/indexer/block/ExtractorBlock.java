package com.agustinbenitez.indexer.block;

import com.agustinbenitez.indexer.block.entity.ExtractorBlockEntity;
import com.agustinbenitez.indexer.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.Container;
import net.minecraft.core.Direction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.mojang.serialization.MapCodec;

public class ExtractorBlock extends BaseEntityBlock {
    public static final MapCodec<ExtractorBlock> CODEC = simpleCodec(ExtractorBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    
    // Mapa para rastrear si un bloque fue roto en modo creativo
    private static final Map<BlockPos, Boolean> creativeModeBreaks = new ConcurrentHashMap<>();

    public ExtractorBlock(Properties properties) {
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            // Verificar si hay un cofre arriba y permitir abrirlo
            BlockPos abovePos = pos.above();
            BlockEntity aboveEntity = level.getBlockEntity(abovePos);
            
            if (aboveEntity instanceof Container && 
                aboveEntity.getClass().getName().contains("ChestBlockEntity")) {
                // Si hay un cofre arriba, permitir que el jugador lo abra
                BlockHitResult newHit = new BlockHitResult(hit.getLocation(), hit.getDirection(), abovePos, hit.isInside());
                return level.getBlockState(abovePos).useWithoutItem(level, player, newHit);
            }
        }
        // No tiene interfaz propia, solo retorna éxito
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtractorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.EXTRACTOR,
                (level1, pos, state1, blockEntity) -> ExtractorBlockEntity.tick(level1, pos, state1, blockEntity));
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
                // Dropear el ítem del bloque Extractor
                net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(this);
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemStack);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}