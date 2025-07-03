package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.entity.DropBoxBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerConnectorBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, IndexerMod.MOD_ID);

    // Entidad de bloque para el controlador del indexador
    public static final RegistryObject<BlockEntityType<IndexerControllerBlockEntity>> INDEXER_CONTROLLER = 
            BLOCK_ENTITIES.register("indexer_controller", 
                    () -> BlockEntityType.Builder.of(
                            IndexerControllerBlockEntity::new, 
                            ModBlocks.INDEXER_CONTROLLER.get())
                    .build(null));

    // Entidad de bloque para el conector del indexador
    public static final RegistryObject<BlockEntityType<IndexerConnectorBlockEntity>> INDEXER_CONNECTOR = 
            BLOCK_ENTITIES.register("indexer_connector", 
                    () -> BlockEntityType.Builder.of(
                            IndexerConnectorBlockEntity::new, 
                            ModBlocks.INDEXER_CONNECTOR.get())
                    .build(null));
                    
    // Entidad de bloque para el DropBox
    public static final RegistryObject<BlockEntityType<DropBoxBlockEntity>> DROP_BOX = 
            BLOCK_ENTITIES.register("drop_box", 
                    () -> BlockEntityType.Builder.of(
                            DropBoxBlockEntity::new, 
                            ModBlocks.DROP_BOX.get())
                    .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}