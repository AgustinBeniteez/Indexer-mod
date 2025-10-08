package com.agustinbenitez.indexer.util;

import com.agustinbenitez.indexer.init.ModItems;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeHooks;

public class FilterUtils {
    
    public static boolean passesFilter(ItemStack itemToCheck, ItemStack filterItem) {
        if (filterItem.isEmpty()) {
            return true; // Sin filtro, permite todo
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
        
        // Filtro de picos
        if (filterType == ModItems.PICKAXE_FILTER.get()) {
            return itemToCheck.getItem() instanceof PickaxeItem;
        }
        
        // Filtro de combustibles
        if (filterType == ModItems.FUEL_FILTER.get()) {
            return isFuelItem(itemToCheck);
        }
        
        // Filtro de minerales
        if (filterType == ModItems.ORES_FILTER.get()) {
            return isOreItem(itemToCheck);
        }
        
        // Filtro de bloques
        if (filterType == ModItems.BLOCKS_FILTER.get()) {
            return itemToCheck.getItem() instanceof BlockItem;
        }
        
        // Filtro de armas
        if (filterType == ModItems.WEAPONS_FILTER.get()) {
            return isWeaponItem(itemToCheck);
        }
        
        // Filtro de armaduras
        if (filterType == ModItems.ARMOR_FILTER.get()) {
            return itemToCheck.getItem() instanceof ArmorItem;
        }
        
        // Filtro personalizable
        if (filterType == ModItems.CUSTOM_TAG_FILTER.get()) {
            return passesCustomTagFilter(itemToCheck, filterItem);
        }
        
        // Filtro exacto (comportamiento original)
        return itemToCheck.getItem() == filterItem.getItem();
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
        return ForgeHooks.getBurnTime(stack, null) > 0;
    }
    
    private static boolean isOreItem(ItemStack stack) {
        String itemName = stack.getItem().toString().toLowerCase();
        return itemName.contains("ore") || 
               itemName.contains("raw_") ||
               itemName.contains("ingot") ||
               itemName.contains("gem") ||
               itemName.contains("diamond") ||
               itemName.contains("emerald") ||
               itemName.contains("coal") ||
               itemName.contains("redstone") ||
               itemName.contains("lapis");
    }
    
    private static boolean isWeaponItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem || 
               item instanceof BowItem || 
               item instanceof CrossbowItem ||
               item instanceof TridentItem;
    }
    
    private static boolean passesCustomTagFilter(ItemStack itemToCheck, ItemStack filterItem) {
        if (!filterItem.hasTag()) {
            return false; // Sin tag configurado
        }
        
        String customTag = filterItem.getTag().getString("CustomTag");
        if (customTag.isEmpty()) {
            return false;
        }
        
        // Verificar si el item tiene el tag especificado
        return itemToCheck.getTags().anyMatch(tag -> tag.location().toString().equals(customTag));
    }
}