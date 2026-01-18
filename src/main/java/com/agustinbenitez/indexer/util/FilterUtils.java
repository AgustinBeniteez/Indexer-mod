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
        if (customTag.isEmpty() && tag.contains("custom_tag")) {
            customTag = tag.getString("custom_tag");
        }
        if (customTag.isEmpty()) {
            return true; // Sin tag configurado, permite todo (no bloquea nada)
        }
        
        // COMPORTAMIENTO DE BLOQUEO: Bloquear SOLO el item exacto configurado
        
        String itemName = itemToCheck.getItem().getDescriptionId();
        String shortItemName = itemName.replace("item.minecraft.", "").replace("block.minecraft.", "");
        net.minecraft.resources.ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(itemToCheck.getItem());
        String rlFull = itemKey != null ? itemKey.toString() : "";
        String rlPath = itemKey != null ? itemKey.getPath() : "";

        String filterValue = customTag;

        if (itemName.equals(filterValue)) {
            return false;
        }

        if (shortItemName.equals(filterValue)) {
            return false;
        }

        if (!rlFull.isEmpty() && rlFull.equals(filterValue)) {
            return false;
        }

        if (!rlPath.isEmpty() && rlPath.equals(filterValue)) {
            return false;
        }
        
        return true; // Permitir todos los demás items
    }
    
    private static boolean passesAttributeFilter(ItemStack itemToCheck, ItemStack filterItem) {
        net.minecraft.world.item.component.CustomData customData = filterItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        
        CompoundTag tag = customData.copyTag();
        String attributeFilter = tag.getString("attribute_filter");
        if (attributeFilter.isEmpty()) {
            return false;
        }
        
        String filterLower = attributeFilter.toLowerCase();

        net.minecraft.world.item.enchantment.ItemEnchantments enchantments = itemToCheck.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>> entry : enchantments.entrySet()) {
                net.minecraft.resources.ResourceLocation enchId = entry.getKey().unwrapKey().map(net.minecraft.resources.ResourceKey::location).orElse(null);
                if (enchId != null && enchId.toString().toLowerCase().contains(filterLower)) {
                    return true;
                }
            }
        }

        net.minecraft.world.item.enchantment.ItemEnchantments storedEnchantments = itemToCheck.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
        if (storedEnchantments != null && !storedEnchantments.isEmpty()) {
            for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>> entry : storedEnchantments.entrySet()) {
                net.minecraft.resources.ResourceLocation enchId = entry.getKey().unwrapKey().map(net.minecraft.resources.ResourceKey::location).orElse(null);
                if (enchId != null && enchId.toString().toLowerCase().contains(filterLower)) {
                    return true;
                }
            }
        }
        
        net.minecraft.world.item.component.CustomData itemCustomData = itemToCheck.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (itemCustomData != null) {
            CompoundTag itemTag = itemCustomData.copyTag();
            
            if (itemTag.contains("Enchantments")) {
                net.minecraft.nbt.ListTag list = itemTag.getList("Enchantments", 10);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag ench = list.getCompound(i);
                    String id = ench.getString("id").toLowerCase();
                    if (id.contains(filterLower)) {
                        return true;
                    }
                }
            }
            
            if (itemTag.contains("StoredEnchantments")) {
                net.minecraft.nbt.ListTag list = itemTag.getList("StoredEnchantments", 10);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag ench = list.getCompound(i);
                    String id = ench.getString("id").toLowerCase();
                    if (id.contains(filterLower)) {
                        return true;
                    }
                }
            }
        }
        
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
