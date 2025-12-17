package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.menu.DropBoxMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record TransferAllItemsPacket() implements CustomPacketPayload {
    public static final Type<TransferAllItemsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "transfer_all_items"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferAllItemsPacket> STREAM_CODEC = StreamCodec
            .unit(new TransferAllItemsPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof DropBoxMenu menu) {
                menu.transferAllItemsToDropBox(player);
            }
        });
    }
}