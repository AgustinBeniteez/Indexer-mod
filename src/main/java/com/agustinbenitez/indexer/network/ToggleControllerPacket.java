package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.menu.IndexerControllerNetworkMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ToggleControllerPacket() implements CustomPacketPayload {
    public static final Type<ToggleControllerPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "toggle_controller"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleControllerPacket> STREAM_CODEC = StreamCodec
            .unit(new ToggleControllerPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof IndexerControllerNetworkMenu menu) {
                if (menu.getBlockEntity() != null) {
                    menu.getBlockEntity().toggleEnabled();
                }
            }
        });
    }
}