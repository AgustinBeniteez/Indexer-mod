package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.inventory.IndexerConnectorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class IndexerConnectorScreen extends AbstractContainerScreen<IndexerConnectorMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(IndexerMod.MOD_ID, "textures/gui/menu_conector.png");
    
    public IndexerConnectorScreen(IndexerConnectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166; // Altura estándar para incluir el inventario del jugador
        this.inventoryLabelY = 74; // Posición de la etiqueta del inventario
        this.titleLabelY = 6; // Posición del título
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Renderizar textura de fondo
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Renderizar título
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        
        // Renderizar etiqueta del inventario del jugador
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 4210752, false);
        
        // Renderizar texto "Filter"
        Component filterText = Component.literal("Filter");
        int textWidth = this.font.width(filterText);
        guiGraphics.drawString(this.font, filterText, (this.imageWidth - textWidth) / 2, 20, 4210752, false);
    }
}