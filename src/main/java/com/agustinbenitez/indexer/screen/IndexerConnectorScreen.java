package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.inventory.IndexerConnectorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class IndexerConnectorScreen extends AbstractContainerScreen<IndexerConnectorMenu> {
    private static final ResourceLocation TEXTURE_BASE = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "textures/gui/menu_conector.png");
    private static final ResourceLocation TEXTURE_LVL2 = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "textures/gui/menu_conector_lvl2.png");
    
    public IndexerConnectorScreen(IndexerConnectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // Ajustar ancho según el número de slots de filtro
        this.imageWidth = (menu.getFilterSlots() == 18) ? 256 : 176; // 256 para textura LVL2
        this.imageHeight = 166; // Altura estándar para incluir el inventario del jugador
        this.inventoryLabelY = 74; // Posición de la etiqueta del inventario
        this.titleLabelY = 6; // Posición del título
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Seleccionar textura según niveles/slots del conector
        ResourceLocation tex = (this.menu.getFilterSlots() == 18) ? TEXTURE_LVL2 : TEXTURE_BASE;
        guiGraphics.blit(tex, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Se han eliminado las etiquetas de título e inventario
    }
}