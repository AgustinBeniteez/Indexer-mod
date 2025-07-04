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
// ChestBlockEntity import removed as we now use generic Container interface
import com.agustinbenitez.indexer.block.entity.IndexerConnectorBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.block.entity.DropBoxBlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public class IndexerAdjusterItem extends Item {
    private BlockPos selectedContainerPos = null;
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
        
        // Si hace clic en cualquier tipo de contenedor (cofre, barril, horno, etc.)
        if (blockEntity instanceof net.minecraft.world.Container) {
            selectedContainerPos = pos;
            selectedDropBoxPos = null; // Resetear selección de DropBox
            // Obtener el nombre del bloque para el mensaje
            String blockName = level.getBlockState(pos).getBlock().getDescriptionId();
            player.sendSystemMessage(Component.literal("Contenedor seleccionado en: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " (" + blockName + ")"));
            return InteractionResult.SUCCESS;
        }
        
        // Si hace clic en un DropBox
        if (blockEntity instanceof DropBoxBlockEntity) {
            selectedDropBoxPos = pos;
            selectedContainerPos = null; // Resetear selección de contenedor
            player.sendSystemMessage(Component.literal("DropBox seleccionado en: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
            return InteractionResult.SUCCESS;
        }
        
        // Si hace clic en un conector
        if (blockEntity instanceof IndexerConnectorBlockEntity connector) {
            if (selectedContainerPos != null) {
                // Verificar que el contenedor seleccionado aún existe
                BlockEntity containerEntity = level.getBlockEntity(selectedContainerPos);
                if (containerEntity instanceof net.minecraft.world.Container) {
                    connector.setConnectedContainerPos(selectedContainerPos);
                    // Obtener el nombre del bloque para el mensaje
                    String blockName = level.getBlockState(selectedContainerPos).getBlock().getDescriptionId();
                    player.sendSystemMessage(Component.literal("Conector vinculado al contenedor en: " + selectedContainerPos.getX() + ", " + selectedContainerPos.getY() + ", " + selectedContainerPos.getZ() + " (" + blockName + ")"));
                    selectedContainerPos = null; // Resetear selección
                } else {
                    player.sendSystemMessage(Component.literal("El contenedor seleccionado ya no existe"));
                    selectedContainerPos = null;
                }
            } else {
                player.sendSystemMessage(Component.literal("Primero selecciona un contenedor haciendo clic derecho en él"));
            }
            return InteractionResult.SUCCESS;
        }
        
        // Si hace clic en un controlador
        if (blockEntity instanceof IndexerControllerBlockEntity controller) {
            if (selectedDropBoxPos != null) {
                // Verificar que el DropBox seleccionado aún existe
                BlockEntity dropBoxEntity = level.getBlockEntity(selectedDropBoxPos);
                if (dropBoxEntity instanceof DropBoxBlockEntity) {
                    controller.setDropContainerPos(selectedDropBoxPos);
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