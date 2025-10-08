package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;
    
    private static int id() {
        return packetId++;
    }
    
    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(IndexerMod.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();
        
        INSTANCE = net;
        
        // Registrar el paquete ToggleControllerPacket
        net.messageBuilder(ToggleControllerPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ToggleControllerPacket::new)
                .encoder(ToggleControllerPacket::toBytes)
                .consumerMainThread(ToggleControllerPacket::handle)
                .add();
        
        // Registrar el paquete TransferAllItemsPacket
        net.messageBuilder(TransferAllItemsPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(TransferAllItemsPacket::new)
                .encoder(TransferAllItemsPacket::toBytes)
                .consumerMainThread(TransferAllItemsPacket::handle)
                .add();
        
        // Registrar el paquete RefreshNetworkPacket
        net.messageBuilder(RefreshNetworkPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RefreshNetworkPacket::new)
                .encoder(RefreshNetworkPacket::toBytes)
                .consumerMainThread(RefreshNetworkPacket::handle)
                .add();
        
        // Registrar el paquete ContainerListUpdatePacket
        net.messageBuilder(ContainerListUpdatePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ContainerListUpdatePacket::new)
                .encoder(ContainerListUpdatePacket::toBytes)
                .consumerMainThread(ContainerListUpdatePacket::handle)
                .add();
        
        // Registrar el paquete CustomTagFilterPacket
        net.messageBuilder(CustomTagFilterPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CustomTagFilterPacket::new)
                .encoder(CustomTagFilterPacket::toBytes)
                .consumerMainThread(CustomTagFilterPacket::handle)
                .add();
    }
    
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
    
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}