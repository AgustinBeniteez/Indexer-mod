package com.agustinbenitez.indexer.item;

import com.agustinbenitez.indexer.init.ModDataComponents;
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
            openAttributeFilterScreen(itemStack, finalSlotIndex);
        }

        return InteractionResultHolder.success(itemStack);
    }

    @OnlyIn(Dist.CLIENT)
    private void openAttributeFilterScreen(ItemStack itemStack, int slotIndex) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.agustinbenitez.indexer.screen.AttributeFilterScreen(itemStack, slotIndex));
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.indexer.attribute_filter.tooltip"));
        tooltipComponents.add(Component.translatable("item.indexer.attribute_filter.description"));

        if (stack.has(ModDataComponents.ATTRIBUTE_FILTER.get())) {
            String attribute = stack.get(ModDataComponents.ATTRIBUTE_FILTER.get());
            Component attributeComponent = Component.translatable("item.indexer.attribute_filter.current_attribute",
                    Component.literal(attribute).withStyle(style -> style.withColor(0x55FF55))); // Verde
            tooltipComponents.add(attributeComponent);
        } else {
            tooltipComponents.add(Component.translatable("item.indexer.attribute_filter.no_attribute"));
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    public String getAttribute(ItemStack stack) {
        if (stack.has(ModDataComponents.ATTRIBUTE_FILTER.get())) {
            return stack.get(ModDataComponents.ATTRIBUTE_FILTER.get());
        }
        return "";
    }

    public void setAttribute(ItemStack stack, String attribute) {
        stack.set(ModDataComponents.ATTRIBUTE_FILTER.get(), attribute);
    }
}