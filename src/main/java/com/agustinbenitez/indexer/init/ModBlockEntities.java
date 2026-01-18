package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.entity.DropBoxBlockEntity;
import com.agustinbenitez.indexer.block.entity.ExtractorBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerControllerBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerConnectorBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerManagerBlockEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    // Entidad de bloque para el controlador del indexador
    public static final BlockEntityType<IndexerControllerBlockEntity> INDEXER_CONTROLLER = register("indexer_controller",
            BlockEntityType.Builder.of(IndexerControllerBlockEntity::new, ModBlocks.INDEXER_CONTROLLER).build(null));

    // Entidad de bloque para el conector del indexador
    public static final BlockEntityType<IndexerConnectorBlockEntity> INDEXER_CONNECTOR = register("indexer_connector",
            BlockEntityType.Builder.of(IndexerConnectorBlockEntity::new, ModBlocks.INDEXER_CONNECTOR).build(null));
            
    // Entidad de bloque para el DropBox
    public static final BlockEntityType<DropBoxBlockEntity> DROP_BOX = register("drop_box",
            BlockEntityType.Builder.of(DropBoxBlockEntity::new, ModBlocks.DROP_BOX).build(null));
            
    // Entidad de bloque para el Extractor
    public static final BlockEntityType<ExtractorBlockEntity> EXTRACTOR = register("extractor",
            BlockEntityType.Builder.of(ExtractorBlockEntity::new, ModBlocks.EXTRACTOR).build(null));
            
    // Entidad de bloque para el Manager
    public static final BlockEntityType<IndexerManagerBlockEntity> INDEXER_MANAGER = register("indexer_manager",
            BlockEntityType.Builder.of(IndexerManagerBlockEntity::new, ModBlocks.INDEXER_MANAGER).build(null));

    private static <T extends BlockEntityType<?>> T register(String name, T blockEntityType) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, IndexerMod.id(name), blockEntityType);
    }

    public static void register() {
        // Just to load class
    }
}
