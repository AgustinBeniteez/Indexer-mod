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
            // Mostrar instrucciones al jugador
            player.sendSystemMessage(Component.literal("===== Indexer Manual =====").withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.literal("El sistema Indexer te permite organizar automáticamente los ítems en tus cofres.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal(""));
            
            player.sendSystemMessage(Component.literal("Componentes:").withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.literal("1. Controlador Indexer: El componente principal donde depositas los ítems.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("2. Tubería Indexer: Conecta el controlador con los conectores.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("3. Conector Indexer: Se coloca junto a un cofre y filtra los ítems.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal(""));
            
            player.sendSystemMessage(Component.literal("Instrucciones:").withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.literal("1. Coloca el Controlador Indexer en el centro de tu sistema.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("2. Conecta Tuberías Indexer desde el controlador hacia los conectores.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("3. Coloca Conectores Indexer junto a tus cofres.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("4. Configura cada conector con el ítem que deseas filtrar.").withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("5. Deposita ítems en el controlador y estos serán enviados a los cofres correspondientes.").withStyle(ChatFormatting.WHITE));
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}