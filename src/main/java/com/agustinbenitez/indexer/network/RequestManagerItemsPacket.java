package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.block.entity.IndexerManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestManagerItemsPacket {
    private final BlockPos managerPos;

    public RequestManagerItemsPacket(BlockPos managerPos) {
        this.managerPos = managerPos;
    }

    public RequestManagerItemsPacket(FriendlyByteBuf buf) {
        this.managerPos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(managerPos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(managerPos);
                if (be instanceof IndexerManagerBlockEntity manager) {
                    manager.sendItemsTo(player);
                }
            }
        });
        return true;
    }
}
