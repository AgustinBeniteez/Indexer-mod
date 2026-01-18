package com.agustinbenitez.indexer.client;

import com.agustinbenitez.indexer.init.ModMenuTypes;
import com.agustinbenitez.indexer.network.ContainerListUpdatePacket;
import com.agustinbenitez.indexer.network.ManagerItemsUpdatePacket;
import com.agustinbenitez.indexer.screen.DropBoxScreen;
import com.agustinbenitez.indexer.screen.IndexerConnectorScreen;
import com.agustinbenitez.indexer.screen.IndexerControllerNetworkScreen;
import com.agustinbenitez.indexer.screen.IndexerManagerScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public class ClientSetup implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // Registrar la pantalla de red del controlador
        MenuScreens.register(ModMenuTypes.INDEXER_CONTROLLER_NETWORK_MENU, IndexerControllerNetworkScreen::new);
        // Registrar la pantalla del DropBox
        MenuScreens.register(ModMenuTypes.DROP_BOX_MENU, DropBoxScreen::new);
        // Registrar la pantalla del conector
        MenuScreens.register(ModMenuTypes.INDEXER_CONNECTOR_MENU, IndexerConnectorScreen::new);
        // Registrar la pantalla del Manager
        MenuScreens.register(ModMenuTypes.INDEXER_MANAGER_MENU, IndexerManagerScreen::new);

        // Register S2C Receivers
        ClientPlayNetworking.registerGlobalReceiver(ContainerListUpdatePacket.ID, ContainerListUpdatePacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(ManagerItemsUpdatePacket.ID, ManagerItemsUpdatePacket::handle);
    }

    public static void openManualScreen() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new com.agustinbenitez.indexer.screen.IndexerManualScreen());
    }
}
