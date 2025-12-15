package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.screen.IndexerManagerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ManagerItemsUpdatePacket {
    private final Map<String, Integer> items;

    public ManagerItemsUpdatePacket(Map<String, Integer> items) {
        this.items = new HashMap<>(items);
    }

    public ManagerItemsUpdatePacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.items = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            int count = buf.readInt();
            this.items.put(id, count);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(items.size());
        for (Map.Entry<String, Integer> e : items.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeInt(e.getValue());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof IndexerManagerScreen screen) {
                    screen.updateItemListFromServer(items);
                }
            });
        });
        return true;
    }
}
