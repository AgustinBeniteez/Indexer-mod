package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.item.AttributeFilterItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AttributeFilterPacket {
    private final int slotIndex;
    private final String attribute;
    
    public AttributeFilterPacket(int slotIndex, String attribute) {
        this.slotIndex = slotIndex;
        this.attribute = attribute;
    }
    
    public AttributeFilterPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readInt();
        this.attribute = buf.readUtf();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.slotIndex);
        buf.writeUtf(this.attribute);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack itemStack = player.getInventory().getItem(this.slotIndex);
                
                if (itemStack.getItem() instanceof AttributeFilterItem) {
                    CompoundTag tag = itemStack.getOrCreateTag();
                    tag.putString("attribute_filter", this.attribute);
                    
                    player.sendSystemMessage(Component.translatable("message.indexer.attribute_filter.attribute_updated", this.attribute));
                }
            }
        });
        return true;
    }
}