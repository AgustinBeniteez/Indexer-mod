package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.block.DropBoxBlock;
import com.agustinbenitez.indexer.block.IndexerControllerBlock;
import com.agustinbenitez.indexer.block.IndexerPipeBlock;
import com.agustinbenitez.indexer.block.IndexerConnectorBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, IndexerMod.MOD_ID);

    // Registra el bloque controlador principal del indexador
    public static final RegistryObject<Block> INDEXER_CONTROLLER = BLOCKS.register("indexer_controller",
            () -> new IndexerControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(0.5f, 0.4f) // Reducida la resistencia para que se rompa más rápido
                    .sound(SoundType.METAL)));

    // Registra el bloque de tubería para conectar componentes
    public static final RegistryObject<Block> INDEXER_PIPE = BLOCKS.register("indexer_pipe",
            () -> new IndexerPipeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(0.2f, 0.1f) // Resistencia extremadamente baja para que se rompa instantáneamente
                    .sound(SoundType.METAL)));

    // Registra el bloque conector que se coloca en los cofres
    public static final RegistryObject<Block> INDEXER_CONNECTOR = BLOCKS.register("indexer_connector",
            () -> new IndexerConnectorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(0.5f, 0.4f) // Reducida la resistencia para que se rompa muy rápido
                    .sound(SoundType.METAL)));
                    
    // Registra el bloque DropBox que funciona como un cofre con más capacidad
    public static final RegistryObject<Block> DROP_BOX = BLOCKS.register("drop_box",
            () -> new DropBoxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(0.5f, 0.4f) // Reducida la resistencia para que se rompa más rápido
                    .sound(SoundType.METAL)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}