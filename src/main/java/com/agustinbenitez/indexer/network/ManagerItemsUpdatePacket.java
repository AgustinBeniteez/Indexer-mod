package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record ManagerItemsUpdatePacket(List<Entry> entries) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ManagerItemsUpdatePacket> ID = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "manager_items_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManagerItemsUpdatePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, Entry.STREAM_CODEC), ManagerItemsUpdatePacket::entries,
            ManagerItemsUpdatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static class Entry {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.of(
                Entry::encode,
                Entry::decode);

        public final ItemStack stackVariant;
        public final int count;
        public final boolean pending;

        public Entry(ItemStack s, int c, boolean p) {
            this.stackVariant = s;
            this.count = c;
            this.pending = p;
        }

        private static void encode(RegistryFriendlyByteBuf buf, Entry entry) {
            ItemStack.STREAM_CODEC.encode(buf, entry.stackVariant);
            buf.writeInt(entry.count);
            buf.writeBoolean(entry.pending);
        }

        private static Entry decode(RegistryFriendlyByteBuf buf) {
            ItemStack s = ItemStack.STREAM_CODEC.decode(buf);
            int c = buf.readInt();
            boolean p = buf.readBoolean();
            return new Entry(s, c, p);
        }
    }
}
