package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.screen.IndexerControllerNetworkScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ContainerListUpdatePacket {
    private final List<ContainerData> containers;
    
    public ContainerListUpdatePacket(List<IndexerControllerBlockEntity.ContainerNetworkInfo> networkContainers) {
        this.containers = new ArrayList<>();
        for (IndexerControllerBlockEntity.ContainerNetworkInfo info : networkContainers) {
            this.containers.add(new ContainerData(info));
        }
    }
    
    public ContainerListUpdatePacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.containers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            this.containers.add(ContainerData.fromBuffer(buf));
        }
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(containers.size());
        for (ContainerData container : containers) {
            container.toBuffer(buf);
        }
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof IndexerControllerNetworkScreen screen) {
                    screen.updateContainerListFromServer(containers);
                }
            });
        });
        return true;
    }
    
    public static class ContainerData {
        public BlockPos position;
        public String containerType;
        public int itemCount;
        public int maxSlots;
        public List<ItemStack> filters;
        public Map<String, Integer> uniqueItems;
        
        public ContainerData(IndexerControllerBlockEntity.ContainerNetworkInfo info) {
            this.position = info.position;
            this.containerType = info.containerType;
            this.itemCount = info.itemCount;
            this.maxSlots = info.maxSlots;
            this.filters = new ArrayList<>(info.filters);
            this.uniqueItems = new HashMap<>(info.uniqueItems);
        }
        
        public ContainerData() {
            this.filters = new ArrayList<>();
            this.uniqueItems = new HashMap<>();
        }
        
        public void toBuffer(FriendlyByteBuf buf) {
            buf.writeBlockPos(position);
            buf.writeUtf(containerType);
            buf.writeInt(itemCount);
            buf.writeInt(maxSlots);
            buf.writeInt(filters.size());
            for (ItemStack filter : filters) {
                buf.writeItem(filter);
            }
            buf.writeInt(uniqueItems.size());
            for (Map.Entry<String, Integer> entry : uniqueItems.entrySet()) {
                buf.writeUtf(entry.getKey());
                buf.writeInt(entry.getValue());
            }
        }
        
        public static ContainerData fromBuffer(FriendlyByteBuf buf) {
            ContainerData data = new ContainerData();
            data.position = buf.readBlockPos();
            data.containerType = buf.readUtf();
            data.itemCount = buf.readInt();
            data.maxSlots = buf.readInt();
            int filterCount = buf.readInt();
            data.filters = new ArrayList<>();
            for (int i = 0; i < filterCount; i++) {
                data.filters.add(buf.readItem());
            }
            int uniqueItemsCount = buf.readInt();
            data.uniqueItems = new HashMap<>();
            for (int i = 0; i < uniqueItemsCount; i++) {
                String itemName = buf.readUtf();
                int quantity = buf.readInt();
                data.uniqueItems.put(itemName, quantity);
            }
            return data;
        }
    }
}