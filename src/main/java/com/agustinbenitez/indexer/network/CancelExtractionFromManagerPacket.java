package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.entity.IndexerManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record CancelExtractionFromManagerPacket(BlockPos managerPos, ItemStack variantStack)
        implements CustomPacketPayload {
    public static final Type<CancelExtractionFromManagerPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "cancel_extraction"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CancelExtractionFromManagerPacket> STREAM_CODEC = StreamCodec
            .composite(
                    BlockPos.STREAM_CODEC, CancelExtractionFromManagerPacket::managerPos,
                    ItemStack.STREAM_CODEC, CancelExtractionFromManagerPacket::variantStack,
                    CancelExtractionFromManagerPacket::new);

    public static CancelExtractionFromManagerPacket create(BlockPos managerPos, ItemStack variantStack) {
        ItemStack copy = variantStack.copy();
        copy.setCount(1);
        return new CancelExtractionFromManagerPacket(managerPos, copy);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context) {
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
    }
}
