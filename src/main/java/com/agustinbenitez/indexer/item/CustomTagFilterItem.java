package com.agustinbenitez.indexer.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class CustomTagFilterItem extends Item {
    
    public CustomTagFilterItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            // Abrir la GUI en el cliente
            int slotIndex = player.getInventory().selected;
            if (hand == InteractionHand.OFF_HAND) {
                slotIndex = 40; // Slot de la mano secundaria
            }
            openCustomTagFilterScreen(itemStack, slotIndex);
        }
        
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }
    
    private void openCustomTagFilterScreen(ItemStack itemStack, int slotIndex) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
            new com.agustinbenitez.indexer.screen.CustomTagFilterScreen(itemStack, slotIndex));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.indexer.custom_tag_filter.description"));
        
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("custom_tag_filter")) {
                String filter = tag.getString("custom_tag_filter");
                Component filterComponent = Component.translatable("item.indexer.custom_tag_filter.current_filter",
                    Component.literal(filter).withStyle(style -> style.withColor(0x55FF55))); // Verde
                tooltipComponents.add(filterComponent);
                super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
                return;
            }
        }
        tooltipComponents.add(Component.translatable("item.indexer.custom_tag_filter.no_filter"));
        
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
    }
    
    public String getFilter(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("custom_tag_filter")) {
                return tag.getString("custom_tag_filter");
            }
        }
        return "";
    }
    
    public void setFilter(ItemStack stack, String filter) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (tag) -> {
            tag.putString("custom_tag_filter", filter);
        });
    }
}
