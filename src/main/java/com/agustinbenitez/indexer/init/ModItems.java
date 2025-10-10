package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.item.*;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, IndexerMod.MOD_ID);

    // Block items
    public static final RegistryObject<Item> INDEXER_CONTROLLER_ITEM = ITEMS.register("indexer_controller",
            () -> new IndexerControllerItem(ModBlocks.INDEXER_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<Item> INDEXER_PIPE_ITEM = ITEMS.register("indexer_pipe",
            () -> new BlockItem(ModBlocks.INDEXER_PIPE.get(), new Item.Properties()));

    public static final RegistryObject<Item> INDEXER_CONNECTOR_ITEM = ITEMS.register("indexer_connector",
            () -> new IndexerConnectorItem(ModBlocks.INDEXER_CONNECTOR.get(), new Item.Properties()));
            
    public static final RegistryObject<Item> DROP_BOX_ITEM = ITEMS.register("drop_box",
            () -> new BlockItem(ModBlocks.DROP_BOX.get(), new Item.Properties()));
            
    public static final RegistryObject<Item> EXTRACTOR_ITEM = ITEMS.register("extractor",
            () -> new ExtractorItem(ModBlocks.EXTRACTOR.get(), new Item.Properties()));

    // Indexer Manual
    public static final RegistryObject<Item> INDEXER_MANUAL = ITEMS.register("indexer_manual",
            () -> new IndexerManualItem(new Item.Properties().stacksTo(1)));
            
    // Connector capacity upgrade (increases filter slots)
    public static final RegistryObject<Item> CONNECTOR_CAPACITY_UPGRADE = ITEMS.register("connector_capacity_upgrade",
            () -> new ConnectorCapacityUpgradeItem(new Item.Properties().stacksTo(1).durability(5)));

    // Transfer speed upgrades
    public static final RegistryObject<Item> TRANSFER_SPEED_UPGRADE_ZERO = ITEMS.register("transfer_speed_upgrade_zero",
            () -> new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 0, 1));
            
    public static final RegistryObject<Item> TRANSFER_SPEED_UPGRADE_BASIC = ITEMS.register("transfer_speed_upgrade_basic",
            () -> new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 1, 5));
            
    public static final RegistryObject<Item> TRANSFER_SPEED_UPGRADE_COPPER = ITEMS.register("transfer_speed_upgrade_copper",
            () -> new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 2, 10));
            
    public static final RegistryObject<Item> TRANSFER_SPEED_UPGRADE_ADVANCED = ITEMS.register("transfer_speed_upgrade_advanced",
            () -> new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 3, 20));
            
    public static final RegistryObject<Item> TRANSFER_SPEED_UPGRADE_ELITE = ITEMS.register("transfer_speed_upgrade_elite",
            () -> new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 4, 64));
            
    public static final RegistryObject<Item> TRANSFER_SPEED_UPGRADE_DEFINITIVE = ITEMS.register("transfer_speed_upgrade_definitive",
            () -> new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 5, 256));

    // Filter items
    public static final RegistryObject<Item> BASE_FILTER = ITEMS.register("base_filter",
            () -> new BaseFilterItem(new Item.Properties().stacksTo(64)));
            
    public static final RegistryObject<Item> TOOLS_FILTER = ITEMS.register("tools_filter",
            () -> new ToolsFilterItem(new Item.Properties().stacksTo(64)));
            
    public static final RegistryObject<Item> FOOD_FILTER = ITEMS.register("food_filter",
            () -> new FoodFilterItem(new Item.Properties().stacksTo(64)));
            
    public static final RegistryObject<Item> CUSTOM_TAG_BLOCKER = ITEMS.register("custom_tag_blocker",
            () -> new CustomTagFilterItem(new Item.Properties().stacksTo(64)));
    
    public static final RegistryObject<Item> FUEL_FILTER = ITEMS.register("fuel_filter",
            () -> new FuelFilterItem(new Item.Properties()));
            
    public static final RegistryObject<Item> ATTRIBUTE_FILTER = ITEMS.register("attribute_filter",
            () -> new AttributeFilterItem(new Item.Properties().stacksTo(64)));
            
    public static final RegistryObject<Item> NAME_FILTER = ITEMS.register("name_filter",
            () -> new NameFilterItem(new Item.Properties().stacksTo(64)));
            
    // MOD_FILTER eliminado



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}