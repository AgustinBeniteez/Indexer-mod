package com.agustinbenitez.indexer.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerConnectorBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.block.entity.DropBoxBlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public class IndexerAdjusterItem extends Item {
    private BlockPos selectedChestPos = null;
    private BlockPos selectedDropBoxPos = null;
    
    public IndexerAdjusterItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        
        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }
        
        BlockEntity blockEntity = level.getBlockEntity(pos);
        
        // Si hace clic en un cofre
        if (blockEntity instanceof ChestBlockEntity) {
            selectedChestPos = pos;
            selectedDropBoxPos = null; // Resetear selección de DropBox
            player.sendSystemMessage(Component.literal("Cofre seleccionado en: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
            return InteractionResult.SUCCESS;
        }
        
        // Si hace clic en un DropBox
        if (blockEntity instanceof DropBoxBlockEntity) {
            selectedDropBoxPos = pos;
            selectedChestPos = null; // Resetear selección de cofre
            player.sendSystemMessage(Component.literal("DropBox seleccionado en: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
            return InteractionResult.SUCCESS;
        }
        
        // Si hace clic en un conector
        if (blockEntity instanceof IndexerConnectorBlockEntity connector) {
            if (selectedChestPos != null) {
                // Verificar que el cofre seleccionado aún existe
                BlockEntity chestEntity = level.getBlockEntity(selectedChestPos);
                if (chestEntity instanceof ChestBlockEntity) {
                    connector.setConnectedChestPos(selectedChestPos);
                    player.sendSystemMessage(Component.literal("Conector vinculado al cofre en: " + selectedChestPos.getX() + ", " + selectedChestPos.getY() + ", " + selectedChestPos.getZ()));
                    selectedChestPos = null; // Resetear selección
                } else {
                    player.sendSystemMessage(Component.literal("El cofre seleccionado ya no existe"));
                    selectedChestPos = null;
                }
            } else {
                player.sendSystemMessage(Component.literal("Primero selecciona un cofre haciendo clic derecho en él"));
            }
            return InteractionResult.SUCCESS;
        }
        
        // Si hace clic en un controlador
        if (blockEntity instanceof IndexerControllerBlockEntity controller) {
            if (selectedDropBoxPos != null) {
                // Verificar que el DropBox seleccionado aún existe
                BlockEntity dropBoxEntity = level.getBlockEntity(selectedDropBoxPos);
                if (dropBoxEntity instanceof DropBoxBlockEntity) {
                    controller.setDropChestPos(selectedDropBoxPos);
                    player.sendSystemMessage(Component.literal("Controlador conectado al DropBox en: " + selectedDropBoxPos.getX() + ", " + selectedDropBoxPos.getY() + ", " + selectedDropBoxPos.getZ()));
                    selectedDropBoxPos = null; // Resetear selección
                } else {
                    player.sendSystemMessage(Component.literal("El DropBox seleccionado ya no existe"));
                    selectedDropBoxPos = null;
                }
            } else {
                player.sendSystemMessage(Component.literal("Primero selecciona un DropBox haciendo clic derecho en él"));
            }
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.indexer.adjuster.tooltip"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}