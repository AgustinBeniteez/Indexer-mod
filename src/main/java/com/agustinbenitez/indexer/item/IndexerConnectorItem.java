package com.agustinbenitez.indexer.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class IndexerConnectorItem extends BlockItem {

    public IndexerConnectorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.indexer.indexer_connector.tooltip"));
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag nbt = customData.copyTag();
            if (nbt.contains("ConnectorLevel")) {
                int connectorLevel = nbt.getInt("ConnectorLevel");
                if (connectorLevel >= 2) {
                    tooltip.add(Component.translatable("item.indexer.indexer_connector.level2").withStyle(net.minecraft.ChatFormatting.GREEN));
                }
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
