package com.agustinbenitez.indexer.item;

import com.agustinbenitez.indexer.screen.ModFilterScreen;
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

public class ModFilterItem extends Item {
    
    public ModFilterItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft.getInstance().setScreen(new ModFilterScreen(stack, -1));
            });
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.indexer.mod_filter.tooltip"));
        tooltipComponents.add(Component.translatable("item.indexer.mod_filter.description"));
        
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("mod_id")) {
            String modId = tag.getString("mod_id");
            if (!modId.isEmpty()) {
                tooltipComponents.add(Component.literal("§eMod configurado: " + modId));
            }
        } else {
            tooltipComponents.add(Component.literal("§7Haz clic derecho para configurar"));
        }
        
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
    
    /**
     * Obtiene el mod ID configurado en el filtro
     */
    public String getModId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("mod_id")) {
            return tag.getString("mod_id");
        }
        return "";
    }
    
    /**
     * Establece el mod ID en el filtro
     */
    public void setModId(ItemStack stack, String modId) {
        stack.getOrCreateTag().putString("mod_id", modId);
    }
}