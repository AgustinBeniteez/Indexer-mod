package com.agustinbenitez.indexer.item;

import com.agustinbenitez.indexer.block.entity.IndexerConnectorBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public class ConnectorCapacityUpgradeItem extends Item {
    public ConnectorCapacityUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.indexer.connector_capacity_upgrade.tooltip").withStyle(ChatFormatting.GRAY));
        int remainingUses = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("item.indexer.connector_capacity_upgrade.uses_info", remainingUses).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack itemstack = context.getItemInHand();

        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IndexerConnectorBlockEntity connector)) {
            player.sendSystemMessage(Component.translatable("message.indexer.connector_upgrade.only_connector").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        // Evitar aplicar si ya tiene la mejora
        if (connector.getConnectorLevel() >= 2) {
            player.sendSystemMessage(Component.translatable("message.indexer.connector_upgrade.already_applied").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }

        // Aplicar mejora: subir a nivel 2 (18 slots de filtros)
        connector.setConnectorLevel(2);
        connector.ensureFilterCapacity();
        connector.setChanged();

        player.sendSystemMessage(Component.translatable("message.indexer.connector_upgrade.success").withStyle(ChatFormatting.GREEN));

        // Consumir un uso del ítem (durabilidad)
        if (!player.getAbilities().instabuild) {
            itemstack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(context.getHand()));
        }

        return InteractionResult.CONSUME;
    }
}