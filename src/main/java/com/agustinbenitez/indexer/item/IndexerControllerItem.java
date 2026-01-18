package com.agustinbenitez.indexer.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class IndexerControllerItem extends BlockItem {
    
    public IndexerControllerItem(Block block, Properties properties) {
        super(block, properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Verificar si el item tiene datos de mejoras guardados
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("UpgradeLevel")) {
                int upgradeLevel = tag.getInt("UpgradeLevel");
                int itemsPerTransfer = tag.getInt("ItemsPerTransfer");
                
                if (upgradeLevel > 0) {
                    // Agregar información de mejora al tooltip
                    tooltip.add(Component.translatable("item.indexer.controller.upgrade_info", 
                        getUpgradeName(upgradeLevel), itemsPerTransfer)
                        .withStyle(ChatFormatting.AQUA));
                }
            }
        }
    }
    
    private String getUpgradeName(int level) {
        switch (level) {
            case 1: return Component.translatable("item.indexer.transfer_speed_upgrade_basic").getString();
            case 2: return Component.translatable("item.indexer.transfer_speed_upgrade_copper").getString();
            case 3: return Component.translatable("item.indexer.transfer_speed_upgrade_advanced").getString();
            case 4: return Component.translatable("item.indexer.transfer_speed_upgrade_elite").getString();
            case 5: return Component.translatable("item.indexer.transfer_speed_upgrade_definitive").getString();
            default: return "Unknown Upgrade";
        }
    }
}
