package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.item.*;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ModItems {

    // Block items
    public static final Item INDEXER_CONTROLLER_ITEM = register("indexer_controller",
            new IndexerControllerItem(ModBlocks.INDEXER_CONTROLLER, new Item.Properties()));

    public static final Item INDEXER_PIPE_ITEM = register("indexer_pipe",
            new BlockItem(ModBlocks.INDEXER_PIPE, new Item.Properties()));

    public static final Item INDEXER_CONNECTOR_ITEM = register("indexer_connector",
            new IndexerConnectorItem(ModBlocks.INDEXER_CONNECTOR, new Item.Properties()));
            
    public static final Item DROP_BOX_ITEM = register("drop_box",
            new BlockItem(ModBlocks.DROP_BOX, new Item.Properties()));
            
    public static final Item EXTRACTOR_ITEM = register("extractor",
            new ExtractorItem(ModBlocks.EXTRACTOR, new Item.Properties()));
    
    public static final Item INDEXER_MANAGER_ITEM = register("indexer_manager",
            new BlockItem(ModBlocks.INDEXER_MANAGER, new Item.Properties()));

    // Indexer Manual
    public static final Item INDEXER_MANUAL = register("indexer_manual",
            new IndexerManualItem(new Item.Properties().stacksTo(1)));
            
    // Connector capacity upgrade (increases filter slots)
    public static final Item CONNECTOR_CAPACITY_UPGRADE = register("connector_capacity_upgrade",
            new ConnectorCapacityUpgradeItem(new Item.Properties().stacksTo(1).durability(5)));

    // Transfer speed upgrades
    public static final Item TRANSFER_SPEED_UPGRADE_ZERO = register("transfer_speed_upgrade_zero",
            new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 0, 1));
            
    public static final Item TRANSFER_SPEED_UPGRADE_BASIC = register("transfer_speed_upgrade_basic",
            new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 1, 5));
            
    public static final Item TRANSFER_SPEED_UPGRADE_COPPER = register("transfer_speed_upgrade_copper",
            new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 2, 10));
            
    public static final Item TRANSFER_SPEED_UPGRADE_ADVANCED = register("transfer_speed_upgrade_advanced",
            new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 3, 20));
            
    public static final Item TRANSFER_SPEED_UPGRADE_ELITE = register("transfer_speed_upgrade_elite",
            new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 4, 64));
            
    public static final Item TRANSFER_SPEED_UPGRADE_DEFINITIVE = register("transfer_speed_upgrade_definitive",
            new TransferSpeedUpgradeItem(new Item.Properties().stacksTo(16), 5, 256));

    // Filter items
    public static final Item BASE_FILTER = register("base_filter",
            new BaseFilterItem(new Item.Properties().stacksTo(64)));
            
    public static final Item TOOLS_FILTER = register("tools_filter",
            new ToolsFilterItem(new Item.Properties().stacksTo(64)));
            
    public static final Item FOOD_FILTER = register("food_filter",
            new FoodFilterItem(new Item.Properties().stacksTo(64)));
            
    public static final Item CUSTOM_TAG_BLOCKER = register("custom_tag_blocker",
            new CustomTagFilterItem(new Item.Properties().stacksTo(64)));
    
    public static final Item FUEL_FILTER = register("fuel_filter",
            new FuelFilterItem(new Item.Properties()));
            
    public static final Item ATTRIBUTE_FILTER = register("attribute_filter",
            new AttributeFilterItem(new Item.Properties().stacksTo(64)));
            
    public static final Item NAME_FILTER = register("name_filter",
            new NameFilterItem(new Item.Properties().stacksTo(64)));
            
    // Chip Manager
    public static final Item CHIP_MANAGER = register("chip_manager",
            new ChipManagerItem(new Item.Properties().stacksTo(64)));

    // Screen
    public static final Item SCREEN_ITEM = register("screen",
            new ScreenItem(new Item.Properties().stacksTo(64)));


    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, IndexerMod.id(name), item);
    }

    public static void register() {
        // Just to load class
    }
}
