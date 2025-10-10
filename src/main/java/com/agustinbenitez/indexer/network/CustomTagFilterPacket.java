package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.item.CustomTagFilterItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CustomTagFilterPacket {
    private final int slotIndex;
    private final String customTag;
    
    public CustomTagFilterPacket(int slotIndex, String customTag) {
        this.slotIndex = slotIndex;
        this.customTag = customTag;
    }
    
    public CustomTagFilterPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readInt();
        this.customTag = buf.readUtf();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.slotIndex);
        buf.writeUtf(this.customTag);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack itemStack = player.getInventory().getItem(this.slotIndex);
                
                if (itemStack.getItem() instanceof CustomTagFilterItem) {
                    CompoundTag tag = itemStack.getOrCreateTag();
                    tag.putString("custom_tag", this.customTag);
                    
                    player.sendSystemMessage(Component.translatable("message.indexer.custom_filter.tag_updated", this.customTag));
                }
            }
        });
        return true;
    }
}