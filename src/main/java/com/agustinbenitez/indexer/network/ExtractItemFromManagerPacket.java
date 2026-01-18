package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.entity.IndexerManagerBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public record ExtractItemFromManagerPacket(BlockPos managerPos, ResourceLocation itemId, int count, ItemStack variantStack) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<ExtractItemFromManagerPacket> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "extract_item_from_manager"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractItemFromManagerPacket> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, ExtractItemFromManagerPacket::managerPos,
        ResourceLocation.STREAM_CODEC, ExtractItemFromManagerPacket::itemId,
        ByteBufCodecs.INT, ExtractItemFromManagerPacket::count,
        ItemStack.STREAM_CODEC, ExtractItemFromManagerPacket::variantStack,
        ExtractItemFromManagerPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ExtractItemFromManagerPacket payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(payload.managerPos());
                if (be instanceof IndexerManagerBlockEntity manager) {
                    int moved = manager.extractImmediately(payload.itemId(), payload.count(), payload.variantStack());
                    int remaining = Math.max(0, payload.count() - moved);
                    if (remaining > 0 && !manager.isInventoryFull()) {
                        manager.queueExtraction(payload.itemId(), remaining, payload.variantStack());
                    }
                    manager.sendItemsTo(player);
                }
            }
        });
    }
}
