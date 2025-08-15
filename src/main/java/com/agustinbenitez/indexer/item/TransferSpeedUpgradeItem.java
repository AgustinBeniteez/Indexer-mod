package com.agustinbenitez.indexer.item;

import com.agustinbenitez.indexer.IndexerMod;

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
        tooltip.add(Component.literal("Transfers up to " + transferRate + " items at once").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Use with right click on an Indexer Controller").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Single use item!").withStyle(ChatFormatting.RED));
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
        
        // Check if the block is an Indexer Controller
        if (blockEntity instanceof IndexerControllerBlockEntity controller) {
                // Validar que se aplique la mejora en el orden correcto
                int currentLevel = controller.getCurrentUpgradeLevel();
                int requiredLevel = this.upgradeLevel - 1; // El nivel requerido es el anterior al que queremos aplicar
                
                if (currentLevel != requiredLevel) {
                    // Mostrar mensaje de error indicando qué mejora necesita
                    String requiredUpgradeName;
                    switch (requiredLevel) {
                        case 0:
                            requiredUpgradeName = "ninguna mejora previa";
                            break;
                        case 1:
                            requiredUpgradeName = "mejora Básica";
                            break;
                        case 2:
                            requiredUpgradeName = "mejora Avanzada";
                            break;
                        default:
                            requiredUpgradeName = "mejora de nivel " + requiredLevel;
                            break;
                    }
                    
                    String currentUpgradeName;
                    switch (this.upgradeLevel) {
                        case 1:
                            currentUpgradeName = "Básica";
                            break;
                        case 2:
                            currentUpgradeName = "Avanzada";
                            break;
                        case 3:
                            currentUpgradeName = "Élite";
                            break;
                        default:
                            currentUpgradeName = "nivel " + this.upgradeLevel;
                            break;
                    }
                    
                    if (currentLevel < requiredLevel) {
                        player.sendSystemMessage(Component.literal("No puedes aplicar la mejora " + currentUpgradeName + 
                                " sin haber aplicado primero: " + requiredUpgradeName).withStyle(ChatFormatting.RED));
                    } else {
                        player.sendSystemMessage(Component.literal("Esta mejora ya ha sido aplicada o superada").withStyle(ChatFormatting.YELLOW));
                    }
                    return InteractionResult.FAIL;
                }
                
                // Apply the upgrade
                try {
                    // Set the transfer speed
                    controller.setItemsPerTransfer(this.transferRate);
                    
                    // Actualizar el nivel de mejora
                    controller.setCurrentUpgradeLevel(this.upgradeLevel);
                    
                    // Reset the cooldown so it starts transferring immediately
                    java.lang.reflect.Field cooldownField = IndexerControllerBlockEntity.class.getDeclaredField("transferCooldown");
                    cooldownField.setAccessible(true);
                    cooldownField.setInt(controller, 0);
                    
                    controller.setChanged();
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    IndexerMod.LOGGER.error("Error al aplicar mejora de velocidad: " + e.getMessage());
                    player.sendSystemMessage(Component.literal("Error al aplicar la mejora").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
                
                // Notify the player
                player.sendSystemMessage(Component.literal("¡Mejora aplicada! Ahora se transferirán hasta " + 
                        this.transferRate + " objetos a la vez en cada ciclo").withStyle(ChatFormatting.GREEN));
                player.sendSystemMessage(Component.literal("La transferencia comenzará inmediatamente").withStyle(ChatFormatting.AQUA));
                
                // Consume the item
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                
                return InteractionResult.CONSUME;
        } else {
            player.sendSystemMessage(Component.literal("This upgrade can only be applied to an Indexer Controller").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
    }
}