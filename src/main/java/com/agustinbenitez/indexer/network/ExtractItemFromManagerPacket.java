package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.entity.IndexerManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ExtractItemFromManagerPacket(BlockPos managerPos, ResourceLocation itemId, int count,
        ItemStack variantStack) implements CustomPacketPayload {
    public static final Type<ExtractItemFromManagerPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "extract_item_from_manager"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractItemFromManagerPacket> STREAM_CODEC = StreamCodec
            .composite(
                    BlockPos.STREAM_CODEC, ExtractItemFromManagerPacket::managerPos,
                    ResourceLocation.STREAM_CODEC, ExtractItemFromManagerPacket::itemId,
                    ByteBufCodecs.INT, ExtractItemFromManagerPacket::count,
                    ItemStack.STREAM_CODEC, ExtractItemFromManagerPacket::variantStack,
                    ExtractItemFromManagerPacket::new);

    public static ExtractItemFromManagerPacket create(BlockPos managerPos, ResourceLocation itemId, int count,
            ItemStack variantStack) {
        ItemStack copy = variantStack.copy();
        copy.setCount(1);
        return new ExtractItemFromManagerPacket(managerPos, itemId, count, copy);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(managerPos);
                if (be instanceof IndexerManagerBlockEntity manager) {
                    int moved = manager.extractImmediately(itemId, count, variantStack);
                    int remaining = Math.max(0, count - moved);
                    if (remaining > 0 && !manager.isInventoryFull()) {
                        manager.queueExtraction(itemId, remaining, variantStack);
                    }
                    manager.sendItemsTo(player);
                }
            }
        });
    }
}
