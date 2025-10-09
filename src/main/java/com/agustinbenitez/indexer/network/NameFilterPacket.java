package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.item.NameFilterItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NameFilterPacket {
    private final int slotIndex;
    private final String customName;
    
    public NameFilterPacket(int slotIndex, String customName) {
        this.slotIndex = slotIndex;
        this.customName = customName;
    }
    
    public NameFilterPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readInt();
        this.customName = buf.readUtf();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.slotIndex);
        buf.writeUtf(this.customName);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack itemStack = player.getInventory().getItem(this.slotIndex);
                
                if (itemStack.getItem() instanceof NameFilterItem) {
                    CompoundTag tag = itemStack.getOrCreateTag();
                    tag.putString("custom_name", this.customName);
                    
                    player.sendSystemMessage(Component.translatable("message.indexer.name_filter.name_updated", this.customName));
                }
            }
        });
        return true;
    }
}