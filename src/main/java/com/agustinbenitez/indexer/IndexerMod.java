package com.agustinbenitez.indexer;

import com.agustinbenitez.indexer.init.ModBlocks;
import com.agustinbenitez.indexer.init.ModItems;
import com.agustinbenitez.indexer.init.ModCreativeTabs;
import com.agustinbenitez.indexer.init.ModBlockEntities;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(IndexerMod.MOD_ID)
public class IndexerMod {
    public static final String MOD_ID = "indexer";
    public static final Logger LOGGER = LoggerFactory.getLogger("indexer");

    public IndexerMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register all mod objects
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}