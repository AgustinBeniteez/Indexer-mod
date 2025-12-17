package com.agustinbenitez.indexer.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class FoodFilterItem extends Item {

    public FoodFilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.indexer.food_filter.tooltip"));
        tooltipComponents.add(Component.translatable("item.indexer.food_filter.description"));
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
    }
}