package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.menu.DropBoxMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;

public class DropBoxScreen extends AbstractContainerScreen<DropBoxMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("indexer", "textures/gui/drop_box.png");
    
    private Button transferAllButton;
    private static final int STATS_PANEL_WIDTH = 100;
    private static final int STATS_PANEL_HEIGHT = 166;

    public DropBoxScreen(DropBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176 + STATS_PANEL_WIDTH; // Expand width to accommodate stats panel
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        
        // Position the transfer all button (small green arrow button)
        int buttonX = this.leftPos + 157; // Position on the main DropBox GUI
        int buttonY = this.topPos + 4;
        
        this.transferAllButton = Button.builder(Component.literal("↑"), button -> {
            // Send packet to server to transfer all items
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.menu.transferAllItemsToDropBox(this.minecraft.player);
            }
        })
        .bounds(buttonX, buttonY, 12, 12)
        .build();
        
        this.addRenderableWidget(this.transferAllButton);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Render main DropBox GUI first
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 176, 222);
        
        // Render stats panel background on the right side
        int statsX = this.leftPos + 176;
        guiGraphics.fill(statsX, this.topPos, statsX + STATS_PANEL_WIDTH, this.topPos + STATS_PANEL_HEIGHT, 0xC0101010);
        
        // Render stats content
        renderStats(guiGraphics, mouseX, mouseY);
    }

    private void renderStats(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int statsX = this.leftPos + 176 + 6; // Position on the right side
        int statsY = this.topPos + 8;
        
        // Title
        Component statsTitle = Component.translatable("gui.indexer.stats");
        guiGraphics.drawString(this.font, statsTitle, statsX, statsY, 0xFFFFFF, false);
        
        // Get stats from menu
        int occupiedSlots = this.menu.getOccupiedSlots();
        int totalCapacity = this.menu.getTotalCapacity();
        int connectedContainers = this.menu.getConnectedContainersCount();
        boolean isEnabled = this.menu.isControllerEnabled();
        
        // Status indicator
        statsY += 15;
        Component statusText = isEnabled ? 
            Component.translatable("gui.indexer.status.enabled") : 
            Component.translatable("gui.indexer.status.disabled");
        int statusColor = isEnabled ? 0x55FF55 : 0xFF5555;
        guiGraphics.drawString(this.font, statusText, statsX, statsY, statusColor, false);
        
        // Capacity section
        statsY += 20;
        Component capacityTitle = Component.translatable("gui.indexer.capacity");
        guiGraphics.drawString(this.font, capacityTitle, statsX, statsY, 0xFFFFFF, false);
        
        // Capacity bar
        statsY += 12;
        int barWidth = 88;
        int barHeight = 6;
        
        // Background bar
        guiGraphics.fill(statsX, statsY, statsX + barWidth, statsY + barHeight, 0xFF333333);
        
        // Filled bar
        if (totalCapacity > 0) {
            float fillPercentage = (float) occupiedSlots / totalCapacity;
            int fillWidth = (int) (barWidth * fillPercentage);
            
            // Color based on fill percentage
            int barColor;
            if (fillPercentage < 0.5f) {
                barColor = 0xFF55FF55; // Green
            } else if (fillPercentage < 0.8f) {
                barColor = 0xFFFFAA00; // Orange
            } else {
                barColor = 0xFFFF5555; // Red
            }
            
            guiGraphics.fill(statsX, statsY, statsX + fillWidth, statsY + barHeight, barColor);
        }
        
        // Capacity text
        statsY += 10;
        String capacityText = occupiedSlots + " / " + totalCapacity;
        if (totalCapacity > 0) {
            int percentage = (int) ((float) occupiedSlots / totalCapacity * 100);
            capacityText += " (" + percentage + "%)";
        }
        guiGraphics.drawString(this.font, capacityText, statsX, statsY, 0xCCCCCC, false);
        
        // Connected containers section
        statsY += 20;
        Component containersTitle = Component.translatable("gui.indexer.connected_containers");
        guiGraphics.drawString(this.font, containersTitle, statsX, statsY, 0xFFFFFF, false);
        
        statsY += 12;
        Component containersCount = Component.literal(String.valueOf(connectedContainers));
        guiGraphics.drawString(this.font, containersCount, statsX, statsY, 0xCCCCCC, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render labels in their original positions since the main GUI is now at leftPos
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}