package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.network.AttributeFilterPacket;
import com.agustinbenitez.indexer.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AttributeFilterScreen extends Screen {
    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 200;
    
    private EditBox attributeEditBox;
    private Button confirmButton;
    private Button cancelButton;
    private final ItemStack filterItem;
    private final int slotIndex;
    
    private List<String> suggestions = new ArrayList<>();
    private int selectedSuggestion = -1;
    private boolean showSuggestions = false;
    
    public AttributeFilterScreen(ItemStack filterItem, int slotIndex) {
        super(Component.translatable("gui.indexer.attribute_filter.title"));
        this.filterItem = filterItem;
        this.slotIndex = slotIndex;
    }
    
    @Override
    protected void init() {
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Campo de texto para el atributo
        this.attributeEditBox = new EditBox(this.font, guiLeft + 20, guiTop + 60, GUI_WIDTH - 40, 20, 
                Component.translatable("gui.indexer.attribute_filter.attribute_input"));
        
        // Configurar el campo de texto para mejor visibilidad
        this.attributeEditBox.setBordered(true);
        this.attributeEditBox.setVisible(true);
        this.attributeEditBox.setEditable(true);
        this.attributeEditBox.setTextColor(0xFFFFFF); // Texto blanco
        this.attributeEditBox.setTextColorUneditable(0xA0A0A0);
        this.attributeEditBox.setMaxLength(100);
        
        CustomData customData = this.filterItem.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("attribute_filter")) {
                this.attributeEditBox.setValue(tag.getString("attribute_filter"));
            }
        }
        
        this.attributeEditBox.setResponder(this::onAttributeChanged);
        this.addRenderableWidget(this.attributeEditBox);
        
        // Botón confirmar
        this.confirmButton = Button.builder(Component.translatable("gui.indexer.attribute_filter.confirm"), 
                button -> this.confirmAttribute())
                .bounds(guiLeft + 20, guiTop + 150, 80, 20)
                .build();
        this.addRenderableWidget(this.confirmButton);
        
        // Botón cancelar
        this.cancelButton = Button.builder(Component.translatable("gui.indexer.attribute_filter.cancel"), 
                button -> this.onClose())
                .bounds(guiLeft + 120, guiTop + 150, 80, 20)
                .build();
        this.addRenderableWidget(this.cancelButton);
        
        this.setFocused(this.attributeEditBox);
    }
    
    private void onAttributeChanged(String text) {
        if (text.isEmpty()) {
            this.suggestions.clear();
            this.showSuggestions = false;
            return;
        }
        
        // Generar sugerencias basadas en encantamientos
        var registryAccess = net.minecraft.client.Minecraft.getInstance().level != null ? net.minecraft.client.Minecraft.getInstance().level.registryAccess() : null;
        if (registryAccess != null) {
            this.suggestions = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).keySet().stream()
                    .map(ResourceLocation::toString)
                    .filter(enchantName -> enchantName.toLowerCase().contains(text.toLowerCase()))
                    .limit(10)
                    .collect(Collectors.toList());
        } else {
            this.suggestions = new ArrayList<>();
        }
        
        // También agregar sugerencias de atributos comunes
        List<String> commonAttributes = List.of(
                "minecraft:sharpness", "minecraft:efficiency", "minecraft:unbreaking",
                "minecraft:fortune", "minecraft:silk_touch", "minecraft:mending",
                "minecraft:protection", "minecraft:fire_protection", "minecraft:blast_protection",
                "minecraft:projectile_protection", "minecraft:thorns", "minecraft:respiration",
                "minecraft:aqua_affinity", "minecraft:depth_strider", "minecraft:frost_walker",
                "minecraft:feather_falling", "minecraft:looting", "minecraft:knockback",
                "minecraft:fire_aspect", "minecraft:sweeping", "minecraft:power",
                "minecraft:punch", "minecraft:flame", "minecraft:infinity"
        );
        
        for (String attribute : commonAttributes) {
            if (attribute.toLowerCase().contains(text.toLowerCase()) && !this.suggestions.contains(attribute)) {
                this.suggestions.add(attribute);
            }
        }
        
        this.showSuggestions = !this.suggestions.isEmpty();
        this.selectedSuggestion = -1;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.attributeEditBox.isFocused() && this.showSuggestions && !this.suggestions.isEmpty()) {
            if (keyCode == 264) { // Flecha abajo
                this.selectedSuggestion = Math.min(this.selectedSuggestion + 1, this.suggestions.size() - 1);
                return true;
            } else if (keyCode == 265) { // Flecha arriba
                this.selectedSuggestion = Math.max(this.selectedSuggestion - 1, -1);
                return true;
            } else if (keyCode == 257 || keyCode == 335) { // Enter o Tab
                if (this.selectedSuggestion >= 0 && this.selectedSuggestion < this.suggestions.size()) {
                    this.attributeEditBox.setValue(this.suggestions.get(this.selectedSuggestion));
                    this.showSuggestions = false;
                    return true;
                }
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Fondo de la GUI
        guiGraphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xC0101010);
        guiGraphics.fill(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, 0xFF2D2D30);
        
        // Título
        Component title = Component.translatable("gui.indexer.attribute_filter.title");
        guiGraphics.drawCenteredString(this.font, title, guiLeft + GUI_WIDTH / 2, guiTop + 20, 0xFFFFFF);
        
        // Instrucciones
        Component instructions = Component.translatable("gui.indexer.attribute_filter.instructions");
        guiGraphics.drawString(this.font, instructions, guiLeft + 20, guiTop + 40, 0xCCCCCC, false);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Renderizar sugerencias
        if (this.showSuggestions && !this.suggestions.isEmpty()) {
            int suggestionsX = this.attributeEditBox.getX();
            int suggestionsY = this.attributeEditBox.getY() + this.attributeEditBox.getHeight() + 2;
            int suggestionsWidth = this.attributeEditBox.getWidth();
            int suggestionHeight = 12;
            int maxSuggestions = Math.min(this.suggestions.size(), 8);
            
            // Fondo de las sugerencias
            guiGraphics.fill(suggestionsX, suggestionsY, 
                    suggestionsX + suggestionsWidth, 
                    suggestionsY + maxSuggestions * suggestionHeight, 0xE0000000);
            
            for (int i = 0; i < maxSuggestions; i++) {
                String suggestion = this.suggestions.get(i);
                int y = suggestionsY + i * suggestionHeight;
                
                // Resaltar la sugerencia seleccionada
                if (i == this.selectedSuggestion) {
                    guiGraphics.fill(suggestionsX, y, suggestionsX + suggestionsWidth, y + suggestionHeight, 0xFF4A4A4A);
                }
                
                guiGraphics.drawString(this.font, suggestion, suggestionsX + 4, y + 2, 0xFFFFFF, false);
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.showSuggestions && !this.suggestions.isEmpty()) {
            int suggestionsX = this.attributeEditBox.getX();
            int suggestionsY = this.attributeEditBox.getY() + this.attributeEditBox.getHeight() + 2;
            int suggestionsWidth = this.attributeEditBox.getWidth();
            int suggestionHeight = 12;
            int maxSuggestions = Math.min(this.suggestions.size(), 8);
            
            if (mouseX >= suggestionsX && mouseX <= suggestionsX + suggestionsWidth &&
                mouseY >= suggestionsY && mouseY <= suggestionsY + maxSuggestions * suggestionHeight) {
                
                int clickedIndex = (int) ((mouseY - suggestionsY) / suggestionHeight);
                if (clickedIndex >= 0 && clickedIndex < maxSuggestions) {
                    this.attributeEditBox.setValue(this.suggestions.get(clickedIndex));
                    this.showSuggestions = false;
                    return true;
                }
            } else {
                this.showSuggestions = false;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void confirmAttribute() {
        String attribute = this.attributeEditBox.getValue().trim();
        if (!attribute.isEmpty()) {
            // Enviar packet al servidor para actualizar el filtro
            ModNetworking.sendToServer(new AttributeFilterPacket(this.slotIndex, attribute));
        }
        this.onClose();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
