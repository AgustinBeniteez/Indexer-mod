package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import net.minecraftforge.event.network.CustomPayloadEvent;

@Mod.EventBusSubscriber(modid = IndexerMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModNetworking {
        public static final SimpleChannel CHANNEL = ChannelBuilder.named(
                        ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "main"))
                        .networkProtocolVersion(1)
                        .simpleChannel();

        @SubscribeEvent
        public static void register(FMLCommonSetupEvent event) {
                int id = 0;

                CHANNEL.messageBuilder(ToggleControllerPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                                .encoder((msg, buf) -> ToggleControllerPacket.STREAM_CODEC
                                                .encode((RegistryFriendlyByteBuf) buf, msg))
                                .decoder(buf -> ToggleControllerPacket.STREAM_CODEC
                                                .decode((RegistryFriendlyByteBuf) buf))
                                .consumerMainThread((msg, ctx) -> msg.handle(ctx))
                                .add();

                CHANNEL.messageBuilder(TransferAllItemsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                                .encoder((msg, buf) -> TransferAllItemsPacket.STREAM_CODEC
                                                .encode((RegistryFriendlyByteBuf) buf, msg))
                                .decoder(buf -> TransferAllItemsPacket.STREAM_CODEC
                                                .decode((RegistryFriendlyByteBuf) buf))
                                .consumerMainThread((msg, ctx) -> msg.handle(ctx))
                                .add();

                CHANNEL.messageBuilder(RefreshNetworkPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                                .encoder((msg, buf) -> RefreshNetworkPacket.STREAM_CODEC
                                                .encode((RegistryFriendlyByteBuf) buf, msg))
                                .decoder(buf -> RefreshNetworkPacket.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf))
                                .consumerMainThread((msg, ctx) -> msg.handle(ctx))
                                .add();

                CHANNEL.messageBuilder(ContainerListUpdatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                                .encoder((msg, buf) -> ContainerListUpdatePacket.STREAM_CODEC
                                                .encode((RegistryFriendlyByteBuf) buf, msg))
                                .decoder(buf -> ContainerListUpdatePacket.STREAM_CODEC
                                                .decode((RegistryFriendlyByteBuf) buf))
                                .consumerMainThread((msg, ctx) -> msg.handle(ctx))
                                .add();

                CHANNEL.messageBuilder(CustomTagFilterPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                                .encoder((msg, buf) -> CustomTagFilterPacket.STREAM_CODEC
                                                .encode((RegistryFriendlyByteBuf) buf, msg))
                                .decoder(buf -> CustomTagFilterPacket.STREAM_CODEC
                                                .decode((RegistryFriendlyByteBuf) buf))
                                .consumerMainThread((msg, ctx) -> msg.handle(ctx))
                                .add();

                CHANNEL.messageBuilder(AttributeFilterPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                                .encoder((msg, buf) -> AttributeFilterPacket.STREAM_CODEC
                                                .encode((RegistryFriendlyByteBuf) buf, msg))
                                .decoder(buf -> AttributeFilterPacket.STREAM_CODEC
                                                .decode((RegistryFriendlyByteBuf) buf))
                                .consumerMainThread((msg, ctx) -> msg.handle(ctx))
                                .add();

                CHANNEL.messageBuilder(NameFilterPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                                .encoder((msg, buf) -> NameFilterPacket.STREAM_CODEC
                                                .encode((RegistryFriendlyByteBuf) buf, msg))
                                .decoder(buf -> NameFilterPacket.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf))
                                .consumerMainThread((msg, ctx) -> msg.handle(ctx))
                                .add();

                CHANNEL.messageBuilder(RequestManagerItemsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                                .encoder((msg, buf) -> RequestManagerItemsPacket.STREAM_CODEC
                                                .encode((RegistryFriendlyByteBuf) buf, msg))
                                .decoder(buf -> RequestManagerItemsPacket.STREAM_CODEC
                                                .decode((RegistryFriendlyByteBuf) buf))
                                .consumerMainThread((msg, ctx) -> msg.handle(ctx))
                                .add();

                CHANNEL.messageBuilder(ManagerItemsUpdatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                                .encoder((msg, buf) -> ManagerItemsUpdatePacket.STREAM_CODEC
                                                .encode((RegistryFriendlyByteBuf) buf, msg))
                                .decoder(buf -> ManagerItemsUpdatePacket.STREAM_CODEC
                                                .decode((RegistryFriendlyByteBuf) buf))
                                .consumerMainThread((msg, ctx) -> msg.handle(ctx))
                                .add();

                CHANNEL.messageBuilder(ExtractItemFromManagerPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                                .encoder((msg, buf) -> ExtractItemFromManagerPacket.STREAM_CODEC
                                                .encode((RegistryFriendlyByteBuf) buf, msg))
                                .decoder(buf -> ExtractItemFromManagerPacket.STREAM_CODEC
                                                .decode((RegistryFriendlyByteBuf) buf))
                                .consumerMainThread((msg, ctx) -> msg.handle(ctx))
                                .add();

                CHANNEL.messageBuilder(CancelExtractionFromManagerPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                                .encoder((msg, buf) -> CancelExtractionFromManagerPacket.STREAM_CODEC
                                                .encode((RegistryFriendlyByteBuf) buf, msg))
                                .decoder(buf -> CancelExtractionFromManagerPacket.STREAM_CODEC
                                                .decode((RegistryFriendlyByteBuf) buf))
                                .consumerMainThread((msg, ctx) -> msg.handle(ctx))
                                .add();
        }

        public static void sendToServer(CustomPacketPayload message) {
                CHANNEL.send(message, PacketDistributor.SERVER.noArg());
        }

        public static void sendToPlayer(CustomPacketPayload message, ServerPlayer player) {
                CHANNEL.send(message, PacketDistributor.PLAYER.with(player));
        }
}
