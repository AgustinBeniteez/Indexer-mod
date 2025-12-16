package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.block.entity.IndexerManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CancelExtractionFromManagerPacket {
    private final BlockPos managerPos;
    private final ItemStack variantStack;

    public CancelExtractionFromManagerPacket(BlockPos managerPos, ItemStack variantStack) {
        this.managerPos = managerPos;
        this.variantStack = variantStack.copy();
        this.variantStack.setCount(1);
    }

    public CancelExtractionFromManagerPacket(FriendlyByteBuf buf) {
        this.managerPos = buf.readBlockPos();
        this.variantStack = buf.readItem();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(managerPos);
        buf.writeItem(variantStack);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(managerPos);
                if (be instanceof IndexerManagerBlockEntity manager) {
                    manager.cancelPendingExtraction(variantStack);
                    manager.sendItemsTo(player);
                }
            }
        });
        return true;
    }
}
