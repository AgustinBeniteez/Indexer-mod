package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.IndexerMod;
import com.agustinbenitez.indexer.menu.DropBoxMenu;
import com.agustinbenitez.indexer.network.ModNetworking;
import com.agustinbenitez.indexer.network.TransferAllItemsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DropBoxScreen extends AbstractContainerScreen<DropBoxMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private Button transferAllButton;

    public DropBoxScreen(DropBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 222; // Altura para 6 filas del DropBox + inventario del jugador
        this.inventoryLabelY = this.imageHeight - 94; // Ajustar posición del label del inventario
    }

    @Override
    protected void init() {
        super.init();
        
        // Agregar botón para transferir todos los items
        // Posicionarlo entre el dropbox y el inventario del jugador, más pequeño y más abajo
        int buttonX = this.leftPos + this.imageWidth - 18; // Completamente a la derecha
        int buttonY = this.topPos + 125; // Un poco más arriba
        
        this.transferAllButton = Button.builder(
                Component.literal("↑").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)), // Flecha hacia arriba y verde
                button -> {
                    // Enviar paquete al servidor para transferir todos los items
                    ModNetworking.sendToServer(new TransferAllItemsPacket());
                })
                .bounds(buttonX, buttonY, 12, 12) // Botón más pequeño
                .build();
        
        this.addRenderableWidget(this.transferAllButton);
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