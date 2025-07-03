package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.menu.DropBoxMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DropBoxScreen extends AbstractContainerScreen<DropBoxMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(IndexerMod.MOD_ID, "textures/gui/drop_box.png");

    public DropBoxScreen(DropBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 222; // Altura para 6 filas del DropBox + inventario del jugador
        this.inventoryLabelY = this.imageHeight - 94; // Ajustar posición del label del inventario
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}