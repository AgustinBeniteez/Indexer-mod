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
        
        if (!level.isClientSide()) {
            // Show instructions to the player
            player.sendSystemMessage(Component.literal("===== Indexer Manual =====").withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.literal("The Indexer system allows you to automatically organize items in your chests.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal(""));
            
            player.sendSystemMessage(Component.literal("Components:").withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.literal("1. Indexer Controller: The main component where you deposit items.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("2. Indexer Pipe: Connects the controller with connectors.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("3. Indexer Connector: Placed next to a chest and filters items.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal(""));
            
            player.sendSystemMessage(Component.literal("Instructions:").withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.literal("1. Place the Indexer Controller at the center of your system.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("2. Connect Indexer Pipes from the controller to the connectors.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("3. Place Indexer Connectors next to your chests.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("4. Configure each connector with the item you want to filter.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("5. Deposit items in the controller and they will be sent to the corresponding chests.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal(""));
            
            player.sendSystemMessage(Component.literal("Technical specifications:").withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.literal("- The controller can detect up to 250 blocks away.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("- There is no limit to the number of connectors you can use.").withStyle(ChatFormatting.WHITE));
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}