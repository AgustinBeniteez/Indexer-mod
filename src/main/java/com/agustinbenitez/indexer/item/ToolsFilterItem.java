package com.agustinbenitez.indexer.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ToolsFilterItem extends Item {
    
    public ToolsFilterItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.indexer.tools_filter.tooltip"));
        tooltipComponents.add(Component.translatable("item.indexer.tools_filter.description"));
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}