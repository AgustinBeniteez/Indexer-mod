package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.screen.IndexerControllerNetworkScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ContainerListUpdatePacket(List<NetworkContainerData> containers) implements CustomPacketPayload {
    public static final Type<ContainerListUpdatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "container_list_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ContainerListUpdatePacket> STREAM_CODEC = StreamCodec
            .composite(
                    NetworkContainerData.STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list()),
                    ContainerListUpdatePacket::containers,
                    ContainerListUpdatePacket::new);

    public static ContainerListUpdatePacket create(
            List<IndexerControllerBlockEntity.ContainerNetworkInfo> networkContainers) {
        return new ContainerListUpdatePacket(convert(networkContainers));
    }

    private static List<NetworkContainerData> convert(
            List<IndexerControllerBlockEntity.ContainerNetworkInfo> networkContainers) {
        List<NetworkContainerData> list = new ArrayList<>();
        for (IndexerControllerBlockEntity.ContainerNetworkInfo info : networkContainers) {
            list.add(new NetworkContainerData(info));
        }
        return list;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof IndexerControllerNetworkScreen screen) {
                    screen.updateContainerListFromServer(containers);
                }
            });
        });
    }

    public static class NetworkContainerData {
        public BlockPos position;
        public String containerType;
        public int itemCount;
        public int maxSlots;
        public List<ItemStack> filters;
        public Map<String, Integer> uniqueItems;

        public static final StreamCodec<RegistryFriendlyByteBuf, NetworkContainerData> STREAM_CODEC = StreamCodec
                .of((buf, data) -> data.toBuffer(buf), NetworkContainerData::fromBuffer);

        public NetworkContainerData(IndexerControllerBlockEntity.ContainerNetworkInfo info) {
            this.position = info.position;
            this.containerType = info.containerType;
            this.itemCount = info.itemCount;
            this.maxSlots = info.maxSlots;
            this.filters = new ArrayList<>(info.filters);
            this.uniqueItems = new HashMap<>(info.uniqueItems);
        }

        public NetworkContainerData() {
            this.filters = new ArrayList<>();
            this.uniqueItems = new HashMap<>();
        }

        public void toBuffer(RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(position);
            buf.writeUtf(containerType);
            buf.writeInt(itemCount);
            buf.writeInt(maxSlots);
            buf.writeInt(filters.size());
            for (ItemStack filter : filters) {
                ItemStack.STREAM_CODEC.encode(buf, filter);
            }
            buf.writeInt(uniqueItems.size());
            for (Map.Entry<String, Integer> entry : uniqueItems.entrySet()) {
                buf.writeUtf(entry.getKey());
                buf.writeInt(entry.getValue());
            }
        }

        public static NetworkContainerData fromBuffer(RegistryFriendlyByteBuf buf) {
            NetworkContainerData data = new NetworkContainerData();
            data.position = buf.readBlockPos();
            data.containerType = buf.readUtf();
            data.itemCount = buf.readInt();
            data.maxSlots = buf.readInt();
            int filterCount = buf.readInt();
            data.filters = new ArrayList<>();
            for (int i = 0; i < filterCount; i++) {
                data.filters.add(ItemStack.STREAM_CODEC.decode(buf));
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