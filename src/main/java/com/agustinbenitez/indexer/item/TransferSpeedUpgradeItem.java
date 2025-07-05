package com.agustinbenitez.indexer.item;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.entity.DropBoxBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;

import net.minecraft.ChatFormatting;
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

import javax.annotation.Nullable;
import java.util.List;

public class TransferSpeedUpgradeItem extends Item {
    private final int upgradeLevel;
    private final int transferRate;

    public TransferSpeedUpgradeItem(Properties properties, int upgradeLevel, int transferRate) {
        super(properties);
        this.upgradeLevel = upgradeLevel;
        this.transferRate = transferRate;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.indexer.transfer_speed_upgrade.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Transfiere hasta " + transferRate + " ítems a la vez").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Usar con click derecho en un DropBox").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("¡Objeto de un solo uso!").withStyle(ChatFormatting.RED));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack itemstack = context.getItemInHand();
        
        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        
        // Verificar si el bloque es un DropBox
        if (blockEntity instanceof DropBoxBlockEntity) {
            // Buscar el controlador conectado
            BlockEntity controllerEntity = null;
            for (int x = -5; x <= 5; x++) {
                for (int y = -5; y <= 5; y++) {
                    for (int z = -5; z <= 5; z++) {
                        BlockPos checkPos = pos.offset(x, y, z);
                        BlockEntity checkEntity = level.getBlockEntity(checkPos);
                        if (checkEntity instanceof IndexerControllerBlockEntity) {
                            controllerEntity = checkEntity;
                            break;
                        }
                    }
                    if (controllerEntity != null) break;
                }
                if (controllerEntity != null) break;
            }
            
            if (controllerEntity instanceof IndexerControllerBlockEntity controller) {
                // Aplicar la mejora
                try {
                    // Establecer la velocidad de transferencia
                    controller.setItemsPerTransfer(this.transferRate);
                    
                    // Reiniciar el cooldown para que comience a transferir inmediatamente
                    java.lang.reflect.Field cooldownField = IndexerControllerBlockEntity.class.getDeclaredField("transferCooldown");
                    cooldownField.setAccessible(true);
                    cooldownField.setInt(controller, 0);
                    
                    controller.setChanged();
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    IndexerMod.LOGGER.error("Error al aplicar mejora de velocidad: " + e.getMessage());
                    player.sendSystemMessage(Component.literal("Error al aplicar la mejora").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
                
                // Notificar al jugador
                player.sendSystemMessage(Component.literal("¡Mejora aplicada! Ahora se transferirán hasta " + 
                        this.transferRate + " ítems a la vez en cada ciclo").withStyle(ChatFormatting.GREEN));
                player.sendSystemMessage(Component.literal("La transferencia comenzará inmediatamente").withStyle(ChatFormatting.AQUA));
                
                // Consumir el ítem
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                
                return InteractionResult.CONSUME;
            } else {
                player.sendSystemMessage(Component.literal("No se encontró un controlador Indexer cercano").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
        }
        
        return InteractionResult.PASS;
    }
}