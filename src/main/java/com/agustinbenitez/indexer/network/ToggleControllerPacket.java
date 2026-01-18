package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record ToggleControllerPacket() implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<ToggleControllerPacket> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "toggle_controller"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleControllerPacket> CODEC = StreamCodec.unit(new ToggleControllerPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ToggleControllerPacket payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player != null && player.containerMenu instanceof com.agustinbenitez.indexer.menu.IndexerControllerNetworkMenu menu) {
                if (menu.getBlockEntity() != null) {
                    menu.getBlockEntity().toggleEnabled();
                }
            }
        });
    }
}
