package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.init.ModDataComponents;
import com.agustinbenitez.indexer.item.AttributeFilterItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record AttributeFilterPacket(int slotIndex, String attribute) implements CustomPacketPayload {
    public static final Type<AttributeFilterPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "attribute_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttributeFilterPacket> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.INT, AttributeFilterPacket::slotIndex,
                    ByteBufCodecs.STRING_UTF8, AttributeFilterPacket::attribute,
                    AttributeFilterPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack itemStack = player.getInventory().getItem(this.slotIndex);

                if (itemStack.getItem() instanceof AttributeFilterItem) {
                    itemStack.set(ModDataComponents.ATTRIBUTE_FILTER.get(), this.attribute);

                    player.sendSystemMessage(Component
                            .translatable("message.indexer.attribute_filter.attribute_updated", this.attribute));
                }
            }
        });
    }
}