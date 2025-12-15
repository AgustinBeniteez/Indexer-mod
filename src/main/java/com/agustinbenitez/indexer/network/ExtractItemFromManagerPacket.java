package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.block.entity.IndexerManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExtractItemFromManagerPacket {
    private final BlockPos managerPos;
    private final ResourceLocation itemId;
    private final int count;

    public ExtractItemFromManagerPacket(BlockPos managerPos, ResourceLocation itemId, int count) {
        this.managerPos = managerPos;
        this.itemId = itemId;
        this.count = count;
    }

    public ExtractItemFromManagerPacket(FriendlyByteBuf buf) {
        this.managerPos = buf.readBlockPos();
        this.itemId = buf.readResourceLocation();
        this.count = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(managerPos);
        buf.writeResourceLocation(itemId);
        buf.writeInt(count);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(managerPos);
                if (be instanceof IndexerManagerBlockEntity manager) {
                    int moved = manager.extractImmediately(itemId, count);
                    int remaining = Math.max(0, count - moved);
                    if (remaining > 0 && !manager.isInventoryFull()) {
                        manager.queueExtraction(itemId, remaining);
                    }
                    manager.sendItemsTo(player);
                }
            }
        });
        return true;
    }
}
