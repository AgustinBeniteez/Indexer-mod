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
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.indexer.transfer_speed_upgrade.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.indexer.transfer_speed_upgrade.transfers_info", transferRate).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.indexer.transfer_speed_upgrade.usage").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.indexer.transfer_speed_upgrade.single_use").withStyle(ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltip, flag);
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
                
                // Debug message
                IndexerMod.LOGGER.info("Aplicando mejora - Nivel actual: {}, Nivel requerido: {}, Nivel de esta mejora: {}", 
                    currentLevel, requiredLevel, this.upgradeLevel);
                
                // Check if this exact upgrade is already applied
                if (currentLevel == this.upgradeLevel) {
                    player.sendSystemMessage(Component.translatable("message.indexer.upgrade.already_applied").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.FAIL;
                }
                
                // Check if upgrade is already applied (higher level)
                if (currentLevel > this.upgradeLevel) {
                    player.sendSystemMessage(Component.translatable("message.indexer.upgrade.already_applied").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.FAIL;
                }
                
                // Check if prerequisite is met - debe tener exactamente el nivel anterior
                if (currentLevel != requiredLevel) {
                    // Mostrar mensaje de error indicando qué mejora necesita
                    Component requiredUpgradeName;
                    switch (requiredLevel) {
                        case 0:
                            requiredUpgradeName = Component.translatable("upgrade.indexer.none");
                            break;
                        case 1:
                            requiredUpgradeName = Component.translatable("upgrade.indexer.basic");
                            break;
                        case 2:
                            requiredUpgradeName = Component.translatable("upgrade.indexer.copper");
                            break;
                        case 3:
                            requiredUpgradeName = Component.translatable("upgrade.indexer.advanced");
                            break;
                        case 4:
                            requiredUpgradeName = Component.translatable("upgrade.indexer.elite");
                            break;
                        default:
                            requiredUpgradeName = Component.literal("nivel " + requiredLevel);
                            break;
                    }
                    
                    Component currentUpgradeName;
                    switch (this.upgradeLevel) {
                        case 1:
                            currentUpgradeName = Component.translatable("upgrade.indexer.basic");
                            break;
                        case 2:
                            currentUpgradeName = Component.translatable("upgrade.indexer.copper");
                            break;
                        case 3:
                            currentUpgradeName = Component.translatable("upgrade.indexer.advanced");
                            break;
                        case 4:
                            currentUpgradeName = Component.translatable("upgrade.indexer.elite");
                            break;
                        case 5:
                            currentUpgradeName = Component.translatable("upgrade.indexer.definitive");
                            break;
                        default:
                            currentUpgradeName = Component.literal("nivel " + this.upgradeLevel);
                            break;
                    }
                    
                    player.sendSystemMessage(Component.translatable("message.indexer.upgrade.prerequisite_required", 
                            currentUpgradeName, requiredUpgradeName).withStyle(ChatFormatting.RED));
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
                    player.sendSystemMessage(Component.translatable("message.indexer.upgrade.error").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
                
                // Notify the player
                player.sendSystemMessage(Component.translatable("message.indexer.upgrade.success", 
                        this.transferRate).withStyle(ChatFormatting.GREEN));
                player.sendSystemMessage(Component.translatable("message.indexer.upgrade.transfer_starts").withStyle(ChatFormatting.AQUA));
                
                // Consume the item
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                
                return InteractionResult.CONSUME;
        } else {
            player.sendSystemMessage(Component.translatable("message.indexer.upgrade.controller_only").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
    }
}
