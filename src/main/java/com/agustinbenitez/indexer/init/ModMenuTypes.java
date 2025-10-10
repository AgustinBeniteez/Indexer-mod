package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.inventory.IndexerConnectorMenu;
import com.agustinbenitez.indexer.menu.DropBoxMenu;
import com.agustinbenitez.indexer.menu.IndexerControllerNetworkMenu;
import com.agustinbenitez.indexer.block.entity.IndexerConnectorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
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
                    () -> IForgeMenuType.create((id, inventory, data) -> {
                        BlockPos pos = data.readBlockPos();
                        BlockEntity be = inventory.player.level().getBlockEntity(pos);
                        if (be instanceof IndexerConnectorBlockEntity connector) {
                            // Cliente reconstruye el menú con el BlockEntity real, habilitando 18 slots si está mejorado
                            return new IndexerConnectorMenu(id, inventory, connector, connector);
                        }
                        // Fallback seguro si no se encuentra el BE
                        return new IndexerConnectorMenu(id, inventory);
                    }));

    public static final RegistryObject<MenuType<IndexerControllerNetworkMenu>> INDEXER_CONTROLLER_NETWORK_MENU = 
            MENUS.register("indexer_controller_network_menu", 
                    () -> IForgeMenuType.create(IndexerControllerNetworkMenu::new));
                    
    public static final RegistryObject<MenuType<DropBoxMenu>> DROP_BOX_MENU = 
            MENUS.register("drop_box_menu", 
                    () -> IForgeMenuType.create((id, inventory, data) -> 
                            new DropBoxMenu(id, inventory)));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}