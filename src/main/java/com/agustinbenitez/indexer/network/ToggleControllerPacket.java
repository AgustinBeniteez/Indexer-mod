package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleControllerPacket {
    
    public ToggleControllerPacket() {
        // Constructor vacío para el paquete
    }
    
    public ToggleControllerPacket(FriendlyByteBuf buf) {
        // No hay datos que leer
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        // No hay datos que escribir
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof com.agustinbenitez.indexer.menu.IndexerControllerMenu menu) {
                menu.toggleEnabled();
            }
        });
        return true;
    }
}