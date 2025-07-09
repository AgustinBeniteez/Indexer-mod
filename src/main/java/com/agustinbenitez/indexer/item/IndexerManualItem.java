package com.agustinbenitez.indexer.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class IndexerManualItem extends Item {

    public IndexerManualItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.indexer.manual.tooltip").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        
        if (level.isClientSide()) {
            // Abrir la pantalla GUI del manual en el cliente
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.agustinbenitez.indexer.screen.IndexerManualScreen());
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}