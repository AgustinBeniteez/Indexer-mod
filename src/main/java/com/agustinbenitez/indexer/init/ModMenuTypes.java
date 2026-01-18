package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.inventory.IndexerConnectorMenu;
import com.agustinbenitez.indexer.menu.DropBoxMenu;
import com.agustinbenitez.indexer.menu.IndexerControllerNetworkMenu;
import com.agustinbenitez.indexer.menu.IndexerManagerMenu;
import com.agustinbenitez.indexer.block.entity.IndexerConnectorBlockEntity;
import com.agustinbenitez.indexer.block.entity.IndexerManagerBlockEntity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Inventory;

public class ModMenuTypes {

    public static final MenuType<IndexerConnectorMenu> INDEXER_CONNECTOR_MENU = register("indexer_connector_menu",
            new ExtendedScreenHandlerType<>(
                    (int syncId, Inventory inventory, BlockPosPayload payload) -> {
                         BlockEntity be = inventory.player.level().getBlockEntity(payload.pos());
                         if (be instanceof IndexerConnectorBlockEntity connector) {
                             return new IndexerConnectorMenu(syncId, inventory, connector, connector);
                         }
                         return new IndexerConnectorMenu(syncId, inventory);
                    },
                    BlockPosPayload.CODEC));

    public static final MenuType<IndexerControllerNetworkMenu> INDEXER_CONTROLLER_NETWORK_MENU = register("indexer_controller_network_menu",
            new ExtendedScreenHandlerType<>(
                    (int syncId, Inventory inventory, BlockPosPayload payload) -> {
                        BlockEntity be = inventory.player.level().getBlockEntity(payload.pos());
                        return new IndexerControllerNetworkMenu(syncId, inventory, be, new net.minecraft.world.inventory.SimpleContainerData(8));
                    },
                    BlockPosPayload.CODEC));

    public static final MenuType<DropBoxMenu> DROP_BOX_MENU = register("drop_box_menu",
            new MenuType<>(DropBoxMenu::new, net.minecraft.world.flag.FeatureFlagSet.of()));

    public static final MenuType<IndexerManagerMenu> INDEXER_MANAGER_MENU = register("indexer_manager_menu",
            new ExtendedScreenHandlerType<>(
                    (int syncId, Inventory inventory, BlockPosPayload payload) -> {
                        BlockEntity be = inventory.player.level().getBlockEntity(payload.pos());
                         return new IndexerManagerMenu(syncId, inventory, be);
                    },
                    BlockPosPayload.CODEC));

    private static <T extends MenuType<?>> T register(String name, T menuType) {
        return Registry.register(BuiltInRegistries.MENU, IndexerMod.id(name), menuType);
    }

    public static void register() {
        // Just to load class
    }

    public record BlockPosPayload(BlockPos pos) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BlockPosPayload> ID = new CustomPacketPayload.Type<>(IndexerMod.id("block_pos"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BlockPosPayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, BlockPosPayload::pos,
                BlockPosPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
}
