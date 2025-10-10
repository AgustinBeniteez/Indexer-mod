package com.agustinbenitez.indexer.network;

import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class RefreshNetworkPacket {
    private final BlockPos controllerPos;
    
    public RefreshNetworkPacket(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }
    
    public RefreshNetworkPacket(FriendlyByteBuf buf) {
        this.controllerPos = buf.readBlockPos();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(controllerPos);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity blockEntity = player.level().getBlockEntity(controllerPos);
                
                if (blockEntity instanceof IndexerControllerBlockEntity controller) {
                    // Forzar actualización de la red
                    controller.forceNetworkRefresh();
                    
                    // Enviar la lista actualizada de contenedores al cliente
                    List<IndexerControllerBlockEntity.ContainerNetworkInfo> containers = controller.getNetworkContainers();
                    ModNetworking.sendToPlayer(new ContainerListUpdatePacket(containers), player);
                }
            }
        });
        return true;
    }
}