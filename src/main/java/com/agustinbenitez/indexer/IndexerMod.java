package com.agustinbenitez.indexer;

import com.agustinbenitez.indexer.init.ModBlocks;
import com.agustinbenitez.indexer.init.ModItems;
import com.agustinbenitez.indexer.init.ModCreativeTabs;
import com.agustinbenitez.indexer.init.ModBlockEntities;
import com.agustinbenitez.indexer.init.ModMenuTypes;
import com.agustinbenitez.indexer.network.ModNetworking;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexerMod implements ModInitializer {
    public static final String MOD_ID = "indexer";
    public static final Logger LOGGER = LoggerFactory.getLogger("indexer");

    @Override
    public void onInitialize() {
        // Register all mod objects
        ModBlocks.register();
        ModItems.register();
        ModCreativeTabs.register();
        ModBlockEntities.register();
        ModMenuTypes.register();
        
        // Register networking
        ModNetworking.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}