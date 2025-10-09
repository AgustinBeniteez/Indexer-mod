package com.agustinbenitez.indexer.util;

import com.agustinbenitez.indexer.init.ModItems;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeHooks;

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
            return itemToCheck.isEdible();
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
        
        // Filtro por mod específico
        if (filterType == ModItems.MOD_FILTER.get()) {
            return passesModFilter(itemToCheck, filterItem);
        }
        
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
        // Solo permitir combustibles específicos: carbón, carbón vegetal, cubo de lava, bloque de carbón y algas cocinadas en bloque
        String itemId = stack.getItem().getDescriptionId();
        
        return itemId.equals("item.minecraft.coal") ||           // Carbón
               itemId.equals("item.minecraft.charcoal") ||        // Carbón vegetal
               itemId.equals("item.minecraft.lava_bucket") ||     // Cubo de lava
               itemId.equals("block.minecraft.coal_block") ||     // Bloque de carbón
               itemId.equals("block.minecraft.dried_kelp_block"); // Algas cocinadas en bloque
    }
    
    private static boolean passesCustomTagFilter(ItemStack itemToCheck, ItemStack filterItem) {
        if (!filterItem.hasTag()) {
            return true; // Sin tag configurado, permite todo (no bloquea nada)
        }
        
        String customTag = filterItem.getTag().getString("custom_tag");
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
        if (!filterItem.hasTag()) {
            return false; // Sin atributo configurado, no pasa nada
        }
        
        String attributeFilter = filterItem.getTag().getString("attribute_filter");
        if (attributeFilter.isEmpty()) {
            return false; // Sin atributo configurado, no pasa nada
        }
        
        // Verificar si el item tiene el atributo especificado
        // Primero verificar encantamientos
        if (itemToCheck.isEnchanted()) {
            return itemToCheck.getAllEnchantments().keySet().stream()
                    .anyMatch(enchantment -> {
                        net.minecraft.resources.ResourceLocation enchantmentKey = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                        return enchantment.getDescriptionId().contains(attributeFilter) ||
                               (enchantmentKey != null && enchantmentKey.toString().equals(attributeFilter));
                    });
        }
        
        // También verificar otros atributos del item (nombre, tags, etc.)
        String itemName = itemToCheck.getItem().toString().toLowerCase();
        String displayName = itemToCheck.getDisplayName().getString().toLowerCase();
        
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
        if (!filterItem.hasTag()) {
            return false; // Sin nombre configurado, no pasa nada
        }
        
        String nameFilter = filterItem.getTag().getString("custom_name");
        if (nameFilter.isEmpty()) {
            return false; // Sin nombre configurado, no pasa nada
        }
        
        // SOLO permitir items que tengan EXACTAMENTE el nombre configurado
        
        // Verificar específicamente el NBT tag que crea el yunque
        // Cuando se renombra con yunque, Minecraft guarda el nombre en display.Name
        if (itemToCheck.hasTag() && itemToCheck.getTag().contains("display")) {
            var displayTag = itemToCheck.getTag().getCompound("display");
            if (displayTag.contains("Name")) {
                String anvilName = displayTag.getString("Name");
                
                // El yunque guarda el nombre en formato JSON: {"text":"NombrePersonalizado"}
                if (anvilName.startsWith("{") && anvilName.contains("\"text\"")) {
                    try {
                        // Extraer el texto del JSON del yunque
                        int textStart = anvilName.indexOf("\"text\":\"") + 8;
                        int textEnd = anvilName.indexOf("\"", textStart);
                        if (textStart > 7 && textEnd > textStart) {
                            String extractedName = anvilName.substring(textStart, textEnd);
                            // SOLO permitir si el nombre es EXACTAMENTE igual
                            return extractedName.equals(nameFilter);
                        }
                    } catch (Exception e) {
                        // Si falla el parsing JSON, intentar comparación directa
                    }
                }
                
                // Fallback: comparar directamente el string del NBT
                // SOLO permitir coincidencia exacta
                return anvilName.equals(nameFilter) || anvilName.equals("{\"text\":\"" + nameFilter + "\"}");
            }
        }
        
        // Si el item NO tiene nombre personalizado, NO debe pasar el filtro de nombre
        return false;
    }
    
    /**
     * Verifica si un item pasa el filtro por mod específico.
     * Solo permite pasar items que provengan del mod configurado.
     */
    private static boolean passesModFilter(ItemStack itemToCheck, ItemStack filterItem) {
        if (!filterItem.hasTag()) {
            System.out.println("[MOD_FILTER_DEBUG] Filtro sin tag NBT");
            return false; // Sin mod configurado, no pasa nada
        }
        
        String modIdFilter = filterItem.getTag().getString("mod_id");
        if (modIdFilter.isEmpty()) {
            System.out.println("[MOD_FILTER_DEBUG] Filtro sin mod_id configurado");
            return false; // Sin mod configurado, no pasa nada
        }
        
        // Obtener el mod ID del item a verificar
        String itemModId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(itemToCheck.getItem()).getNamespace();
        
        System.out.println("[MOD_FILTER_DEBUG] Item: " + itemToCheck.getItem().getDescriptionId() + 
                          " | Item ModID: " + itemModId + 
                          " | Filter ModID: " + modIdFilter + 
                          " | Match: " + modIdFilter.equals(itemModId));
        
        // Verificar si coincide exactamente con el mod configurado
        return modIdFilter.equals(itemModId);
    }
}