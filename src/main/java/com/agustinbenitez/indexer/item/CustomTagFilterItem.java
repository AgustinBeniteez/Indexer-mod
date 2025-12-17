package com.agustinbenitez.indexer.item;

import com.agustinbenitez.indexer.init.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.RegistryObject;
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

import net.minecraft.world.item.Item.TooltipContext;

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

    @OnlyIn(Dist.CLIENT)
    private void openCustomTagFilterScreen(ItemStack itemStack, int slotIndex) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.agustinbenitez.indexer.screen.CustomTagFilterScreen(itemStack, slotIndex));
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.indexer.custom_tag_blocker.tooltip"));
        tooltipComponents.add(Component.translatable("item.indexer.custom_tag_blocker.description"));

        if (stack.has(ModDataComponents.CUSTOM_TAG.get())) {
            String customTag = stack.get(ModDataComponents.CUSTOM_TAG.get());
            Component tagComponent = Component.translatable("item.indexer.custom_tag_blocker.current_tag",
                    Component.literal(customTag).withStyle(style -> style.withColor(0x5555FF))); // Azul
            tooltipComponents.add(tagComponent);
        } else {
            tooltipComponents.add(Component.translatable("item.indexer.custom_tag_blocker.no_tag"));
        }

        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
    }

    public String getCustomTag(ItemStack stack) {
        if (stack.has(ModDataComponents.CUSTOM_TAG.get())) {
            return stack.get(ModDataComponents.CUSTOM_TAG.get());
        }
        return "";
    }

    public void setCustomTag(ItemStack stack, String customTag) {
        stack.set(ModDataComponents.CUSTOM_TAG.get(), customTag);
    }
}