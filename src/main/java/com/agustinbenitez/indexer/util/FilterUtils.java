package com.agustinbenitez.indexer.util;

import com.agustinbenitez.indexer.init.ModDataComponents;
import com.agustinbenitez.indexer.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;

public class FilterUtils {

    public static boolean passesFilter(ItemStack itemToCheck, ItemStack filterItem) {
        if (itemToCheck.isEmpty() || filterItem.isEmpty()) {
            return false;
        }

        Item filterType = filterItem.getItem();

        // Filtro de herramientas
        if (filterType == ModItems.TOOLS_FILTER.get()) {
            return isToolItem(itemToCheck);
        }

        // Filtro de comida
        if (filterType == ModItems.FOOD_FILTER.get()) {
            return itemToCheck.has(DataComponents.FOOD);
        }

        // Filtro de combustibles
        if (filterType == ModItems.FUEL_FILTER.get()) {
            return isFuelItem(itemToCheck);
        }

        // Filtro personalizable (bloqueador)
        if (filterType == ModItems.CUSTOM_TAG_BLOCKER.get()) {
            return passesCustomTagFilter(itemToCheck, filterItem);
        }

        // Filtro de atributos
        if (filterType == ModItems.ATTRIBUTE_FILTER.get()) {
            return passesAttributeFilter(itemToCheck, filterItem);
        }

        // Filtro por nombre personalizado
        if (filterType == ModItems.NAME_FILTER.get()) {
            return passesNameFilter(itemToCheck, filterItem);
        }

        // Filtro exacto (comportamiento original)
        return itemToCheck.getItem() == filterItem.getItem();
    }

    /**
     * Verifica si un filtro es de tipo bloqueador
     * 
     * @param filterItem el filtro a verificar
     * @return true si es un filtro de bloqueo, false en caso contrario
     */
    public static boolean isBlockingFilter(ItemStack filterItem) {
        if (filterItem.isEmpty()) {
            return false;
        }
        return filterItem.getItem() == ModItems.CUSTOM_TAG_BLOCKER.get();
    }

    private static boolean isToolItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof PickaxeItem ||
                item instanceof AxeItem ||
                item instanceof ShovelItem ||
                item instanceof HoeItem ||
                item instanceof SwordItem;
    }

    private static boolean isFuelItem(ItemStack stack) {
        // Solo permitir combustibles específicos: carbón, carbón vegetal, cubo de lava,
        // bloque de carbón y algas cocinadas en bloque
        String itemId = stack.getItem().getDescriptionId();

        return itemId.equals("item.minecraft.coal") || // Carbón
                itemId.equals("item.minecraft.charcoal") || // Carbón vegetal
                itemId.equals("item.minecraft.lava_bucket") || // Cubo de lava
                itemId.equals("block.minecraft.coal_block") || // Bloque de carbón
                itemId.equals("block.minecraft.dried_kelp_block"); // Algas cocinadas en bloque
    }

    private static boolean passesCustomTagFilter(ItemStack itemToCheck, ItemStack filterItem) {
        if (!filterItem.has(ModDataComponents.CUSTOM_TAG.get())) {
            return true; // Sin tag configurado, permite todo (no bloquea nada)
        }

        String customTag = filterItem.get(ModDataComponents.CUSTOM_TAG.get());
        if (customTag == null || customTag.isEmpty()) {
            return true; // Sin tag configurado, permite todo (no bloquea nada)
        }

        // COMPORTAMIENTO DE BLOQUEO: Bloquear SOLO el item exacto configurado

        // Verificar si es exactamente el mismo item
        String itemName = itemToCheck.getItem().getDescriptionId();
        String filterItemName = customTag;

        // Si el customTag contiene el ID completo del item (ej:
        // "item.minecraft.iron_pickaxe")
        if (itemName.equals(filterItemName)) {
            return false; // Bloquear este item específico
        }

        // Si el customTag solo contiene el nombre corto (ej: "iron_pickaxe")
        String shortItemName = itemName.replace("item.minecraft.", "").replace("block.minecraft.", "");
        if (shortItemName.equals(filterItemName)) {
            return false; // Bloquear este item específico
        }

        return true; // Permitir todos los demás items
    }

    private static boolean passesAttributeFilter(ItemStack itemToCheck, ItemStack filterItem) {
        if (!filterItem.has(ModDataComponents.ATTRIBUTE_FILTER.get())) {
            return false; // Sin atributo configurado, no pasa nada
        }

        String attributeFilter = filterItem.get(ModDataComponents.ATTRIBUTE_FILTER.get());
        if (attributeFilter == null || attributeFilter.isEmpty()) {
            return false; // Sin atributo configurado, no pasa nada
        }

        // Verificar si el item tiene el atributo especificado
        // Primero verificar encantamientos
        if (itemToCheck.isEnchanted()) {
            return itemToCheck.getEnchantments().entrySet().stream()
                    .anyMatch(entry -> {
                        var enchantmentHolder = entry.getKey();
                        net.minecraft.resources.ResourceLocation enchantmentKey = enchantmentHolder.unwrapKey()
                                .map(net.minecraft.resources.ResourceKey::location).orElse(null);
                        return enchantmentHolder.value().description().getString().contains(attributeFilter) ||
                                (enchantmentKey != null && enchantmentKey.toString().equals(attributeFilter));
                    });
        }

        // También verificar otros atributos del item (nombre, tags, etc.)
        String itemName = itemToCheck.getItem().toString().toLowerCase();
        String displayName = itemToCheck.getHoverName().getString().toLowerCase();

        return itemName.contains(attributeFilter.toLowerCase()) ||
                displayName.contains(attributeFilter.toLowerCase()) ||
                itemToCheck.getTags().anyMatch(tag -> tag.location().toString().contains(attributeFilter));
    }

    /**
     * Verifica si un item pasa el filtro por nombre personalizado.
     * Solo permite pasar items que tengan exactamente el nombre configurado.
     * Útil para items renombrados con yunque.
     */
    private static boolean passesNameFilter(ItemStack itemToCheck, ItemStack filterItem) {
        if (!filterItem.has(ModDataComponents.CUSTOM_NAME.get())) {
            return false; // Sin nombre configurado, no pasa nada
        }

        String nameFilter = filterItem.get(ModDataComponents.CUSTOM_NAME.get());
        if (nameFilter == null || nameFilter.isEmpty()) {
            return false; // Sin nombre configurado, no pasa nada
        }

        // SOLO permitir items que tengan EXACTAMENTE el nombre configurado

        // Verificar si el item tiene nombre personalizado (DataComponents.CUSTOM_NAME)
        if (itemToCheck.has(DataComponents.CUSTOM_NAME)) {
            String customName = itemToCheck.getHoverName().getString();
            return customName.equals(nameFilter);
        }

        // Si el item NO tiene nombre personalizado, NO debe pasar el filtro de nombre
        return false;
    }
}