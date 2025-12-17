package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.init.ModDataComponents;
import com.agustinbenitez.indexer.item.NameFilterItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record NameFilterPacket(int slotIndex, String customName) implements CustomPacketPayload {
    public static final Type<NameFilterPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "name_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NameFilterPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, NameFilterPacket::slotIndex,
            ByteBufCodecs.STRING_UTF8, NameFilterPacket::customName,
            NameFilterPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack itemStack = player.getInventory().getItem(this.slotIndex);

                if (itemStack.getItem() instanceof NameFilterItem) {
                    itemStack.set(ModDataComponents.CUSTOM_NAME.get(), this.customName);

                    player.sendSystemMessage(
                            Component.translatable("message.indexer.name_filter.name_updated", this.customName));
                }
            }
        });
    }
}