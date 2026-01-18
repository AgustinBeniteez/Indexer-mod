package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {

    public static final CreativeModeTab INDEXER_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            IndexerMod.id("indexer_tab"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.INDEXER_CONTROLLER_ITEM))
                    .title(Component.translatable("itemGroup.indexer"))
                    .displayItems((parameters, output) -> {
                        // Add all items
                        output.accept(ModItems.INDEXER_CONTROLLER_ITEM);
                        output.accept(ModItems.INDEXER_PIPE_ITEM);
                        output.accept(ModItems.INDEXER_CONNECTOR_ITEM);
                        output.accept(ModItems.DROP_BOX_ITEM);
                        output.accept(ModItems.EXTRACTOR_ITEM);
                        output.accept(ModItems.INDEXER_MANAGER_ITEM);
                        output.accept(ModItems.INDEXER_MANUAL);
                        output.accept(ModItems.CONNECTOR_CAPACITY_UPGRADE);
                        output.accept(ModItems.TRANSFER_SPEED_UPGRADE_BASIC);
                        output.accept(ModItems.TRANSFER_SPEED_UPGRADE_COPPER);
                        output.accept(ModItems.TRANSFER_SPEED_UPGRADE_ADVANCED);
                        output.accept(ModItems.TRANSFER_SPEED_UPGRADE_ELITE);
                        output.accept(ModItems.TRANSFER_SPEED_UPGRADE_DEFINITIVE);
                        // Filter items
                        output.accept(ModItems.BASE_FILTER);
                        output.accept(ModItems.TOOLS_FILTER);
                        output.accept(ModItems.FOOD_FILTER);
                        output.accept(ModItems.FUEL_FILTER);
                        output.accept(ModItems.CUSTOM_TAG_BLOCKER);
                        output.accept(ModItems.ATTRIBUTE_FILTER);
                        output.accept(ModItems.NAME_FILTER);
                        output.accept(ModItems.CHIP_MANAGER);
                        output.accept(ModItems.SCREEN_ITEM);
                    })
                    .build());

    public static void register() {
        // Just to load class
    }
}
