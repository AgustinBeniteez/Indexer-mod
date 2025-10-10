package com.agustinbenitez.indexer.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

public class IndexerConnectorItem extends BlockItem {

    public IndexerConnectorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.indexer.indexer_connector.tooltip"));
        if (stack.hasTag()) {
            net.minecraft.nbt.CompoundTag nbt = stack.getTag();
            if (nbt.contains("ConnectorLevel")) {
                int connectorLevel = nbt.getInt("ConnectorLevel");
                if (connectorLevel >= 2) {
                    tooltip.add(Component.translatable("item.indexer.indexer_connector.level2").withStyle(net.minecraft.ChatFormatting.GREEN));
                }
            }
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}