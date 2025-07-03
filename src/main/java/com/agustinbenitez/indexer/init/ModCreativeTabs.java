package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IndexerMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> INDEXER_TAB = CREATIVE_MODE_TABS.register("indexer_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.INDEXER_CONTROLLER_ITEM.get()))
                    .title(Component.translatable("itemGroup.indexer"))
                    .displayItems((parameters, output) -> {
                        // Añadir todos los ítems del mod a la pestaña creativa
                        output.accept(ModItems.INDEXER_CONTROLLER_ITEM.get());
                        output.accept(ModItems.INDEXER_PIPE_ITEM.get());
                        output.accept(ModItems.INDEXER_CONNECTOR_ITEM.get());
                        output.accept(ModItems.INDEXER_MANUAL.get());
                        output.accept(ModItems.INDEXER_ADJUSTER.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}