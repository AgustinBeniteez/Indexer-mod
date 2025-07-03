package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.inventory.IndexerConnectorMenu;
import com.agustinbenitez.indexer.menu.DropBoxMenu;
import com.agustinbenitez.indexer.menu.IndexerControllerMenu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = 
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, IndexerMod.MOD_ID);

    public static final RegistryObject<MenuType<IndexerConnectorMenu>> INDEXER_CONNECTOR_MENU = 
            MENUS.register("indexer_connector_menu", 
                    () -> IForgeMenuType.create((id, inventory, data) -> 
                            new IndexerConnectorMenu(id, inventory, new net.minecraft.world.SimpleContainer(1), null)));

    public static final RegistryObject<MenuType<IndexerControllerMenu>> INDEXER_CONTROLLER_MENU = 
            MENUS.register("indexer_controller_menu", 
                    () -> IForgeMenuType.create(IndexerControllerMenu::new));
                    
    public static final RegistryObject<MenuType<DropBoxMenu>> DROP_BOX_MENU = 
            MENUS.register("drop_box_menu", 
                    () -> IForgeMenuType.create(DropBoxMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}