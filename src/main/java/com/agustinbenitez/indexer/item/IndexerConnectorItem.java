package com.agustinbenitez.indexer.item;

import com.agustinbenitez.indexer.init.ModDataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

public class IndexerConnectorItem extends BlockItem {

    public IndexerConnectorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.indexer.indexer_connector.tooltip"));
        if (stack.has(ModDataComponents.CONNECTOR_LEVEL.get())) {
            int connectorLevel = stack.get(ModDataComponents.CONNECTOR_LEVEL.get());
            if (connectorLevel >= 2) {
                tooltip.add(Component.translatable("item.indexer.indexer_connector.level2")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}