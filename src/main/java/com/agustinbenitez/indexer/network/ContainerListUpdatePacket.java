package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.screen.IndexerControllerNetworkScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ContainerListUpdatePacket(List<ContainerData> containers) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<ContainerListUpdatePacket> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "container_list_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContainerListUpdatePacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.collection(ArrayList::new, ContainerData.STREAM_CODEC), ContainerListUpdatePacket::containers,
        ContainerListUpdatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ContainerListUpdatePacket payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            Minecraft mc = context.client();
            if (mc.screen instanceof IndexerControllerNetworkScreen screen) {
                screen.updateContainerListFromServer(payload.containers());
            }
        });
    }
    
    public static class ContainerData {
        public static final StreamCodec<RegistryFriendlyByteBuf, ContainerData> STREAM_CODEC = StreamCodec.of(
            ContainerData::encode,
            ContainerData::decode
        );

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
        
        private static void encode(RegistryFriendlyByteBuf buf, ContainerData data) {
            BlockPos.STREAM_CODEC.encode(buf, data.position);
            buf.writeUtf(data.containerType);
            buf.writeInt(data.itemCount);
            buf.writeInt(data.maxSlots);
            buf.writeInt(data.filters.size());
            for (ItemStack filter : data.filters) {
                ItemStack.STREAM_CODEC.encode(buf, filter);
            }
            buf.writeInt(data.uniqueItems.size());
            for (Map.Entry<String, Integer> entry : data.uniqueItems.entrySet()) {
                buf.writeUtf(entry.getKey());
                buf.writeInt(entry.getValue());
            }
        }
        
        private static ContainerData decode(RegistryFriendlyByteBuf buf) {
            ContainerData data = new ContainerData();
            data.position = BlockPos.STREAM_CODEC.decode(buf);
            data.containerType = buf.readUtf();
            data.itemCount = buf.readInt();
            data.maxSlots = buf.readInt();
            int filterCount = buf.readInt();
            data.filters = new ArrayList<>();
            for (int i = 0; i < filterCount; i++) {
                ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
                data.filters.add(stack);
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
