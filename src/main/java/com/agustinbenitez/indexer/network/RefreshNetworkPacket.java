package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.List;

public record RefreshNetworkPacket(BlockPos controllerPos) implements CustomPacketPayload {
    public static final Type<RefreshNetworkPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "refresh_network"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RefreshNetworkPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RefreshNetworkPacket::controllerPos,
            RefreshNetworkPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity blockEntity = player.level().getBlockEntity(controllerPos);

                if (blockEntity instanceof IndexerControllerBlockEntity controller) {
                    // Force network refresh
                    controller.forceNetworkRefresh();

                    // Send updated container list to client
                    List<IndexerControllerBlockEntity.ContainerNetworkInfo> containers = controller
                            .getNetworkContainers();
                    ModNetworking.sendToPlayer(ContainerListUpdatePacket.create(containers), player);
                }
            }
        });
    }
}