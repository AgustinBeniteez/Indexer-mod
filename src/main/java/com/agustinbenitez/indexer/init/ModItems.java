package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.item.IndexerManualItem;
import com.agustinbenitez.indexer.item.TransferSpeedUpgradeItem;

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
            
    public static final RegistryObject<Item> DROP_BOX_ITEM = ITEMS.register("drop_box",
            () -> new BlockItem(ModBlocks.DROP_BOX.get(), new Item.Properties()));

    // Manual del Indexer
    public static final RegistryObject<Item> INDEXER_MANUAL = ITEMS.register("indexer_manual",
            () -> new IndexerManualItem(new Item.Properties().stacksTo(1)));
            
    // Mejoras de velocidad de transferencia
    public static final RegistryObject<Item> TRANSFER_SPEED_UPGRADE_BASIC = ITEMS.register("transfer_speed_upgrade_basic",
            () -> new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 1, 4));
            
    public static final RegistryObject<Item> TRANSFER_SPEED_UPGRADE_ADVANCED = ITEMS.register("transfer_speed_upgrade_advanced",
            () -> new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 2, 10));
            
    public static final RegistryObject<Item> TRANSFER_SPEED_UPGRADE_ELITE = ITEMS.register("transfer_speed_upgrade_elite",
            () -> new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 3, 20));



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}