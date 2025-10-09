package com.agustinbenitez.indexer.item;

import com.agustinbenitez.indexer.screen.AttributeFilterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

public class AttributeFilterItem extends Item {
    
    public AttributeFilterItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        
        if (level.isClientSide) {
            // Obtener el índice del slot del item en el inventario
            int slotIndex = -1;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i) == itemStack) {
                    slotIndex = i;
                    break;
                }
            }
            
            final int finalSlotIndex = slotIndex;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft.getInstance().setScreen(new AttributeFilterScreen(itemStack, finalSlotIndex));
            });
        }
        
        return InteractionResultHolder.success(itemStack);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.indexer.attribute_filter.tooltip"));
        tooltipComponents.add(Component.translatable("item.indexer.attribute_filter.description"));
        
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("attribute_filter")) {
            String attribute = tag.getString("attribute_filter");
            Component attributeComponent = Component.translatable("item.indexer.attribute_filter.current_attribute", 
                Component.literal(attribute).withStyle(style -> style.withColor(0x55FF55))); // Verde
            tooltipComponents.add(attributeComponent);
        } else {
            tooltipComponents.add(Component.translatable("item.indexer.attribute_filter.no_attribute"));
        }
        
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
    
    public String getAttribute(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("attribute_filter")) {
            return tag.getString("attribute_filter");
        }
        return "";
    }
    
    public void setAttribute(ItemStack stack, String attribute) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("attribute_filter", attribute);
    }
}