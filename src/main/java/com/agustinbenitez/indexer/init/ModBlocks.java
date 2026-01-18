package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {
    
    public static final Block INDEXER_CONTROLLER = register("indexer_controller",
            new IndexerControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(1.2f, 2.0f)
                    .sound(SoundType.METAL)));

    public static final Block INDEXER_PIPE = register("indexer_pipe",
            new IndexerPipeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(0.2f, 0.1f)
                    .sound(SoundType.METAL)));

    public static final Block INDEXER_CONNECTOR = register("indexer_connector",
            new IndexerConnectorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(1.2f, 2.0f)
                    .sound(SoundType.METAL)));

    public static final Block DROP_BOX = register("drop_box",
            new DropBoxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(1.2f, 2.0f)
                    .sound(SoundType.METAL)));

    public static final Block EXTRACTOR = register("extractor",
            new ExtractorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(1.2f, 2.0f)
                    .sound(SoundType.METAL)));

    public static final Block INDEXER_MANAGER = register("indexer_manager",
            new IndexerManagerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(1.2f, 2.0f)
                    .sound(SoundType.METAL)));

    private static Block register(String name, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, IndexerMod.id(name), block);
    }

    public static void register() {
        // Just to load the class
    }
}
