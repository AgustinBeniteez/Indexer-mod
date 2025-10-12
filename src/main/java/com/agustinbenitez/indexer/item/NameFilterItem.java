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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

public class NameFilterItem extends Item {
    
    public NameFilterItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
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
            openNameFilterScreen(itemStack, finalSlotIndex);
        }
        
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void openNameFilterScreen(ItemStack itemStack, int slotIndex) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.agustinbenitez.indexer.screen.NameFilterScreen(itemStack, slotIndex));
        });
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.indexer.name_filter.tooltip"));
        tooltipComponents.add(Component.translatable("item.indexer.name_filter.description"));
        
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("custom_name")) {
            String customName = tag.getString("custom_name");
            if (!customName.isEmpty()) {
                tooltipComponents.add(Component.literal("§eNombre configurado: " + customName));
            }
        } else {
            tooltipComponents.add(Component.literal("§7Haz clic derecho para configurar"));
        }
    }
    
    /**
     * Obtiene el nombre personalizado configurado en el filtro
     */
    public String getCustomName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("custom_name")) {
            return tag.getString("custom_name");
        }
        return "";
    }
    
    /**
     * Establece el nombre personalizado en el filtro
     */
    public void setCustomName(ItemStack stack, String customName) {
        stack.getOrCreateTag().putString("custom_name", customName);
    }
}