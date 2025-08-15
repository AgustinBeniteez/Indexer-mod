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
        guiGraphics.drawString(this.font, "Connection Info:", 15, 20, 0x555555, false);
        
        String dropBoxText = "DropBox: " + (this.menu.hasDropContainer() ? "Connected" : "None");
        guiGraphics.drawString(this.font, dropBoxText, 15, 32, 4210752, false);
        
        String containersText = "Containers: " + this.menu.getConnectedContainersCount();
        guiGraphics.drawString(this.font, containersText, 15, 44, 4210752, false);
        
        // Columna derecha - Información de capacidad
        guiGraphics.drawString(this.font, "Capacity Info:", 125, 20, 0x555555, false);
        
        String slotsText = "Slots: " + formatNumber(this.menu.getTotalAvailableSlots());
        guiGraphics.drawString(this.font, slotsText, 125, 32, 4210752, false);
        
        // Información de velocidad
        String transferRateText = "Speed: " + this.menu.getItemsPerTransfer() + " items at once";
        guiGraphics.drawString(this.font, transferRateText, 125, 44, 0x00AA00, false);
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