package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.menu.DropBoxMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TransferAllItemsPacket {
    
    public TransferAllItemsPacket() {
        // Constructor vacío para el paquete
    }
    
    public TransferAllItemsPacket(FriendlyByteBuf buf) {
        // No hay datos que leer
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        // No hay datos que escribir
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof DropBoxMenu menu) {
                menu.transferAllItemsToDropBox(player);
            }
        });
        return true;
    }
}