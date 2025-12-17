package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.screen.IndexerManagerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

public record ManagerItemsUpdatePacket(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<ManagerItemsUpdatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "manager_items_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManagerItemsUpdatePacket> STREAM_CODEC = StreamCodec
            .composite(
                    Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), ManagerItemsUpdatePacket::entries,
                    ManagerItemsUpdatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof IndexerManagerScreen screen) {
                    screen.updateItemListFromServer(entries);
                }
            });
        });
    }

    public record Entry(ItemStack stackVariant, int count, boolean pending) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Entry::stackVariant,
                ByteBufCodecs.INT, Entry::count,
                ByteBufCodecs.BOOL, Entry::pending,
                Entry::new);
    }
}
