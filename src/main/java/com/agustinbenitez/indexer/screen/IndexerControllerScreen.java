package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.menu.IndexerControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.text.DecimalFormat;

@OnlyIn(Dist.CLIENT)
public class IndexerControllerScreen extends AbstractContainerScreen<IndexerControllerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("indexer", "textures/gui/indexer_controller_gui.png");
    

    
    public IndexerControllerScreen(IndexerControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 250;
        this.imageHeight = 120;
        this.inventoryLabelY = this.imageHeight - 10;
    }
    
    @Override
    protected void init() {
        super.init();
    }
    

    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Renderizar la textura personalizada del GUI
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Renderizar título centrado en la parte superior
        int titleWidth = this.font.width(this.title);
        int titleX = (this.imageWidth - titleWidth) / 2;
        guiGraphics.drawString(this.font, this.title, titleX, 6, 4210752, false);
        
        // Columna izquierda - Información de conexión
        guiGraphics.drawString(this.font, Component.translatable("gui.indexer.controller.connection_info"), 15, 20, 0x555555, false);
        
        String dropBoxText = Component.translatable("gui.indexer.controller.dropbox").getString() + ": " + (this.menu.hasDropContainer() ? Component.translatable("gui.indexer.controller.connected").getString() : "None");
        guiGraphics.drawString(this.font, dropBoxText, 15, 32, 4210752, false);
        
        String containersText = Component.translatable("gui.indexer.controller.containers").getString() + ": " + this.menu.getConnectedContainersCount();
        guiGraphics.drawString(this.font, containersText, 15, 44, 4210752, false);
        
        // Columna derecha - Información de capacidad
        guiGraphics.drawString(this.font, Component.translatable("gui.indexer.controller.capacity_info"), 125, 20, 0x555555, false);
        
        int totalCapacity = this.menu.getTotalCapacity();
        int occupiedSlots = this.menu.getOccupiedSlots();
        
        // Mostrar la capacidad total y la capacidad ocupada en items totales (multiplicando por 64 que es el tamaño de un stack)
        String capacityText = Component.translatable("gui.indexer.controller.slots").getString() + ": " + 
                              formatNumber(occupiedSlots * 64) + "/" + formatNumber(totalCapacity * 64);
        guiGraphics.drawString(this.font, capacityText, 125, 32, 4210752, false);
        
        // Mostrar la capacidad en términos de slots (sin multiplicar)
        String stacksText = Component.translatable("gui.indexer.controller.stacks").getString() + ": " + 
                            formatNumber(occupiedSlots) + "/" + formatNumber(totalCapacity);
        guiGraphics.drawString(this.font, stacksText, 125, 44, 4210752, false);
        
        // Información de velocidad
        String transferRateText = Component.translatable("gui.indexer.controller.speed").getString() + ": " + this.menu.getItemsPerTransfer() + " " + Component.translatable("gui.indexer.controller.items_at_once").getString();
        guiGraphics.drawString(this.font, transferRateText, 125, 56, 0x00AA00, false);
    }
    
    /**
     * Formats a number using condensed notation (K, M, etc.) for large values.
     * @param number The number to format
     * @return Formatted string representation of the number
     */
    private String formatNumber(int number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1000000) {
            return String.format("%.1fK", number / 1000.0).replace(".0K", "K");
        } else {
            return String.format("%.1fM", number / 1000000.0).replace(".0M", "M");
        }
    }
}