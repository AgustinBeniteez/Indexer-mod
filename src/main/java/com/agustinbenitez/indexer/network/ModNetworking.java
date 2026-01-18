package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class ModNetworking {
    
    public static void register() {
        // Register C2S Packets
        PayloadTypeRegistry.playC2S().register(ToggleControllerPacket.ID, ToggleControllerPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(TransferAllItemsPacket.ID, TransferAllItemsPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RefreshNetworkPacket.ID, RefreshNetworkPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CustomTagFilterPacket.ID, CustomTagFilterPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AttributeFilterPacket.ID, AttributeFilterPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(NameFilterPacket.ID, NameFilterPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestManagerItemsPacket.ID, RequestManagerItemsPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ExtractItemFromManagerPacket.ID, ExtractItemFromManagerPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CancelExtractionFromManagerPacket.ID, CancelExtractionFromManagerPacket.CODEC);
        
        // Register S2C Packets
        PayloadTypeRegistry.playS2C().register(ContainerListUpdatePacket.ID, ContainerListUpdatePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ManagerItemsUpdatePacket.ID, ManagerItemsUpdatePacket.CODEC);

        // Register Server Receivers
        ServerPlayNetworking.registerGlobalReceiver(ToggleControllerPacket.ID, ToggleControllerPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(TransferAllItemsPacket.ID, TransferAllItemsPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(RefreshNetworkPacket.ID, RefreshNetworkPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(CustomTagFilterPacket.ID, CustomTagFilterPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(AttributeFilterPacket.ID, AttributeFilterPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(NameFilterPacket.ID, NameFilterPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(RequestManagerItemsPacket.ID, RequestManagerItemsPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(ExtractItemFromManagerPacket.ID, ExtractItemFromManagerPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(CancelExtractionFromManagerPacket.ID, CancelExtractionFromManagerPacket::handle);
    }
    
    public static void sendToPlayer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload, ServerPlayer player) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
