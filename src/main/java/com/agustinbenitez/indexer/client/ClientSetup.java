package com.agustinbenitez.indexer.client;

import com.agustinbenitez.indexer.init.ModMenuTypes;
import com.agustinbenitez.indexer.screen.DropBoxScreen;
import com.agustinbenitez.indexer.screen.IndexerConnectorScreen;

import com.agustinbenitez.indexer.screen.IndexerControllerNetworkScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "indexer", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Registrar la pantalla de red del controlador
            MenuScreens.register(ModMenuTypes.INDEXER_CONTROLLER_NETWORK_MENU.get(), IndexerControllerNetworkScreen::new);
            // Registrar la pantalla del DropBox
            MenuScreens.register(ModMenuTypes.DROP_BOX_MENU.get(), DropBoxScreen::new);
            // Registrar la pantalla del conector
            MenuScreens.register(ModMenuTypes.INDEXER_CONNECTOR_MENU.get(), IndexerConnectorScreen::new);
        });
    }
}