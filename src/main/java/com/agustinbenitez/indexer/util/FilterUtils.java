package com.agustinbenitez.indexer.util;

import com.agustinbenitez.indexer.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;

public class FilterUtils {
    
    public static boolean passesFilter(ItemStack itemToCheck, ItemStack filterItem) {
        if (itemToCheck.isEmpty() || filterItem.isEmpty()) {
            return false;
        }
        
        Item filterType = filterItem.getItem();
        
        // Filtro de herramientas
        if (filterType == ModItems.TOOLS_FILTER) {
            return isToolItem(itemToCheck);
        }
        
        // Filtro de comida
        if (filterType == ModItems.FOOD_FILTER) {
            return itemToCheck.get(net.minecraft.core.component.DataComponents.FOOD) != null;
        }
        
        // Filtro de combustibles
        if (filterType == ModItems.FUEL_FILTER) {
            return isFuelItem(itemToCheck);
        }
        
        // Filtro personalizable (bloqueador)
        if (filterType == ModItems.CUSTOM_TAG_BLOCKER) {
            return passesCustomTagFilter(itemToCheck, filterItem);
        }
        
        // Filtro de atributos
        if (filterType == ModItems.ATTRIBUTE_FILTER) {
            return passesAttributeFilter(itemToCheck, filterItem);
        }
        
        // Filtro por nombre personalizado
        if (filterType == ModItems.NAME_FILTER) {
            return passesNameFilter(itemToCheck, filterItem);
        }
        
        // Filtro por mod específico eliminado
        
        // Filtro exacto (comportamiento original)
        return itemToCheck.getItem() == filterItem.getItem();
    }
    
    /**
     * Verifica si un filtro es de tipo bloqueador
     * @param filterItem el filtro a verificar
     * @return true si es un filtro de bloqueo, false en caso contrario
     */
    public static boolean isBlockingFilter(ItemStack filterItem) {
        if (filterItem.isEmpty()) {
            return false;
        }
        return filterItem.getItem() == ModItems.CUSTOM_TAG_BLOCKER;
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
        // Solo permitir combustibles específicos: carbón, carbón vegetal, cubo de lava, bloque de carbón y algas cocinadas en bloque
        String itemId = stack.getItem().getDescriptionId();
        
        return itemId.equals("item.minecraft.coal") ||           // Carbón
               itemId.equals("item.minecraft.charcoal") ||        // Carbón vegetal
               itemId.equals("item.minecraft.lava_bucket") ||     // Cubo de lava
               itemId.equals("block.minecraft.coal_block") ||     // Bloque de carbón
               itemId.equals("block.minecraft.dried_kelp_block"); // Algas cocinadas en bloque
    }
    
    private static boolean passesCustomTagFilter(ItemStack itemToCheck, ItemStack filterItem) {
        net.minecraft.world.item.component.CustomData customData = filterItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return true; // Sin tag configurado, permite todo (no bloquea nada)
        }
        
        CompoundTag tag = customData.copyTag();
        String customTag = tag.getString("custom_tag_filter");
        if (customTag.isEmpty()) {
            return true; // Sin tag configurado, permite todo (no bloquea nada)
        }
        
        // COMPORTAMIENTO DE BLOQUEO: Bloquear SOLO el item exacto configurado
        
        // Verificar si es exactamente el mismo item
        String itemName = itemToCheck.getItem().getDescriptionId();
        String filterItemName = customTag;
        
        // Si el customTag contiene el ID completo del item (ej: "item.minecraft.iron_pickaxe")
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
        net.minecraft.world.item.component.CustomData customData = filterItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false; // Sin atributo configurado, no pasa nada
        }
        
        CompoundTag tag = customData.copyTag();
        String attributeFilter = tag.getString("attribute_filter");
        if (attributeFilter.isEmpty()) {
            return false; // Sin atributo configurado, no pasa nada
        }
        
        // Verificar si el item tiene el atributo especificado
        String filterLower = attributeFilter.toLowerCase();
        
        String itemName = itemToCheck.getItem().getDescriptionId().toLowerCase();
        String displayName = itemToCheck.getHoverName().getString().toLowerCase();
        
        return itemName.contains(filterLower) || displayName.contains(filterLower);
    }
    
    /**
     * Verifica si un item pasa el filtro por nombre personalizado.
     * Solo permite pasar items que tengan exactamente el nombre configurado.
     * Útil para items renombrados con yunque.
     */
    private static boolean passesNameFilter(ItemStack itemToCheck, ItemStack filterItem) {
        net.minecraft.world.item.component.CustomData customData = filterItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false; // Sin nombre configurado, no pasa nada
        }
        
        CompoundTag tag = customData.copyTag();
        String nameFilter = tag.getString("custom_name");
        if (nameFilter.isEmpty()) {
            return false; // Sin nombre configurado, no pasa nada
        }
        
        // SOLO permitir items que tengan EXACTAMENTE el nombre configurado
        String displayName = itemToCheck.getHoverName().getString();
        return displayName.equals(nameFilter);
    }
    
    /**
     * Verifica si un item pasa el filtro por mod específico.
     * Solo permite pasar items que provengan del mod configurado.
     */
    // Método passesModFilter eliminado
}
