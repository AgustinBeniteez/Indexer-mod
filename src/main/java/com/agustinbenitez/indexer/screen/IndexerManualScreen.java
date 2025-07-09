package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.IndexerMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Pantalla GUI para el manual del Indexer.
 * Muestra instrucciones paso a paso con imágenes y permite navegar entre páginas.
 */
public class IndexerManualScreen extends Screen {
    // Constantes para la pantalla
    private static final int SCREEN_WIDTH = 271;
    private static final int SCREEN_HEIGHT = 180;
    private static final ResourceLocation BACKGROUND = new ResourceLocation(IndexerMod.MOD_ID, "textures/gui/manual/manualgui.png");
    
    // Botones de navegación
    private Button nextButton;
    private Button prevButton;
    private Button closeButton;
    
    // Estado de la pantalla
    private int currentPage = 0;
    private final int totalPages = 8; // Número total de páginas disponibles
    
    // Recursos para las imágenes de cada página
    private final ResourceLocation[] pageImages = new ResourceLocation[totalPages];
    
    public IndexerManualScreen() {
        super(Component.translatable("item.indexer.indexer_manual"));
        
        // Initialize page images
        for (int i = 0; i < totalPages; i++) {
            pageImages[i] = new ResourceLocation(IndexerMod.MOD_ID, "textures/gui/manual/manual" + (i + 1) + ".png");
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Calcular posiciones centradas
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int leftPos = centerX - (SCREEN_WIDTH / 2);
        int topPos = centerY - (SCREEN_HEIGHT / 2);
        
        // Crear botones de navegación
        this.prevButton = Button.builder(
            Component.literal("<"),
            button -> previousPage()
        ).bounds(leftPos + 10, topPos + SCREEN_HEIGHT - 30, 20, 20).build();
        
        this.nextButton = Button.builder(
            Component.literal(">"),
            button -> nextPage()
        ).bounds(leftPos + SCREEN_WIDTH - 30, topPos + SCREEN_HEIGHT - 30, 20, 20).build();
        
        this.closeButton = Button.builder(
            Component.literal("X"),
            button -> this.onClose()
        ).bounds(leftPos + SCREEN_WIDTH - 25, topPos + 5, 20, 20).build();
        
        // Añadir botones a la pantalla
        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(closeButton);
        
        // Actualizar estado de los botones
        updateButtonStates();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Renderizar fondo oscuro
        this.renderBackground(guiGraphics);
        
        // Calcular posiciones centradas
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int leftPos = centerX - (SCREEN_WIDTH / 2);
        int topPos = centerY - (SCREEN_HEIGHT / 2);
        
        // Renderizar fondo del manual
        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, SCREEN_WIDTH, SCREEN_HEIGHT);
        
        // Renderizar número de página
        String pageText = (currentPage + 1) + "/" + totalPages;
        guiGraphics.drawString(this.font, pageText, centerX - (this.font.width(pageText) / 2), topPos + SCREEN_HEIGHT - 15, 0xFFFFFF, false);
        
        // Renderizar imagen de la página actual
        guiGraphics.blit(pageImages[currentPage], leftPos + 28, topPos + 30, 0, 0, 200, 120, 200, 120);
        
        // Renderizar botones y otros elementos
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    /**
     * Renders the specific content for each page
     */
    private void renderPageContent(GuiGraphics guiGraphics, int leftPos, int topPos) {
        // No text content - removed as requested
    }
    
    /**
     * Advances to the next page
     */
    private void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            updateButtonStates();
        }
    }
    
    /**
     * Goes back to the previous page
     */
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateButtonStates();
        }
    }
    
    /**
     * Updates the state of navigation buttons
     */
    private void updateButtonStates() {
        this.prevButton.active = currentPage > 0;
        this.nextButton.active = currentPage < totalPages - 1;
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // No pausar el juego cuando se muestra esta pantalla
    }
}