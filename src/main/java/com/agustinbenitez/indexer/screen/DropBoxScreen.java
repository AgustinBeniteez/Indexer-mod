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
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("indexer", "textures/gui/drop_box.png");
    
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
        
        // Position the transfer all button at bottom right between the two inventories
        int buttonX = this.leftPos + 157; // Right side of the DropBox inventory area
        int buttonY = this.topPos + 125; // Bottom area between DropBox and player inventories
        
        this.transferAllButton = Button.builder(Component.literal("↑"), button -> {
            // Send packet to server to transfer all items
            com.agustinbenitez.indexer.network.ModNetworking.sendToServer(new com.agustinbenitez.indexer.network.TransferAllItemsPacket());
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
        
        if (!this.menu.isControllerConnected()) {
            statsY += 20;
            Component errorText = Component.translatable("gui.indexer.manager.controller_not_found");
            // Wrap text if needed or just display it
            guiGraphics.drawWordWrap(this.font, errorText, statsX, statsY, STATS_PANEL_WIDTH - 12, 0xFF5555);
            
            // Also render in the main area to be more prominent
            int areaX = this.leftPos + 8;
            int areaW = 176 - 16;
            int areaY = this.topPos + 24;
            int textWidth = this.font.width(errorText);
            int tx = areaX + (areaW - textWidth) / 2;
            int ty = areaY + 20;
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300); // Render on top of everything
            guiGraphics.drawString(this.font, errorText, tx, ty, 0xFF5555, false);
            guiGraphics.pose().popPose();
            return;
        }
        
        // Get stats from menu
        int occupiedSlots = this.menu.getOccupiedSlots();
        int totalCapacity = this.menu.getTotalCapacity();
        int connectedContainers = this.menu.getConnectedContainersCount();
        int upgradeLevel = this.menu.getUpgradeLevel();
        
        // Upgrade level indicator
        statsY += 15;
        String speedMultiplier;
        switch (upgradeLevel) {
            case 1: speedMultiplier = "x5"; break;
            case 2: speedMultiplier = "x10"; break;
            case 3: speedMultiplier = "x20"; break;
            case 4: speedMultiplier = "x64"; break;
            case 5: speedMultiplier = "x256"; break;
            default: speedMultiplier = "x1"; break;
        }
        Component speedText = Component.literal("Speed: " + speedMultiplier);
        int speedColor = upgradeLevel > 0 ? 0x55FF55 : 0xCCCCCC;
        guiGraphics.drawString(this.font, speedText, statsX, statsY, speedColor, false);
        if (upgradeLevel > 0) {
            net.minecraft.world.item.ItemStack upgradeItem = getUpgradeItemForLevel(upgradeLevel);
            if (!upgradeItem.isEmpty()) {
                int textWidth = this.font.width(speedText.getString());
                guiGraphics.renderItem(upgradeItem, statsX + textWidth + 6, statsY - 2);
            }
        }
        
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
    
    private net.minecraft.world.item.ItemStack getUpgradeItemForLevel(int level) {
        switch (level) {
            case 0: return new net.minecraft.world.item.ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_ZERO);
            case 1: return new net.minecraft.world.item.ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_BASIC);
            case 2: return new net.minecraft.world.item.ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_COPPER);
            case 3: return new net.minecraft.world.item.ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_ADVANCED);
            case 4: return new net.minecraft.world.item.ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_ELITE);
            case 5: return new net.minecraft.world.item.ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_DEFINITIVE);
            default: return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
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
