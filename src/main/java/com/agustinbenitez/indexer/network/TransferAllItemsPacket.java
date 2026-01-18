package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.menu.DropBoxMenu;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record TransferAllItemsPacket() implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<TransferAllItemsPacket> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "transfer_all_items"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TransferAllItemsPacket> CODEC = StreamCodec.unit(new TransferAllItemsPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(TransferAllItemsPacket payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player != null && player.containerMenu instanceof DropBoxMenu menu) {
                menu.transferAllItemsToDropBox(player);
            }
        });
    }
}
