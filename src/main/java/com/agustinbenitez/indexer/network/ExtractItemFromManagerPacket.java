package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.block.entity.IndexerManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExtractItemFromManagerPacket {
    private final BlockPos managerPos;
    private final ResourceLocation itemId;
    private final int count;
    private final ItemStack variantStack;

    public ExtractItemFromManagerPacket(BlockPos managerPos, ResourceLocation itemId, int count, ItemStack variantStack) {
        this.managerPos = managerPos;
        this.itemId = itemId;
        this.count = count;
        this.variantStack = variantStack.copy();
        this.variantStack.setCount(1);
    }

    public ExtractItemFromManagerPacket(FriendlyByteBuf buf) {
        this.managerPos = buf.readBlockPos();
        this.itemId = buf.readResourceLocation();
        this.count = buf.readInt();
        this.variantStack = buf.readItem();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(managerPos);
        buf.writeResourceLocation(itemId);
        buf.writeInt(count);
        buf.writeItem(variantStack);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(managerPos);
                if (be instanceof IndexerManagerBlockEntity manager) {
                    int moved = manager.extractImmediately(itemId, count, variantStack);
                    int remaining = Math.max(0, count - moved);
                    if (remaining > 0 && !manager.isInventoryFull()) {
                        manager.queueExtraction(itemId, remaining, variantStack);
                    }
                    manager.sendItemsTo(player);
                }
            }
        });
        return true;
    }
}
