package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.item.NameFilterItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record NameFilterPacket(int slotIndex, String customName) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<NameFilterPacket> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "name_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NameFilterPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, NameFilterPacket::slotIndex,
        ByteBufCodecs.STRING_UTF8, NameFilterPacket::customName,
        NameFilterPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(NameFilterPacket payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player != null) {
                ItemStack itemStack = player.getInventory().getItem(payload.slotIndex());
                
                if (itemStack.getItem() instanceof NameFilterItem) {
                    CustomData customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                    CompoundTag tag = customData.copyTag();
                    tag.putString("custom_name", payload.customName());
                    itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    
                    player.sendSystemMessage(Component.translatable("message.indexer.name_filter.name_updated", payload.customName()));
                }
            }
        });
    }
}
