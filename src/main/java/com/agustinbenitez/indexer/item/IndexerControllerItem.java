package com.agustinbenitez.indexer.item;

import net.minecraft.ChatFormatting;
import com.agustinbenitez.indexer.init.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.block.Block;
import java.util.List;

public class IndexerControllerItem extends BlockItem {

    public IndexerControllerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // Verificar si el item tiene datos de mejoras guardados
        if (stack.has(ModDataComponents.UPGRADE_LEVEL.get())) {
            int upgradeLevel = stack.get(ModDataComponents.UPGRADE_LEVEL.get());
            int itemsPerTransfer = 0;
            if (stack.has(ModDataComponents.ITEMS_PER_TRANSFER.get())) {
                itemsPerTransfer = stack.get(ModDataComponents.ITEMS_PER_TRANSFER.get());
            } else {
                // Default value or strictly required? Assuming 64 if not set but upgrade is
                // set?
                // Let's assume 0 for now or safe default.
                itemsPerTransfer = 64;
            }

            if (upgradeLevel > 0) {
                // Agregar información de mejora al tooltip
                tooltip.add(Component.translatable("item.indexer.controller.upgrade_info",
                        getUpgradeName(upgradeLevel), itemsPerTransfer)
                        .withStyle(ChatFormatting.AQUA));
            }
        }
    }

    private String getUpgradeName(int level) {
        switch (level) {
            case 1:
                return Component.translatable("item.indexer.transfer_speed_upgrade_basic").getString();
            case 2:
                return Component.translatable("item.indexer.transfer_speed_upgrade_copper").getString();
            case 3:
                return Component.translatable("item.indexer.transfer_speed_upgrade_advanced").getString();
            case 4:
                return Component.translatable("item.indexer.transfer_speed_upgrade_elite").getString();
            case 5:
                return Component.translatable("item.indexer.transfer_speed_upgrade_definitive").getString();
            default:
                return "Unknown Upgrade";
        }
    }
}