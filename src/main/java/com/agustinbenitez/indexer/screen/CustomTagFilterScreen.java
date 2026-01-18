package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.network.CustomTagFilterPacket;
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
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CustomTagFilterScreen extends Screen {
    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 200;
    
    private EditBox tagEditBox;
    private Button confirmButton;
    private Button cancelButton;
    private final ItemStack filterItem;
    private final int slotIndex;
    
    private List<String> suggestions = new ArrayList<>();
    private int selectedSuggestion = -1;
    private boolean showSuggestions = false;
    
    public CustomTagFilterScreen(ItemStack filterItem, int slotIndex) {
        super(Component.translatable("gui.indexer.custom_tag_blocker.title"));
        this.filterItem = filterItem;
        this.slotIndex = slotIndex;
    }
    
    @Override
    protected void init() {
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Campo de texto para el tag
        this.tagEditBox = new EditBox(this.font, guiLeft + 20, guiTop + 60, GUI_WIDTH - 40, 20, 
                Component.translatable("gui.indexer.custom_tag_blocker.tag_input"));
        
        // Configurar el campo de texto para mejor visibilidad
        this.tagEditBox.setBordered(true);
        this.tagEditBox.setVisible(true);
        this.tagEditBox.setEditable(true);
        this.tagEditBox.setTextColor(0xFFFFFF); // Texto blanco
        this.tagEditBox.setTextColorUneditable(0xA0A0A0);
        this.tagEditBox.setMaxLength(100);
        
        CustomData customData = this.filterItem.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("custom_tag_filter")) {
                this.tagEditBox.setValue(tag.getString("custom_tag_filter"));
            } else if (tag.contains("custom_tag")) {
                this.tagEditBox.setValue(tag.getString("custom_tag"));
            }
        }
        
        this.tagEditBox.setResponder(this::onTagChanged);
        this.addRenderableWidget(this.tagEditBox);
        
        // Botón confirmar
        this.confirmButton = Button.builder(Component.translatable("gui.indexer.custom_tag_blocker.confirm"), 
                button -> this.confirmTag())
                .bounds(guiLeft + 20, guiTop + 150, 80, 20)
                .build();
        this.addRenderableWidget(this.confirmButton);
        
        // Botón cancelar
        this.cancelButton = Button.builder(Component.translatable("gui.indexer.custom_tag_blocker.cancel"), 
                button -> this.onClose())
                .bounds(guiLeft + 120, guiTop + 150, 80, 20)
                .build();
        this.addRenderableWidget(this.cancelButton);
        
        this.setInitialFocus(this.tagEditBox);
    }
    
    private void onTagChanged(String text) {
        if (text.isEmpty()) {
            this.suggestions.clear();
            this.showSuggestions = false;
            return;
        }
        
        // Generar sugerencias basadas en items registrados
        this.suggestions = BuiltInRegistries.ITEM.keySet().stream()
                .map(ResourceLocation::toString)
                .filter(itemName -> itemName.toLowerCase().contains(text.toLowerCase()))
                .limit(10)
                .collect(Collectors.toList());
        
        // También agregar sugerencias de tags comunes
        List<String> commonTags = List.of(
                "minecraft:logs", "minecraft:planks", "minecraft:stone_bricks",
                "minecraft:wool", "minecraft:flowers", "minecraft:saplings",
                "minecraft:ingots", "minecraft:gems", "minecraft:ores", "minecraft:dusts",
                "minecraft:storage_blocks", "minecraft:tools", "minecraft:armor"
        );
        
        for (String tag : commonTags) {
            if (tag.toLowerCase().contains(text.toLowerCase()) && !this.suggestions.contains(tag)) {
                this.suggestions.add(tag);
            }
        }
        
        this.showSuggestions = !this.suggestions.isEmpty();
        this.selectedSuggestion = -1;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.tagEditBox.isFocused() && this.showSuggestions && !this.suggestions.isEmpty()) {
            if (keyCode == 264) { // Flecha abajo
                this.selectedSuggestion = Math.min(this.selectedSuggestion + 1, this.suggestions.size() - 1);
                return true;
            } else if (keyCode == 265) { // Flecha arriba
                this.selectedSuggestion = Math.max(this.selectedSuggestion - 1, -1);
                return true;
            } else if (keyCode == 257 || keyCode == 335) { // Enter o Tab
                if (this.selectedSuggestion >= 0 && this.selectedSuggestion < this.suggestions.size()) {
                    this.tagEditBox.setValue(this.suggestions.get(this.selectedSuggestion));
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
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500);
        guiGraphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xC0101010);
        guiGraphics.fill(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, 0xFF2D2D30);
        guiGraphics.pose().popPose();
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 600);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().popPose();
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 700);
        Component title = Component.translatable("gui.indexer.custom_tag_blocker.title");
        guiGraphics.drawCenteredString(this.font, title, guiLeft + GUI_WIDTH / 2, guiTop + 20, 0xFFFFFF);
        Component instructions = Component.translatable("gui.indexer.custom_tag_blocker.instructions");
        guiGraphics.drawString(this.font, instructions, guiLeft + 20, guiTop + 40, 0xCCCCCC, false);
        guiGraphics.pose().popPose();
        
        if (this.showSuggestions && !this.suggestions.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 750);
            int suggestionsX = this.tagEditBox.getX();
            int suggestionsY = this.tagEditBox.getY() + this.tagEditBox.getHeight() + 2;
            int suggestionsWidth = this.tagEditBox.getWidth();
            int suggestionHeight = 12;
            int maxSuggestions = Math.min(this.suggestions.size(), 8);
            guiGraphics.fill(suggestionsX, suggestionsY, 
                    suggestionsX + suggestionsWidth, 
                    suggestionsY + maxSuggestions * suggestionHeight, 0xE0000000);
            for (int i = 0; i < maxSuggestions; i++) {
                String suggestion = this.suggestions.get(i);
                int y = suggestionsY + i * suggestionHeight;
                if (i == this.selectedSuggestion) {
                    guiGraphics.fill(suggestionsX, y, suggestionsX + suggestionsWidth, y + suggestionHeight, 0xFF4A4A4A);
                }
                guiGraphics.drawString(this.font, suggestion, suggestionsX + 4, y + 2, 0xFFFFFF, false);
            }
            guiGraphics.pose().popPose();
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Verificar si se hizo clic en una sugerencia
        if (this.showSuggestions && !this.suggestions.isEmpty()) {
            int suggestionsX = this.tagEditBox.getX();
            int suggestionsY = this.tagEditBox.getY() + this.tagEditBox.getHeight() + 2;
            int suggestionsWidth = this.tagEditBox.getWidth();
            int suggestionHeight = 12;
            int maxSuggestions = Math.min(this.suggestions.size(), 8);
            
            if (mouseX >= suggestionsX && mouseX <= suggestionsX + suggestionsWidth &&
                mouseY >= suggestionsY && mouseY <= suggestionsY + maxSuggestions * suggestionHeight) {
                
                int clickedIndex = (int) ((mouseY - suggestionsY) / suggestionHeight);
                if (clickedIndex >= 0 && clickedIndex < this.suggestions.size()) {
                    this.tagEditBox.setValue(this.suggestions.get(clickedIndex));
                    this.showSuggestions = false;
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void confirmTag() {
        String tag = this.tagEditBox.getValue().trim();
        if (!tag.isEmpty()) {
            // Enviar packet al servidor para actualizar el filtro
            ModNetworking.sendToServer(new CustomTagFilterPacket(this.slotIndex, tag));
        }
        this.onClose();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
