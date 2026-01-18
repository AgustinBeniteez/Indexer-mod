package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public record RefreshNetworkPacket(BlockPos controllerPos) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<RefreshNetworkPacket> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "refresh_network"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RefreshNetworkPacket> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, RefreshNetworkPacket::controllerPos,
        RefreshNetworkPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(RefreshNetworkPacket payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player != null) {
                BlockEntity blockEntity = player.level().getBlockEntity(payload.controllerPos());
                
                if (blockEntity instanceof IndexerControllerBlockEntity controller) {
                    controller.forceNetworkRefresh();
                    
                    List<IndexerControllerBlockEntity.ContainerNetworkInfo> containers = controller.getNetworkContainers();
                    java.util.List<ContainerListUpdatePacket.ContainerData> containerDataList = new java.util.ArrayList<>();
                    for (IndexerControllerBlockEntity.ContainerNetworkInfo info : containers) {
                        containerDataList.add(new ContainerListUpdatePacket.ContainerData(info));
                    }
                    ModNetworking.sendToPlayer(new ContainerListUpdatePacket(containerDataList), player);
                }
            }
        });
    }
}
