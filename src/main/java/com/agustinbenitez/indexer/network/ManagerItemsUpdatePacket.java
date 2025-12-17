package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.screen.IndexerManagerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ManagerItemsUpdatePacket {
    public static class Entry {
        public final ItemStack stackVariant;
        public final int count;
        public final boolean pending;
        public Entry(ItemStack s, int c, boolean p) { this.stackVariant = s; this.count = c; this.pending = p; }
    }

    private final List<Entry> entries;

    public ManagerItemsUpdatePacket(List<Entry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    public ManagerItemsUpdatePacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack s = buf.readItem();
            int c = buf.readInt();
            boolean p = buf.readBoolean();
            this.entries.add(new Entry(s, c, p));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entries.size());
        for (Entry e : entries) {
            buf.writeItem(e.stackVariant);
            buf.writeInt(e.count);
            buf.writeBoolean(e.pending);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof IndexerManagerScreen screen) {
                    screen.updateItemListFromServer(entries);
                }
            });
        });
        return true;
    }
}
