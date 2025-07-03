package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.item.IndexerManualItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, IndexerMod.MOD_ID);

    // Ítems de bloque
    public static final RegistryObject<Item> INDEXER_CONTROLLER_ITEM = ITEMS.register("indexer_controller",
            () -> new BlockItem(ModBlocks.INDEXER_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<Item> INDEXER_PIPE_ITEM = ITEMS.register("indexer_pipe",
            () -> new BlockItem(ModBlocks.INDEXER_PIPE.get(), new Item.Properties()));

    public static final RegistryObject<Item> INDEXER_CONNECTOR_ITEM = ITEMS.register("indexer_connector",
            () -> new BlockItem(ModBlocks.INDEXER_CONNECTOR.get(), new Item.Properties()));

    // Manual del Indexer
    public static final RegistryObject<Item> INDEXER_MANUAL = ITEMS.register("indexer_manual",
            () -> new IndexerManualItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}