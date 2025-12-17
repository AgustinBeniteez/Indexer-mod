package com.agustinbenitez.indexer.init;

import com.agustinbenitez.indexer.IndexerMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.UnaryOperator;

public class ModDataComponents {
        public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister
                        .create(Registries.DATA_COMPONENT_TYPE, IndexerMod.MOD_ID);

        public static final RegistryObject<DataComponentType<String>> CUSTOM_NAME = register("custom_name",
                        builder -> builder.persistent(ExtraCodecs.NON_EMPTY_STRING)
                                        .networkSynchronized(ByteBufCodecs.STRING_UTF8));

        public static final RegistryObject<DataComponentType<String>> CUSTOM_TAG = register("custom_tag",
                        builder -> builder.persistent(ExtraCodecs.NON_EMPTY_STRING)
                                        .networkSynchronized(ByteBufCodecs.STRING_UTF8));

        public static final RegistryObject<DataComponentType<String>> ATTRIBUTE_FILTER = register("attribute_filter",
                        builder -> builder.persistent(ExtraCodecs.NON_EMPTY_STRING)
                                        .networkSynchronized(ByteBufCodecs.STRING_UTF8));

        public static final RegistryObject<DataComponentType<Integer>> UPGRADE_LEVEL = register("upgrade_level",
                        builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT)
                                        .networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final RegistryObject<DataComponentType<Integer>> ITEMS_PER_TRANSFER = register(
                        "items_per_transfer",
                        builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT)
                                        .networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final RegistryObject<DataComponentType<Integer>> CONNECTOR_LEVEL = register("connector_level",
                        builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT)
                                        .networkSynchronized(ByteBufCodecs.VAR_INT));

        private static <T> RegistryObject<DataComponentType<T>> register(String name,
                        UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
                return DATA_COMPONENT_TYPES.register(name,
                                () -> builderOperator.apply(DataComponentType.builder()).build());
        }

        public static void register(IEventBus eventBus) {
                DATA_COMPONENT_TYPES.register(eventBus);
        }
}
