package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.init.ModDataComponents;
import com.agustinbenitez.indexer.item.CustomTagFilterItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record CustomTagFilterPacket(int slotIndex, String customTag) implements CustomPacketPayload {
    public static final Type<CustomTagFilterPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "custom_tag_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomTagFilterPacket> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.INT, CustomTagFilterPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8, CustomTagFilterPacket::customTag,
                    CustomTagFilterPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack itemStack = player.getInventory().getItem(this.slotIndex);

                if (itemStack.getItem() instanceof CustomTagFilterItem) {
                    itemStack.set(ModDataComponents.CUSTOM_TAG.get(), this.customTag);

                    player.sendSystemMessage(
                            Component.translatable("message.indexer.custom_filter.tag_updated", this.customTag));
                }
            }
        });
    }
}