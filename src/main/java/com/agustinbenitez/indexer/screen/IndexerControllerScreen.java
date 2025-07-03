package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.menu.IndexerControllerMenu;
import com.agustinbenitez.indexer.network.ModNetworking;
import com.agustinbenitez.indexer.network.ToggleControllerPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class IndexerControllerScreen extends AbstractContainerScreen<IndexerControllerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    
    private Button toggleButton;
    
    public IndexerControllerScreen(IndexerControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 80;
        this.inventoryLabelY = this.imageHeight - 10;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Crear botón de encendido/apagado
        this.toggleButton = Button.builder(
            Component.literal(this.menu.isEnabled() ? "ON" : "OFF"),
            button -> {
                // Enviar paquete al servidor para alternar el estado
                ModNetworking.sendToServer(new ToggleControllerPacket());
            }
        ).bounds(this.leftPos + 10, this.topPos + 20, 40, 20).build();
        
        this.addRenderableWidget(this.toggleButton);
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        
        // Actualizar el texto del botón basado en el estado actual
        this.toggleButton.setMessage(Component.literal(this.menu.isEnabled() ? "ON" : "OFF"));
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Renderizar fondo personalizado para la información del controlador
        guiGraphics.fill(x, y, x + this.imageWidth, y + 80, 0xFFC6C6C6);
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 79, 0xFF8B8B8B);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Renderizar título
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        
        // Renderizar información del estado
        String statusText = "Estado: " + (this.menu.isEnabled() ? "Encendido" : "Apagado");
        guiGraphics.drawString(this.font, statusText, 60, 25, 4210752, false);
        
        // Renderizar información del DropBox
        String dropBoxText = "DropBox: " + (this.menu.hasDropChest() ? "Conectado" : "No conectado");
        guiGraphics.drawString(this.font, dropBoxText, 8, 40, 4210752, false);
        
        // Renderizar información de cofres conectados
        String chestsText = "Cofres: " + this.menu.getConnectedChestsCount();
        guiGraphics.drawString(this.font, chestsText, 8, 52, 4210752, false);
        
        // Renderizar espacios disponibles
        String slotsText = "Espacios: " + this.menu.getTotalAvailableSlots();
        guiGraphics.drawString(this.font, slotsText, 100, 52, 4210752, false);
    }
}