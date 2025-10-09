package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.item.ModFilterItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModFilterScreen extends Screen {
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 200;
    
    private final ItemStack filterItem;
    private final int slotIndex;
    private EditBox modIdEditBox;
    private Button confirmButton;
    private Button cancelButton;
    
    private List<String> suggestions = new ArrayList<>();
    private boolean showSuggestions = false;
    private int selectedSuggestion = -1;
    
    public ModFilterScreen(ItemStack filterItem, int slotIndex) {
        super(Component.translatable("gui.indexer.mod_filter.title"));
        this.filterItem = filterItem;
        this.slotIndex = slotIndex;
    }
    
    @Override
    protected void init() {
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Campo de texto para el mod ID
        this.modIdEditBox = new EditBox(this.font, guiLeft + 20, guiTop + 60, GUI_WIDTH - 40, 20, 
                Component.translatable("gui.indexer.mod_filter.mod_input"));
        
        // Configurar el campo de texto
        this.modIdEditBox.setBordered(true);
        this.modIdEditBox.setVisible(true);
        this.modIdEditBox.setEditable(true);
        this.modIdEditBox.setTextColor(0xFFFFFF);
        this.modIdEditBox.setTextColorUneditable(0xA0A0A0);
        this.modIdEditBox.setMaxLength(50);
        this.modIdEditBox.setResponder(this::onModIdChanged);
        
        // Cargar el mod ID actual si existe
        if (this.filterItem.getItem() instanceof ModFilterItem modFilter) {
            String currentModId = modFilter.getModId(this.filterItem);
            this.modIdEditBox.setValue(currentModId);
        }
        
        this.addRenderableWidget(this.modIdEditBox);
        this.setInitialFocus(this.modIdEditBox);
        
        // Botón de confirmar
        this.confirmButton = Button.builder(Component.translatable("gui.indexer.mod_filter.confirm"), 
                button -> this.confirmModId())
                .bounds(guiLeft + 20, guiTop + 150, 80, 20)
                .build();
        this.addRenderableWidget(this.confirmButton);
        
        // Botón de cancelar
        this.cancelButton = Button.builder(Component.translatable("gui.indexer.mod_filter.cancel"), 
                button -> this.onClose())
                .bounds(guiLeft + 110, guiTop + 150, 80, 20)
                .build();
        this.addRenderableWidget(this.cancelButton);
        
        // Generar sugerencias iniciales
        this.generateSuggestions("");
    }
    
    private void onModIdChanged(String text) {
        this.generateSuggestions(text);
        this.selectedSuggestion = -1;
    }
    
    private void generateSuggestions(String input) {
        if (input.isEmpty()) {
            // Mostrar mods más comunes cuando no hay input
            this.suggestions = List.of("minecraft", "create", "mekanism", "thermal", "immersiveengineering", 
                                     "botania", "ae2", "refinedstorage", "industrialforegoing", "enderio");
            this.showSuggestions = true;
        } else {
            // Filtrar mods basado en el input
            this.suggestions = ForgeRegistries.ITEMS.getKeys().stream()
                    .map(ResourceLocation::getNamespace)
                    .distinct()
                    .filter(modId -> modId.toLowerCase().contains(input.toLowerCase()))
                    .limit(10)
                    .collect(Collectors.toList());
            
            this.showSuggestions = !this.suggestions.isEmpty();
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.showSuggestions && !this.suggestions.isEmpty()) {
            // Navegación con flechas
            if (keyCode == 264) { // Flecha abajo
                this.selectedSuggestion = Math.min(this.selectedSuggestion + 1, this.suggestions.size() - 1);
                return true;
            } else if (keyCode == 265) { // Flecha arriba
                this.selectedSuggestion = Math.max(this.selectedSuggestion - 1, -1);
                return true;
            } else if (keyCode == 257 || keyCode == 335) { // Enter o Tab
                if (this.selectedSuggestion >= 0 && this.selectedSuggestion < this.suggestions.size()) {
                    this.modIdEditBox.setValue(this.suggestions.get(this.selectedSuggestion));
                    this.showSuggestions = false;
                    return true;
                }
            } else if (keyCode == 256) { // Escape
                this.showSuggestions = false;
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.showSuggestions && !this.suggestions.isEmpty()) {
            int guiLeft = (this.width - GUI_WIDTH) / 2;
            int guiTop = (this.height - GUI_HEIGHT) / 2;
            int suggestionY = guiTop + 85;
            
            for (int i = 0; i < Math.min(this.suggestions.size(), 5); i++) {
                if (mouseX >= guiLeft + 20 && mouseX <= guiLeft + GUI_WIDTH - 20 &&
                    mouseY >= suggestionY + i * 12 && mouseY <= suggestionY + i * 12 + 12) {
                    this.modIdEditBox.setValue(this.suggestions.get(i));
                    this.showSuggestions = false;
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void confirmModId() {
        String modId = this.modIdEditBox.getValue().trim();
        
        if (this.filterItem.getItem() instanceof ModFilterItem modFilter) {
            modFilter.setModId(this.filterItem, modId);
        }
        
        this.onClose();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Fondo de la GUI
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xC0101010);
        guiGraphics.fill(guiLeft + 2, guiTop + 2, guiLeft + GUI_WIDTH - 2, guiTop + GUI_HEIGHT - 2, 0xFF383838);
        
        // Título
        Component title = Component.translatable("gui.indexer.mod_filter.title");
        guiGraphics.drawCenteredString(this.font, title, guiLeft + GUI_WIDTH / 2, guiTop + 20, 0xFFFFFF);
        
        // Etiqueta del campo de texto
        Component label = Component.translatable("gui.indexer.mod_filter.mod_label");
        guiGraphics.drawString(this.font, label, guiLeft + 20, guiTop + 45, 0xFFFFFF);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Renderizar sugerencias
        if (this.showSuggestions && !this.suggestions.isEmpty()) {
            int suggestionY = guiTop + 85;
            int maxSuggestions = Math.min(this.suggestions.size(), 5);
            
            // Fondo de las sugerencias
            guiGraphics.fill(guiLeft + 20, suggestionY, guiLeft + GUI_WIDTH - 20, 
                           suggestionY + maxSuggestions * 12, 0xE0000000);
            
            for (int i = 0; i < maxSuggestions; i++) {
                String suggestion = this.suggestions.get(i);
                int color = (i == this.selectedSuggestion) ? 0xFFFFFF00 : 0xFFAAAAAA;
                
                if (i == this.selectedSuggestion) {
                    guiGraphics.fill(guiLeft + 20, suggestionY + i * 12, guiLeft + GUI_WIDTH - 20, 
                                   suggestionY + i * 12 + 12, 0x80FFFFFF);
                }
                
                guiGraphics.drawString(this.font, suggestion, guiLeft + 25, suggestionY + i * 12 + 2, color);
            }
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}