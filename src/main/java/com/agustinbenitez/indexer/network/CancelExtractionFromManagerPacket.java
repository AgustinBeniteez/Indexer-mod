package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.entity.IndexerManagerBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public record CancelExtractionFromManagerPacket(BlockPos managerPos, ItemStack variantStack) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<CancelExtractionFromManagerPacket> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "cancel_extraction_from_manager"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CancelExtractionFromManagerPacket> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, CancelExtractionFromManagerPacket::managerPos,
        ItemStack.STREAM_CODEC, CancelExtractionFromManagerPacket::variantStack,
        CancelExtractionFromManagerPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(CancelExtractionFromManagerPacket payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(payload.managerPos());
                if (be instanceof IndexerManagerBlockEntity manager) {
                    manager.cancelPendingExtraction(payload.variantStack());
                    manager.sendItemsTo(player);
                }
            }
        });
    }
}
