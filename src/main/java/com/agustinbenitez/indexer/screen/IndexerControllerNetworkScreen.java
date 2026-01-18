package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.menu.IndexerControllerNetworkMenu;
import com.agustinbenitez.indexer.network.ModNetworking;
import com.agustinbenitez.indexer.network.RefreshNetworkPacket;
import com.agustinbenitez.indexer.network.ContainerListUpdatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;

import java.text.DecimalFormat;
import java.util.*;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class IndexerControllerNetworkScreen extends AbstractContainerScreen<IndexerControllerNetworkMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("indexer:textures/gui/indexer_controller_network_gui.png");
    
    // Dimensiones de la GUI
    private static final int GUI_WIDTH = 400;
    private static final int GUI_HEIGHT = 240;
    
    // Panel izquierdo - Lista de contenedores
    private static final int LEFT_PANEL_X = 8;
    private static final int LEFT_PANEL_Y = 20;
    private static final int LEFT_PANEL_WIDTH = 180;
    private static final int LEFT_PANEL_HEIGHT = 200;
    
    // Panel derecho - Estadísticas
    private static final int RIGHT_PANEL_X = 200;
    private static final int RIGHT_PANEL_Y = 20;
    private static final int RIGHT_PANEL_WIDTH = 180;
    private static final int RIGHT_PANEL_HEIGHT = 200;
    
    // Lista de contenedores y scroll
    private List<ContainerInfo> containerList = new ArrayList<>();
    
    // Estadísticas cacheadas
    private int totalNetworkItems = 0;
    private int totalUniqueTypes = 0;
    private float averageFillPercentage = 0f;
    private String topItemName = "";
    private int topItemCount = 0;
    
    private int scrollOffset = 0;
    private int maxVisibleContainers = 5; // Reducido de 6 a 5 para evitar que el último elemento se corte
    private ContainerInfo selectedContainer = null;

    // Vista detallada
    private boolean showDetailedView = false;
    private ContainerInfo detailedContainer = null;
    // Area and scroll for detailed items
    private int detailedItemsAreaX = 0;
    private int detailedItemsAreaY = 0;
    private int detailedItemsAreaWidth = 0;
    private int detailedItemsAreaHeight = 0;
    private int detailedItemsScrollRowOffset = 0;
    
    // Búsqueda
    private EditBox searchBox;
    private String searchFilter = "";
    
    // Estado de scrollbars interactivos
    private boolean draggingLeftScrollbar = false;
    private boolean draggingDetailScrollbar = false;
    
    // Indicador de carga
    private boolean isLoading = false;
    
    public IndexerControllerNetworkScreen(IndexerControllerNetworkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 10;
        
        // Actualización inicial al abrir el controlador
        refreshNetwork();
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Si estamos en vista detallada y se presiona Escape, cerrar el modal
        if (showDetailedView && keyCode == 256) { // 256 es el código de la tecla Escape
            showDetailedView = false;
            detailedContainer = null;
            return true; // Consumir el evento para que no cierre la pantalla completa
        }
        
        // Si no estamos en vista detallada, permitir que Escape cierre la pantalla normalmente
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        super.init();
        
        int leftX = (this.width - this.imageWidth) / 2;
        int topY = (this.height - this.imageHeight) / 2;
        
        // Caja de búsqueda
        this.searchBox = new EditBox(this.font, leftX + LEFT_PANEL_X + 2, topY + LEFT_PANEL_Y - 15, 
                                   LEFT_PANEL_WIDTH - 4, 12, Component.translatable("gui.indexer.controller.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addWidget(this.searchBox);
        
    }
    
    private void onSearchChanged(String search) {
        this.searchFilter = search.toLowerCase();
        this.scrollOffset = 0;
    }
    
    private void refreshNetwork() {
        // Enviar paquete al servidor para solicitar actualización de la red
        ModNetworking.sendToServer(new RefreshNetworkPacket(this.menu.getBlockEntity().getBlockPos()));
        
        // Actualizar la lista de contenedores inmediatamente
        updateContainerList();
        
        // Opcional: Mostrar feedback visual al usuario
        // Se podría agregar un mensaje temporal o cambiar el color del botón brevemente
    }
    
    private void updateContainerList() {
        // Mostrar indicador de carga
        isLoading = true;
        
        // Limpiar la lista actual
        containerList.clear();
        
        // Enviar solicitud al servidor para obtener datos reales
        ModNetworking.sendToServer(new RefreshNetworkPacket(this.menu.getBlockEntity().getBlockPos()));
    }
    
    // Método para actualizar la lista desde el servidor
    public void updateContainerListFromServer(List<ContainerListUpdatePacket.ContainerData> serverContainers) {
        containerList.clear();
        
        for (ContainerListUpdatePacket.ContainerData serverContainer : serverContainers) {
            ContainerInfo info = new ContainerInfo();
            info.position = serverContainer.position;
            info.containerType = serverContainer.containerType;
            info.itemCount = serverContainer.itemCount;
            info.maxSlots = serverContainer.maxSlots;
            info.filters = new ArrayList<>(serverContainer.filters);
            info.uniqueItems = new HashMap<>(serverContainer.uniqueItems);
            containerList.add(info);
        }
        
        recalculateStats();
        
        // Ocultar indicador de carga
        isLoading = false;
    }
    
    private void recalculateStats() {
        totalNetworkItems = 0;
        Set<String> uniqueTypes = new HashSet<>();
        Map<String, Integer> itemCounts = new HashMap<>();
        float totalFill = 0;
        int filledContainers = 0;

        for (ContainerInfo c : containerList) {
            totalNetworkItems += c.itemCount;
            uniqueTypes.addAll(c.uniqueItems.keySet());
            
            c.uniqueItems.forEach((k, v) -> itemCounts.merge(k, v, Integer::sum));
            
            if (c.maxSlots > 0) {
                totalFill += (float) c.itemCount / c.maxSlots;
                filledContainers++;
            }
        }
        
        totalUniqueTypes = uniqueTypes.size();
        averageFillPercentage = filledContainers > 0 ? (totalFill / filledContainers) * 100 : 0;
        
        // Find top item
        topItemCount = 0;
        topItemName = "";
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            if (entry.getValue() > topItemCount) {
                topItemCount = entry.getValue();
                topItemName = entry.getKey();
            }
        }
    }
    
    private String getRandomContainerType() {
        String[] types = {"Chest", "Barrel", "Furnace", "Blast Furnace", "Smoker", "Hopper"};
        return types[(int)(Math.random() * types.length)];
    }
    
    private int getMaxSlotsForType(String type) {
        return switch (type) {
            case "Chest", "Barrel" -> 27;
            case "Furnace", "Blast Furnace", "Smoker" -> 3;
            case "Hopper" -> 5;
            default -> 27;
        };
    }
    
    private List<ItemStack> generateRandomFilters() {
        List<ItemStack> filters = new ArrayList<>();
        ItemStack[] possibleItems = {
            new ItemStack(Items.IRON_INGOT),
            new ItemStack(Items.GOLD_INGOT),
            new ItemStack(Items.DIAMOND),
            new ItemStack(Items.COAL),
            new ItemStack(Items.REDSTONE)
        };
        
        int filterCount = (int)(Math.random() * 3);
        for (int i = 0; i < filterCount; i++) {
            filters.add(possibleItems[(int)(Math.random() * possibleItems.length)]);
        }
        
        return filters;
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Renderizar fondo base en modo oscuro
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF2D2D30);
        
        // Bordes de la GUI en modo oscuro
        guiGraphics.fill(x, y, x + this.imageWidth, y + 2, 0xFF1E1E1E); // Top
        guiGraphics.fill(x, y + this.imageHeight - 2, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E1E); // Bottom
        guiGraphics.fill(x, y, x + 2, y + this.imageHeight, 0xFF1E1E1E); // Left
        guiGraphics.fill(x + this.imageWidth - 2, y, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E1E); // Right
        
        // Panel izquierdo - fondo oscuro
        guiGraphics.fill(x + LEFT_PANEL_X, y + LEFT_PANEL_Y, 
                        x + LEFT_PANEL_X + LEFT_PANEL_WIDTH, y + LEFT_PANEL_Y + LEFT_PANEL_HEIGHT, 
                        0xFF1E1E1E);
        
        // Panel izquierdo - borde
        guiGraphics.fill(x + LEFT_PANEL_X, y + LEFT_PANEL_Y, 
                        x + LEFT_PANEL_X + LEFT_PANEL_WIDTH, y + LEFT_PANEL_Y + 1, 0xFF404040);
        guiGraphics.fill(x + LEFT_PANEL_X, y + LEFT_PANEL_Y + LEFT_PANEL_HEIGHT - 1, 
                        x + LEFT_PANEL_X + LEFT_PANEL_WIDTH, y + LEFT_PANEL_Y + LEFT_PANEL_HEIGHT, 0xFF404040);
        guiGraphics.fill(x + LEFT_PANEL_X, y + LEFT_PANEL_Y, 
                        x + LEFT_PANEL_X + 1, y + LEFT_PANEL_Y + LEFT_PANEL_HEIGHT, 0xFF404040);
        guiGraphics.fill(x + LEFT_PANEL_X + LEFT_PANEL_WIDTH - 1, y + LEFT_PANEL_Y, 
                        x + LEFT_PANEL_X + LEFT_PANEL_WIDTH, y + LEFT_PANEL_Y + LEFT_PANEL_HEIGHT, 0xFF404040);
        
        // Panel derecho - fondo oscuro
        guiGraphics.fill(x + RIGHT_PANEL_X, y + RIGHT_PANEL_Y, 
                        x + RIGHT_PANEL_X + RIGHT_PANEL_WIDTH, y + RIGHT_PANEL_Y + RIGHT_PANEL_HEIGHT, 
                        0xFF1E1E1E);
        
        // Panel derecho - borde
        guiGraphics.fill(x + RIGHT_PANEL_X, y + RIGHT_PANEL_Y, 
                        x + RIGHT_PANEL_X + RIGHT_PANEL_WIDTH, y + RIGHT_PANEL_Y + 1, 0xFF404040);
        guiGraphics.fill(x + RIGHT_PANEL_X, y + RIGHT_PANEL_Y + RIGHT_PANEL_HEIGHT - 1, 
                        x + RIGHT_PANEL_X + RIGHT_PANEL_WIDTH, y + RIGHT_PANEL_Y + RIGHT_PANEL_HEIGHT, 0xFF404040);
        guiGraphics.fill(x + RIGHT_PANEL_X, y + RIGHT_PANEL_Y, 
                        x + RIGHT_PANEL_X + 1, y + RIGHT_PANEL_Y + RIGHT_PANEL_HEIGHT, 0xFF404040);
        guiGraphics.fill(x + RIGHT_PANEL_X + RIGHT_PANEL_WIDTH - 1, y + RIGHT_PANEL_Y, 
                        x + RIGHT_PANEL_X + RIGHT_PANEL_WIDTH, y + RIGHT_PANEL_Y + RIGHT_PANEL_HEIGHT, 0xFF404040);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Título centrado en color claro para modo oscuro - movido más abajo
        int titleWidth = this.font.width(this.title);
        int titleX = (this.imageWidth - titleWidth) / 2;
        guiGraphics.drawString(this.font, this.title, titleX, this.imageHeight - 15, 0xFFFFFF, false);
        
        // Título del panel izquierdo en color claro
        guiGraphics.drawString(this.font, Component.translatable("gui.indexer.controller.containers"), 
                              LEFT_PANEL_X + 5, LEFT_PANEL_Y - 10, 0xCCCCCC, false);
        
        // Título del panel derecho en color claro
        guiGraphics.drawString(this.font, Component.translatable("gui.indexer.controller.network_stats"), 
                              RIGHT_PANEL_X + 5, RIGHT_PANEL_Y - 10, 0xCCCCCC, false);
        
        // Renderizar lista de contenedores
        renderContainerList(guiGraphics, mouseX, mouseY);
        
        // Renderizar estadísticas solo si no hay modal abierto
        if (!showDetailedView) {
            renderNetworkStats(guiGraphics);
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Ocultar caja de búsqueda cuando se muestra la vista detallada
        this.searchBox.visible = !showDetailedView;
        
        // Renderizar todo el contenido base primero
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Renderizar tooltips de items normales si no estamos en vista detallada
        if (!showDetailedView) {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
        
        // Renderizar vista detallada AL FINAL para que aparezca por encima de TODO
        if (showDetailedView && detailedContainer != null) {
            renderDetailedView(guiGraphics, mouseX, mouseY);
        }
    }
    
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Solo renderizar tooltips normales si no estamos en vista detallada
        if (!showDetailedView) {
            super.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }
    
    private void renderContainerList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Mostrar indicador de carga si está cargando
        if (isLoading) {
            int centerX = LEFT_PANEL_X + LEFT_PANEL_WIDTH / 2;
            int centerY = LEFT_PANEL_Y + LEFT_PANEL_HEIGHT / 2;
            
            // Fondo semi-transparente
            guiGraphics.fill(LEFT_PANEL_X + 2, LEFT_PANEL_Y + 2, 
                           LEFT_PANEL_X + LEFT_PANEL_WIDTH - 2, LEFT_PANEL_Y + LEFT_PANEL_HEIGHT - 2, 
                           0x80000000);
            
            // Icono de carga (spinner simple usando caracteres)
            long time = System.currentTimeMillis() / 200;
            String[] spinnerChars = {"|", "/", "-", "\\"};
            String spinner = spinnerChars[(int)(time % 4)];
            
            // Texto de carga con icono
            String loadingText = Component.translatable("gui.indexer.controller.updating").getString() + " " + spinner;
            int textWidth = this.font.width(loadingText);
            guiGraphics.drawString(this.font, loadingText, centerX - textWidth / 2, centerY - 4, 0xFFFFFF, false);
            
            return;
        }
        
        List<ContainerInfo> filteredContainers = getFilteredContainers();
        
        int startY = LEFT_PANEL_Y + 5;
        int itemHeight = 35; // Aumentado para dar espacio a separaciones
        
        for (int i = 0; i < Math.min(maxVisibleContainers, filteredContainers.size() - scrollOffset); i++) {
            int index = i + scrollOffset;
            if (index >= filteredContainers.size()) break;
            
            ContainerInfo container = filteredContainers.get(index);
            int itemY = startY + (i * itemHeight);
            
            // Fondo del item (solo hover, sin selección) - colores para modo oscuro
            boolean isHovered = mouseX >= LEFT_PANEL_X && mouseX <= LEFT_PANEL_X + LEFT_PANEL_WIDTH - 8 &&
                               mouseY >= itemY && mouseY <= itemY + itemHeight - 5;
            
            if (isHovered && !showDetailedView) { // Solo mostrar hover si no estamos en vista detallada
                guiGraphics.fill(LEFT_PANEL_X + 2, itemY, LEFT_PANEL_X + LEFT_PANEL_WIDTH - 20, itemY + itemHeight - 5, 0xFF555555);
                // Renderizar preview flotante de contenido del contenedor solo si tiene ítems
                if (container.uniqueItems != null && !container.uniqueItems.isEmpty()) {
                    renderContainerPreviewOverlay(guiGraphics, container, itemY, mouseX, mouseY);
                }
            }
            
            // Información del contenedor - posición
            String posText = container.position.getX() + ", " + container.position.getY() + ", " + container.position.getZ();
            guiGraphics.drawString(this.font, posText, LEFT_PANEL_X + 5, itemY + 2, 0xCCCCCC, false);
            
            // Mostrar tipo de contenedor con traducción
            String translatedType = getTranslatedContainerType(container.containerType);
            String typeText = translatedType + " (" + container.itemCount + "/" + container.maxSlots + ")";
            guiGraphics.drawString(this.font, typeText, LEFT_PANEL_X + 5, itemY + 12, 0xAAAAAA, false);

            // Icono del tipo de contenedor a la derecha de la card
            // No mostrar cuando el modal de detalle está abierto
            if (!showDetailedView) {
                ItemStack typeIcon = getContainerTypeIcon(container.containerType);
                int iconSize = 16;
                int iconX = LEFT_PANEL_X + LEFT_PANEL_WIDTH - 20 - iconSize; // cerca del borde derecho
                int iconY = itemY + 6; // alineado verticalmente con el contenido
                
                // Render de fondo del slot para consistencia visual
                guiGraphics.fill(iconX - 1, iconY - 1, iconX + iconSize + 1, iconY + iconSize + 1, 0xFF1A1A1A);
                guiGraphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0xFF2D2D30);
                
                if (!typeIcon.isEmpty()) {
                    guiGraphics.renderItem(typeIcon, iconX, iconY);
                } else {
                    // Desconocido: mostrar un signo de interrogación centrado
                    String q = "?";
                    int tw = this.font.width(q);
                    int tx = iconX + (iconSize - tw) / 2;
                    int ty = iconY + 4;
                    guiGraphics.drawString(this.font, q, tx, ty, 0xFFFFFF, false);
                }
            }
            
            // Mostrar filtros del contenedor solo si no estamos en vista detallada
            if (!showDetailedView) {
                // Etiqueta "Filtros:" antes de mostrar los filtros
                String filtersLabel = Component.translatable("gui.indexer.controller.filters").getString();
                guiGraphics.drawString(this.font, filtersLabel, LEFT_PANEL_X + 5, itemY + 22, 0x999999, false);
                
                if (!container.filters.isEmpty()) {
                    int filterX = LEFT_PANEL_X + 5 + this.font.width(filtersLabel) + 3; // Después de la etiqueta
                    int filterY = itemY + 22; // Misma línea que la etiqueta
                    int maxFilters = 4; // Reducido para dar espacio a la etiqueta
                    int filterSpacing = 12; // Espaciado entre filtros
                    
                    for (int f = 0; f < Math.min(container.filters.size(), maxFilters); f++) {
                        ItemStack filter = container.filters.get(f);
                        if (!filter.isEmpty()) {
                            // Renderizar el icono del item del filtro en pequeño
                            guiGraphics.pose().pushPose();
                            guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
                            guiGraphics.renderItem(filter, (int)((filterX + (f * filterSpacing)) / 0.5f), (int)((filterY - 2) / 0.5f));
                            guiGraphics.pose().popPose();
                        }
                    }
                    
                    // Si hay más filtros, mostrar "..."
                    if (container.filters.size() > maxFilters) {
                        guiGraphics.drawString(this.font, "...", filterX + (maxFilters * filterSpacing), filterY, 0xAAAAAA, false);
                    }
                } else {
                    // Mostrar "Sin filtros" después de la etiqueta
                    String noFiltersText = Component.translatable("gui.indexer.controller.no_filters").getString();
                    int noFiltersX = LEFT_PANEL_X + 5 + this.font.width(filtersLabel) + 3;
                    guiGraphics.drawString(this.font, noFiltersText, noFiltersX, itemY + 22, 0x666666, false);
                }
            }
            
            // Línea separadora entre contenedores (excepto el último)
            if (i < Math.min(maxVisibleContainers, filteredContainers.size() - scrollOffset) - 1 && 
                index < filteredContainers.size() - 1) {
                int separatorY = itemY + itemHeight - 3;
                guiGraphics.fill(LEFT_PANEL_X + 5, separatorY, LEFT_PANEL_X + LEFT_PANEL_WIDTH - 25, separatorY + 1, 0xFF444444);
            }
        }
        
        // Scrollbar si es necesario
        if (filteredContainers.size() > maxVisibleContainers) {
            renderScrollbar(guiGraphics, filteredContainers.size());
        }
    }

    private void renderContainerPreviewOverlay(GuiGraphics guiGraphics, ContainerInfo container, int baseItemY, int mouseX, int mouseY) {
        // Dimensiones del overlay
        int overlayWidth = 140;
        int itemsPerRow = 4;
        int itemSize = 16;
        int itemSpacing = 22;
        int headerHeight = 16;
        int padding = 8;
        int maxItems = itemsPerRow * 2; // mostrar hasta 2 filas

        // Calcular alto en función de si hay items
        int shownItems = Math.min(container.uniqueItems.size(), maxItems);
        int rows = Math.max(1, (int) Math.ceil(shownItems / (float) itemsPerRow));
        int overlayHeight = padding * 2 + headerHeight + rows * itemSpacing;

        // Posición del overlay a la derecha del panel izquierdo
        // Posición del overlay: dentro del panel izquierdo, pegado al borde derecho
        int overlayX = LEFT_PANEL_X + LEFT_PANEL_WIDTH - overlayWidth - 12;
        int overlayY = baseItemY - 4;

        // Asegurar que no se salga por abajo
        int maxY = LEFT_PANEL_Y + LEFT_PANEL_HEIGHT - overlayHeight - 4;
        if (overlayY > maxY) overlayY = maxY;

        // Elevar z-index del overlay para asegurar que quede sobre textos
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500);
        
        // Fondo y borde
        guiGraphics.fill(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, 0xCC2D2D30);
        guiGraphics.fill(overlayX, overlayY, overlayX + overlayWidth, overlayY + 1, 0xFF404040);
        guiGraphics.fill(overlayX, overlayY + overlayHeight - 1, overlayX + overlayWidth, overlayY + overlayHeight, 0xFF404040);
        guiGraphics.fill(overlayX, overlayY, overlayX + 1, overlayY + overlayHeight, 0xFF404040);
        guiGraphics.fill(overlayX + overlayWidth - 1, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, 0xFF404040);

        // Título
        Component itemsTitle = Component.translatable("gui.indexer.controller.detailed_view.items");
        int titleWidth = this.font.width(itemsTitle);
        guiGraphics.drawString(this.font, itemsTitle, overlayX + (overlayWidth - titleWidth) / 2, overlayY + 4, 0xFFFFFF, false);

        // Si está vacío
        if (container.uniqueItems.isEmpty()) {
            String emptyText = Component.translatable("gui.indexer.controller.container_empty").getString();
            int emptyWidth = this.font.width(emptyText);
            guiGraphics.drawString(this.font, emptyText, overlayX + (overlayWidth - emptyWidth) / 2, overlayY + headerHeight + padding, 0xAAAAAA, false);
            guiGraphics.pose().popPose();
            return;
        }

        // Ordenar por cantidad y limitar
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(container.uniqueItems.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        boolean hasMoreItems = entries.size() > maxItems;
        if (hasMoreItems) entries = entries.subList(0, maxItems);

        int gridX = overlayX + padding;
        int gridY = overlayY + headerHeight + padding;

        int index = 0;
        for (Map.Entry<String, Integer> entry : entries) {
            int row = index / itemsPerRow;
            int col = index % itemsPerRow;
            int itemX = gridX + col * itemSpacing;
            int itemY = gridY + row * itemSpacing;

            // Render slot de fondo
            guiGraphics.fill(itemX - 1, itemY - 1, itemX + itemSize + 1, itemY + itemSize + 1, 0xFF1A1A1A);
            guiGraphics.fill(itemX, itemY, itemX + itemSize, itemY + itemSize, 0xFF2D2D30);

            ItemStack displayItem = createItemStackFromName(entry.getKey());
            if (!displayItem.isEmpty()) {
                guiGraphics.renderItem(displayItem, itemX, itemY);

                // Cantidad formateada en pequeño en la esquina
                String qty = formatNumber(entry.getValue());
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 200);
                guiGraphics.pose().scale(0.7f, 0.7f, 1.0f);

                float scaledX = (itemX + itemSize - 2) / 0.7f;
                float scaledY = (itemY + itemSize - 6) / 0.7f;
                guiGraphics.drawString(this.font, qty, (int) scaledX - this.font.width(qty), (int) scaledY, 0xFFFFFF, true);
                guiGraphics.pose().popPose();

                // Tooltip al pasar sobre el item dentro del overlay
                if (mouseX >= itemX && mouseX < itemX + itemSize && mouseY >= itemY && mouseY < itemY + itemSize) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(displayItem.getHoverName());
                    tooltip.add(Component.literal("Cantidad: " + formatNumber(entry.getValue())));
                    guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                }
            }
            index++;
        }
        
        // Indicador de más elementos si se truncó la lista
        if (hasMoreItems) {
            String moreText = "...";
            int moreWidth = this.font.width(moreText);
            guiGraphics.drawString(this.font, moreText,
                overlayX + overlayWidth - padding - moreWidth,
                overlayY + overlayHeight - padding - 9,
                0xAAAAAA,
                false);
        }

        // Cerrar pose elevada
        guiGraphics.pose().popPose();
    }
    
    private void renderScrollbar(GuiGraphics guiGraphics, int totalItems) {
        int scrollbarX = LEFT_PANEL_X + LEFT_PANEL_WIDTH - 6;
        int scrollbarY = LEFT_PANEL_Y + 5;
        int scrollbarHeight = LEFT_PANEL_HEIGHT - 10;
        
        // Fondo del scrollbar
        guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 6, scrollbarY + scrollbarHeight, 0xFFCCCCCC);
        
        // Thumb del scrollbar
        int thumbHeight = Math.max(10, (maxVisibleContainers * scrollbarHeight) / totalItems);
        int thumbY = scrollbarY + (scrollOffset * (scrollbarHeight - thumbHeight)) / (totalItems - maxVisibleContainers);
        
        guiGraphics.fill(scrollbarX + 1, thumbY, scrollbarX + 5, thumbY + thumbHeight, 0xFF888888);
    }
    
    private void renderNetworkStats(GuiGraphics guiGraphics) {
        int startX = RIGHT_PANEL_X + 10;
        int startY = RIGHT_PANEL_Y + 10;
        int lineHeight = 12;
        int currentY = startY;
        
        // --- RESUMEN DE RED ---
        guiGraphics.drawString(this.font, Component.translatable("gui.indexer.controller.network_summary"), 
                              startX, currentY, 0xCCCCCC, false);
        currentY += lineHeight + 2;
        
        // Contenedores
        String containersText = Component.translatable("gui.indexer.controller.containers").getString() + ": " + 
                               this.menu.getConnectedContainersCount();
        guiGraphics.drawString(this.font, containersText, startX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;
        
        // Items Totales
        String itemsText = Component.translatable("gui.indexer.controller.total_items").getString() + ": " + 
                           formatNumber(totalNetworkItems);
        guiGraphics.drawString(this.font, itemsText, startX, currentY, 0xFFFFFF, false);
        currentY += lineHeight;

        // Tipos Únicos
        String typesText = Component.translatable("gui.indexer.controller.unique_types").getString() + ": " + 
                           formatNumber(totalUniqueTypes);
        guiGraphics.drawString(this.font, typesText, startX, currentY, 0xFFFFFF, false);
        currentY += lineHeight + 8;
        
        // --- MAYOR STOCK ---
        if (!topItemName.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.indexer.controller.top_stock"), 
                                  startX, currentY, 0xCCCCCC, false);
            currentY += lineHeight + 2;
            
            ItemStack topStack = createItemStackFromName(topItemName);
            if (!topStack.isEmpty()) {
                // Render Item
                guiGraphics.renderItem(topStack, startX, currentY);
                
                // Name and count
                String name = topStack.getHoverName().getString();
                if (name.length() > 18) name = name.substring(0, 15) + "...";
                
                guiGraphics.drawString(this.font, name, startX + 20, currentY, 0xFFFFFF, false);
                guiGraphics.drawString(this.font, formatNumber(topItemCount), startX + 20, currentY + 9, 0xAAAAAA, false);
                
                currentY += 20;
            } else {
                 currentY += lineHeight;
            }
            currentY += 8;
        }

        // --- EFICIENCIA ---
        guiGraphics.drawString(this.font, Component.translatable("gui.indexer.controller.efficiency"), 
                              startX, currentY, 0xCCCCCC, false);
        currentY += lineHeight + 2;
        
        // Avg Fill
        String fillText = Component.translatable("gui.indexer.controller.avg_fill").getString() + ": ";
        int fillTextWidth = this.font.width(fillText);
        guiGraphics.drawString(this.font, fillText, startX, currentY, 0xFFFFFF, false);
        
        String pctText = String.format("%.1f%%", averageFillPercentage);
        int color = averageFillPercentage > 80 ? 0xFF44FF44 : averageFillPercentage > 50 ? 0xFFFFAA00 : 0xFFFF4444;
        guiGraphics.drawString(this.font, pctText, startX + fillTextWidth, currentY, color, false);
        currentY += lineHeight + 8;
        
        // --- VELOCIDAD (Existing) ---
        int upgradeLevel = this.menu.getCurrentUpgradeLevel();
        String transferRateText = Component.translatable("gui.indexer.controller.speed").getString() + ": " + 
                                 this.menu.getItemsPerTransfer() + "/t";
        
        guiGraphics.drawString(this.font, transferRateText, startX, currentY, 0x00FF00, false);
        
        if (upgradeLevel > 0) {
            ItemStack upgradeItem = getUpgradeItemForLevel(upgradeLevel);
            if (!upgradeItem.isEmpty()) {
                int textWidth = this.font.width(transferRateText);
                guiGraphics.renderItem(upgradeItem, startX + 5 + textWidth, currentY - 4);
            }
        }
    }
    
    private void renderDetailedView(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Fondo semi-transparente para toda la pantalla
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        
        // Panel detallado centrado - ajustado más hacia la izquierda
        int panelWidth = 320; // Aumentado ligeramente para más espacio
        int panelHeight = 220; // Aumentado para acomodar mejor los filtros
        int panelX = (this.width - panelWidth) / 2 - 30; // Movido 30 píxeles a la izquierda
        int panelY = (this.height - panelHeight) / 2;
        
        // Fondo del panel
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF2D2D30);
        
        // Borde del panel
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF404040);
        guiGraphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFF404040);
        guiGraphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFF404040);
        guiGraphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF404040);
        
        // Título del panel
        Component title = Component.translatable("gui.indexer.controller.detailed_view.title");
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, panelX + (panelWidth - titleWidth) / 2, panelY + 8, 0xFFFFFF, false);
        
        // Información básica
        int yOffset = panelY + 25;
        
        // Posición
        Component posText = Component.translatable("gui.indexer.controller.detailed_view.position")
                .append(" " + detailedContainer.position.getX() + ", " + 
                        detailedContainer.position.getY() + ", " + detailedContainer.position.getZ());
        guiGraphics.drawString(this.font, posText, panelX + 10, yOffset, 0xCCCCCC, false);
        yOffset += 12;
        
        // Tipo de contenedor
        String translatedType = getTranslatedContainerType(detailedContainer.containerType);
        Component typeText = Component.translatable("gui.indexer.controller.detailed_view.type").append(" " + translatedType);
        guiGraphics.drawString(this.font, typeText, panelX + 10, yOffset, 0xCCCCCC, false);
        yOffset += 12;
        
        // Capacidad y llenado
        Component capacityText = Component.translatable("gui.indexer.controller.detailed_view.capacity")
                .append(" " + detailedContainer.itemCount + "/" + detailedContainer.maxSlots + " slots");
        guiGraphics.drawString(this.font, capacityText, panelX + 10, yOffset, 0xCCCCCC, false);
        yOffset += 12;
        
        // Porcentaje de llenado
        float fillPercentage = detailedContainer.maxSlots > 0 ? 
            (float) detailedContainer.itemCount / detailedContainer.maxSlots * 100 : 0;
        Component fillText = Component.translatable("gui.indexer.controller.detailed_view.filled")
                .append(String.format(" %.1f%%", fillPercentage));
        int fillColor = fillPercentage > 80 ? 0xFFFF4444 : fillPercentage > 50 ? 0xFFFFAA00 : 0xFF44FF44;
        guiGraphics.drawString(this.font, fillText, panelX + 10, yOffset, fillColor, false);
        yOffset += 12;
        
        // Barra de llenado visual
        int barWidth = panelWidth - 20;
        int barHeight = 6;
        int barX = panelX + 10;
        int barY = yOffset;
        
        // Fondo de la barra
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF444444);
        
        // Barra de progreso
        int fillWidth = (int) (barWidth * fillPercentage / 100);
        if (fillWidth > 0) {
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, fillColor);
        }
        yOffset += 20;
        
        // Filtros
        Component filtersText = Component.translatable("gui.indexer.controller.detailed_view.filters");
        guiGraphics.drawString(this.font, filtersText, panelX + 10, yOffset, 0xFFFFFF, false);
        yOffset += 15;
        
        if (detailedContainer.filters.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.indexer.controller.no_filters_configured").getString(), panelX + 10, yOffset, 0x888888, false);
        } else {
            // Mostrar todos los filtros en una cuadrícula con mejor espaciado
            int filterX = panelX + 10;
            int filterY = yOffset;
            int filterSize = 16;
            int filterSpacing = 20; // Aumentado el espaciado entre filtros
            int filtersPerRow = (panelWidth - 20) / filterSpacing;
            
            for (int i = 0; i < detailedContainer.filters.size(); i++) {
                ItemStack filter = detailedContainer.filters.get(i);
                if (!filter.isEmpty()) {
                    int row = i / filtersPerRow;
                    int col = i % filtersPerRow;
                    int itemX = filterX + col * filterSpacing;
                    int itemY = filterY + row * filterSpacing;
                    
                    // Fondo para el slot del filtro para evitar solapamiento
                    guiGraphics.fill(itemX - 1, itemY - 1, itemX + filterSize + 1, itemY + filterSize + 1, 0xFF1A1A1A);
                    guiGraphics.fill(itemX, itemY, itemX + filterSize, itemY + filterSize, 0xFF2D2D30);
                    
                    // Renderizar el item
                    guiGraphics.renderItem(filter, itemX, itemY);
                    
                    // Tooltip si el mouse está sobre el item
                    if (mouseX >= itemX && mouseX < itemX + filterSize &&
                        mouseY >= itemY && mouseY < itemY + filterSize) {
                        guiGraphics.renderTooltip(this.font, filter, mouseX, mouseY);
                    }
                }
            }
        }
        yOffset += 40; // Espacio después de los filtros
        
        // Items únicos en el contenedor
        Component itemsText = Component.translatable("gui.indexer.controller.detailed_view.items");
        guiGraphics.drawString(this.font, itemsText, panelX + 10, yOffset, 0xFFFFFF, false);
        yOffset += 15;
        
        if (detailedContainer.uniqueItems.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.indexer.controller.container_empty").getString(), panelX + 10, yOffset, 0x888888, false);
        } else {
            // Area with scroll for Items
            int itemSize = 16;
            int itemSpacing = 20;

            // Definir viewport para items dentro del panel
            detailedItemsAreaX = panelX + 10;
            detailedItemsAreaY = yOffset;
            int scrollbarWidth = 6;
            int rightMargin = 8;
            detailedItemsAreaWidth = panelWidth - 20 - (scrollbarWidth + rightMargin);
            // Reservar ~40px al fondo para el texto de cierre y margen, pero asegurar mínimo 2 filas visibles
            detailedItemsAreaHeight = panelY + panelHeight - detailedItemsAreaY - 40;
            detailedItemsAreaHeight = Math.max(itemSpacing * 2 + 8, detailedItemsAreaHeight);

            // Fondo del área
            guiGraphics.fill(detailedItemsAreaX, detailedItemsAreaY,
                             detailedItemsAreaX + detailedItemsAreaWidth,
                             detailedItemsAreaY + detailedItemsAreaHeight,
                             0xFF232323);

            int itemsPerRow = Math.max(1, detailedItemsAreaWidth / itemSpacing);
            int visibleRows = Math.max(2, detailedItemsAreaHeight / itemSpacing);
            int totalItems = detailedContainer.uniqueItems.size();
            int totalRows = (int) Math.ceil(totalItems / (double) itemsPerRow);

            // Clamp del offset de filas
            int maxRowOffset = Math.max(0, totalRows - visibleRows);
            if (detailedItemsScrollRowOffset > maxRowOffset) detailedItemsScrollRowOffset = maxRowOffset;
            if (detailedItemsScrollRowOffset < 0) detailedItemsScrollRowOffset = 0;

            int itemIndex = 0;
            for (Map.Entry<String, Integer> entry : detailedContainer.uniqueItems.entrySet()) {
                String itemName = entry.getKey();
                int quantity = entry.getValue();
                
                int row = itemIndex / itemsPerRow;
                int col = itemIndex % itemsPerRow;

                // Renderizar solo las filas visibles dentro del viewport
                if (row < detailedItemsScrollRowOffset || row >= detailedItemsScrollRowOffset + visibleRows) {
                    itemIndex++;
                    continue;
                }

                int visibleRowIndex = row - detailedItemsScrollRowOffset;
                int currentItemX = detailedItemsAreaX + col * itemSpacing;
                int currentItemY = detailedItemsAreaY + visibleRowIndex * itemSpacing;
                
                // Fondo para el slot del item
                guiGraphics.fill(currentItemX - 1, currentItemY - 1, currentItemX + itemSize + 1, currentItemY + itemSize + 1, 0xFF1A1A1A);
                guiGraphics.fill(currentItemX, currentItemY, currentItemX + itemSize, currentItemY + itemSize, 0xFF2D2D30);
                
                // Crear ItemStack desde el nombre del item para renderizar
                ItemStack displayItem = createItemStackFromName(itemName);
                if (!displayItem.isEmpty()) {
                    // Renderizar el item
                    guiGraphics.renderItem(displayItem, currentItemX, currentItemY);
                    
                    // Renderizar la cantidad en la esquina inferior derecha con texto más pequeño
                    // Usar un z-level más alto para que aparezca por encima del item
                    String quantityText = formatNumber(quantity);
                    
                    // Mover el pose para renderizar por encima
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 200); // Z-level más alto
                    guiGraphics.pose().scale(0.7f, 0.7f, 1.0f);
                    
                    // Calcular posición ajustada para la escala
                    float scaledX = (currentItemX + itemSize - 2) / 0.7f;
                    float scaledY = (currentItemY + itemSize - 2) / 0.7f;
                    int textWidth = (int)(this.font.width(quantityText) * 0.7f);
                    
                    // Dibujar el texto escalado con sombra para mejor visibilidad
                    guiGraphics.drawString(this.font, quantityText, 
                                         (int)(scaledX - textWidth / 0.7f), 
                                         (int)(scaledY - 8 / 0.7f), 
                                         0xFFFFFF, true);
                    
                    guiGraphics.pose().popPose();
                    
                    // Tooltip si el mouse está sobre el item
                    if (mouseX >= currentItemX && mouseX < currentItemX + itemSize &&
                        mouseY >= currentItemY && mouseY < currentItemY + itemSize &&
                        mouseX >= detailedItemsAreaX && mouseX < detailedItemsAreaX + detailedItemsAreaWidth &&
                        mouseY >= detailedItemsAreaY && mouseY < detailedItemsAreaY + detailedItemsAreaHeight) {
                        List<Component> tooltip = new ArrayList<>();
                        tooltip.add(displayItem.getHoverName());
                        tooltip.add(Component.literal("Cantidad: " + formatNumber(quantity)));
                        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    }
                }
                
                itemIndex++;
            }

            // Scrollbar vertical si hay más filas que visibles
            if (totalRows > visibleRows) {
                int scrollbarX = detailedItemsAreaX + detailedItemsAreaWidth + rightMargin;
                int scrollbarY = detailedItemsAreaY;
                int scrollbarHeight = detailedItemsAreaHeight;

                // Fondo del scrollbar
                guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, 0xFF3A3A3A);

                // Thumb
                int thumbHeight = Math.max(10, (visibleRows * scrollbarHeight) / totalRows);
                int thumbY = scrollbarY + (detailedItemsScrollRowOffset * (scrollbarHeight - thumbHeight)) / (totalRows - visibleRows);
                guiGraphics.fill(scrollbarX + 1, thumbY, scrollbarX + scrollbarWidth - 1, thumbY + thumbHeight, 0xFF888888);
            }
        }
        
        // Instrucciones para cerrar
        Component closeText = Component.translatable("gui.indexer.controller.detailed_view.close_instruction");
        int closeWidth = this.font.width(closeText);
        guiGraphics.drawString(this.font, closeText, panelX + (panelWidth - closeWidth) / 2, 
                              panelY + panelHeight - 15, 0x888888, false);
    }
    
    private List<ContainerInfo> getFilteredContainers() {
        if (searchFilter.isEmpty()) {
            return containerList;
        }
        
        return containerList.stream()
                .filter(container -> 
                    container.containerType.toLowerCase().contains(searchFilter) ||
                    container.position.toString().toLowerCase().contains(searchFilter))
                .toList();
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Si estamos en vista detallada, manejar el click para cerrarla
        if (showDetailedView) {
            // Click fuera del panel detallado para cerrarlo
            if (mouseX < LEFT_PANEL_X || mouseX > LEFT_PANEL_X + LEFT_PANEL_WIDTH ||
                mouseY < LEFT_PANEL_Y || mouseY > LEFT_PANEL_Y + LEFT_PANEL_HEIGHT) {
                showDetailedView = false;
                detailedContainer = null;
                return true;
            }
            return true; // Consumir el click dentro del panel detallado
        }
        
        // Click en la barra de scroll del panel izquierdo
        List<ContainerInfo> filtered = getFilteredContainers();
        if (filtered.size() > maxVisibleContainers) {
            int scrollbarX = LEFT_PANEL_X + LEFT_PANEL_WIDTH - 6;
            int scrollbarY = LEFT_PANEL_Y + 5;
            int scrollbarHeight = LEFT_PANEL_HEIGHT - 10;
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + 6 &&
                mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
                int thumbHeight = Math.max(10, (maxVisibleContainers * scrollbarHeight) / filtered.size());
                int track = Math.max(1, scrollbarHeight - thumbHeight);
                int pos = (int) Math.max(0, Math.min(track, mouseY - scrollbarY - thumbHeight / 2));
                int maxScroll = Math.max(0, filtered.size() - maxVisibleContainers);
                scrollOffset = (pos * maxScroll) / track;
                draggingLeftScrollbar = true;
                return true;
            }
        }
        
        // Manejar clics en la lista de contenedores para abrir vista detallada
        if (mouseX >= LEFT_PANEL_X && mouseX <= LEFT_PANEL_X + LEFT_PANEL_WIDTH - 8 &&
            mouseY >= LEFT_PANEL_Y + 5 && mouseY <= LEFT_PANEL_Y + LEFT_PANEL_HEIGHT - 5) {
            
            int itemHeight = 35; // Coincidir con renderContainerList
            int clickedIndex = ((int)mouseY - LEFT_PANEL_Y - 5) / itemHeight + scrollOffset;
            List<ContainerInfo> filteredContainers = getFilteredContainers();
            
            if (clickedIndex >= 0 && clickedIndex < filteredContainers.size()) {
                // Abrir vista detallada en lugar de seleccionar
                detailedContainer = filteredContainers.get(clickedIndex);
                showDetailedView = true;
                return true;
            }
        }
        
        // Click en scrollbar del detalle (cuando modal está abierto)
        if (showDetailedView && detailedContainer != null) {
            int itemSpacing = 20;
            int itemsPerRow = Math.max(1, detailedItemsAreaWidth / itemSpacing);
            int totalItems = detailedContainer.uniqueItems.size();
            int totalRows = (int) Math.ceil(totalItems / (double) itemsPerRow);
            int visibleRows = Math.max(2, detailedItemsAreaHeight / itemSpacing);
            if (totalRows > visibleRows) {
                int scrollbarWidth = 6;
                int rightMargin = 8;
                int scrollbarX = detailedItemsAreaX + detailedItemsAreaWidth + rightMargin;
                int scrollbarY = detailedItemsAreaY;
                int scrollbarHeight = detailedItemsAreaHeight;
                if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                    mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
                    int thumbHeight = Math.max(10, (visibleRows * scrollbarHeight) / totalRows);
                    int track = Math.max(1, scrollbarHeight - thumbHeight);
                    int pos = (int) Math.max(0, Math.min(track, mouseY - scrollbarY - thumbHeight / 2));
                    int maxRowOffset = Math.max(0, totalRows - visibleRows);
                    detailedItemsScrollRowOffset = Math.max(0, Math.min(maxRowOffset, (pos * maxRowOffset) / track));
                    draggingDetailScrollbar = true;
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Scroll en la lista de contenedores
        if (mouseX >= LEFT_PANEL_X && mouseX <= LEFT_PANEL_X + LEFT_PANEL_WIDTH &&
            mouseY >= LEFT_PANEL_Y && mouseY <= LEFT_PANEL_Y + LEFT_PANEL_HEIGHT) {
            
            List<ContainerInfo> filteredContainers = getFilteredContainers();
            int maxScroll = Math.max(0, filteredContainers.size() - maxVisibleContainers);
            
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)scrollY));
            return true;
        }

        // Scroll en el área de items del detalle
        if (showDetailedView && detailedContainer != null &&
            mouseX >= detailedItemsAreaX && mouseX <= detailedItemsAreaX + detailedItemsAreaWidth &&
            mouseY >= detailedItemsAreaY && mouseY <= detailedItemsAreaY + detailedItemsAreaHeight) {

            int itemSpacing = 20;
            int itemsPerRow = Math.max(1, detailedItemsAreaWidth / itemSpacing);
            int visibleRows = Math.max(2, detailedItemsAreaHeight / itemSpacing);
            int totalItems = detailedContainer.uniqueItems.size();
            int totalRows = (int) Math.ceil(totalItems / (double) itemsPerRow);
            int maxRowOffset = Math.max(0, totalRows - visibleRows);

            detailedItemsScrollRowOffset = Math.max(0, Math.min(maxRowOffset, detailedItemsScrollRowOffset - (int)scrollY));
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingLeftScrollbar) {
            List<ContainerInfo> filtered = getFilteredContainers();
            int scrollbarY = LEFT_PANEL_Y + 5;
            int scrollbarHeight = LEFT_PANEL_HEIGHT - 10;
            int thumbHeight = Math.max(10, (maxVisibleContainers * scrollbarHeight) / Math.max(1, filtered.size()));
            int track = Math.max(1, scrollbarHeight - thumbHeight);
            int pos = (int) Math.max(0, Math.min(track, mouseY - scrollbarY - thumbHeight / 2));
            int maxScroll = Math.max(0, filtered.size() - maxVisibleContainers);
            scrollOffset = (pos * maxScroll) / track;
            return true;
        }
        if (draggingDetailScrollbar && showDetailedView && detailedContainer != null) {
            int itemSpacing = 20;
            int itemsPerRow = Math.max(1, detailedItemsAreaWidth / itemSpacing);
            int totalItems = detailedContainer.uniqueItems.size();
            int totalRows = (int) Math.ceil(totalItems / (double) itemsPerRow);
            int visibleRows = Math.max(2, detailedItemsAreaHeight / itemSpacing);
            int maxRowOffset = Math.max(0, totalRows - visibleRows);
            int scrollbarHeight = detailedItemsAreaHeight;
            int thumbHeight = Math.max(10, (visibleRows * scrollbarHeight) / Math.max(1, totalRows));
            int track = Math.max(1, scrollbarHeight - thumbHeight);
            int pos = (int) Math.max(0, Math.min(track, mouseY - detailedItemsAreaY - thumbHeight / 2));
            detailedItemsScrollRowOffset = Math.max(0, Math.min(maxRowOffset, (pos * maxRowOffset) / track));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingLeftScrollbar = false;
        draggingDetailScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        // El auto-refresh ha sido eliminado - ahora solo se actualiza cuando es necesario
    }
    
    private String formatNumber(int number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1000000) {
            return String.format("%.1fK", number / 1000.0).replace(".0K", "K");
        } else {
            return String.format("%.1fM", number / 1000000.0).replace(".0M", "M");
        }
    }
    
    private String getTranslatedContainerType(String containerType) {
        switch (containerType.toLowerCase()) {
            case "chest":
                return Component.translatable("gui.indexer.controller.container_type.chest").getString();
            case "furnace":
                return Component.translatable("gui.indexer.controller.container_type.furnace").getString();
            case "blast_furnace":
                return Component.translatable("gui.indexer.controller.container_type.blast_furnace").getString();
            case "smoker":
                return Component.translatable("gui.indexer.controller.container_type.smoker").getString();
            case "barrel":
                return Component.translatable("gui.indexer.controller.container_type.barrel").getString();
            case "shulker_box":
                return Component.translatable("gui.indexer.controller.container_type.shulker_box").getString();
            case "hopper":
                return Component.translatable("gui.indexer.controller.container_type.hopper").getString();
            case "dispenser":
                return Component.translatable("gui.indexer.controller.container_type.dispenser").getString();
            case "dropper":
                return Component.translatable("gui.indexer.controller.container_type.dropper").getString();
            case "brewing_stand":
                return Component.translatable("gui.indexer.controller.container_type.brewing_stand").getString();
            default:
                return Component.translatable("gui.indexer.controller.container_type.other").getString();
        }
    }

    private ItemStack getContainerTypeIcon(String containerType) {
        switch (containerType.toLowerCase()) {
            case "chest":
                return new ItemStack(Items.CHEST);
            case "barrel":
                return new ItemStack(Items.BARREL);
            case "hopper":
                return new ItemStack(Items.HOPPER);
            case "furnace":
                return new ItemStack(Items.FURNACE);
            case "blast_furnace":
                return new ItemStack(Items.BLAST_FURNACE);
            case "smoker":
                return new ItemStack(Items.SMOKER);
            case "dispenser":
                return new ItemStack(Items.DISPENSER);
            case "dropper":
                return new ItemStack(Items.DROPPER);
            case "brewing_stand":
                return new ItemStack(Items.BREWING_STAND);
            case "shulker_box":
                return new ItemStack(Items.SHULKER_BOX);
            default:
                return ItemStack.EMPTY; // desconocido, mostramos "?"
        }
    }
    
    private ItemStack createItemStackFromName(String itemName) {
        try {
            // Intentar crear el ItemStack desde el ResourceLocation
            ResourceLocation itemLocation = ResourceLocation.parse(itemName);
            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(itemLocation);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            // Si falla, devolver ItemStack vacío
        }
        return ItemStack.EMPTY;
    }
    
    private ItemStack getUpgradeItemForLevel(int level) {
        switch (level) {
            case 0: return new ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_ZERO);
            case 1: return new ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_BASIC);
            case 2: return new ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_COPPER);
            case 3: return new ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_ADVANCED);
            case 4: return new ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_ELITE);
            case 5: return new ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_DEFINITIVE);
            default: return ItemStack.EMPTY;
        }
    }
    
    private String getUpgradeNameForLevel(int level) {
        switch (level) {
            case 0: return Component.translatable("upgrade.indexer.zero").getString();
            case 1: return Component.translatable("upgrade.indexer.basic").getString();
            case 2: return Component.translatable("upgrade.indexer.copper").getString();
            case 3: return Component.translatable("upgrade.indexer.advanced").getString();
            case 4: return Component.translatable("upgrade.indexer.elite").getString();
            case 5: return Component.translatable("upgrade.indexer.definitive").getString();
            default: return "Sin mejora";
        }
    }
    
    // Clase interna para almacenar información de contenedores
    private static class ContainerInfo {
        BlockPos position;
        String containerType;
        int itemCount;
        int maxSlots;
        List<ItemStack> filters;
        Map<String, Integer> uniqueItems;
        
        public ContainerInfo() {
            this.filters = new ArrayList<>();
            this.uniqueItems = new HashMap<>();
        }
    }
}
