package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.IndexerMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import com.mojang.math.Axis;

/**
 * Pantalla GUI para el manual del Indexer.
 * Muestra instrucciones paso a paso con imágenes y permite navegar entre páginas.
 */
public class IndexerManualScreen extends Screen {
    // Constantes para la pantalla
    private static final int SCREEN_WIDTH = 271;
    private static final int SCREEN_HEIGHT = 180;
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "textures/gui/manual/manualgui.png");
    private static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "textures/block/indexer_controller_top.png");
    private static final String WIKI_URL = "https://agustinbeniteez.github.io/Wikimods/mod/index.html?id=indexer&game=minecraft";
    
    // Botones de navegación
    private Button nextButton;
    private Button prevButton;
    private Button closeButton;
    private Button menuButton;
    private Button craftingButton;
    private Button tutorialButton;
    private Button wikiButton;
    private EditBox searchBox;
    
    // Estado de la pantalla
    private int currentPage = 0;
    private final int totalPages = 13; // Número total de páginas disponibles
    
    // Recursos para las imágenes de cada página
    private final ResourceLocation[] pageImages = new ResourceLocation[totalPages];
    
    // Estados de vista
    private enum ViewMode { MENU, TUTORIAL, CRAFTING }
    private ViewMode currentView = ViewMode.MENU;
    
    // Splash del logo
    private boolean inSplash = true;
    private long splashEndMillis = 0L;

    // Texturas de botones del menú
    private static final ResourceLocation MENU_CRAFT_TEXTURE = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "textures/gui/manual/menu/menucraft.png");
    private static final ResourceLocation MENU_TUTORIAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "textures/gui/manual/menu/menututorial.png");
    private static final ResourceLocation MENU_WIKI_TEXTURE = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "textures/gui/manual/menu/menuwiki.png");

    // Posición/tamaño de botones del menú (para reutilizar)
    private int menuBtnWidth;
    private int menuBtnHeight;
    private int menuStartX;
    private int menuY;

    // Botón con textura para el menú
    private static class TexturedMenuButton extends Button {
        private final ResourceLocation texture;
        public TexturedMenuButton(int x, int y, int width, int height, Component message, OnPress onPress, ResourceLocation texture) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.texture = texture;
        }
        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();
            if (this.isHoveredOrFocused()) {
                int dw = (int)Math.round(w * 1.06);
                int dh = (int)Math.round(h * 1.06);
                int dx = x - (dw - w) / 2;
                int dy = y - (dh - h) / 2;
                guiGraphics.blit(texture, dx, dy, 0, 0, dw, dh, w, h);
                guiGraphics.fill(x, y, x + w, y + h, 0x66000000);
                guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), x + w / 2, y + (h - 8) / 2, 0xFFFFFF);
            } else {
                guiGraphics.blit(texture, x, y, 0, 0, w, h, w, h);
            }
        }
    }
    
    // Crafteos
    private java.util.List<Recipe<?>> modCraftingRecipes = java.util.Collections.emptyList();
    private java.util.List<Recipe<?>> filteredCraftingRecipes = java.util.Collections.emptyList();
    private int craftingPage = 0;
    private static final int recipesPerPage = 2;
    
    public IndexerManualScreen() {
        super(Component.translatable("item.indexer.indexer_manual"));
        
        // Initialize page images
        for (int i = 0; i < totalPages; i++) {
            pageImages[i] = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "textures/gui/manual/manual" + (i + 1) + ".png");
        }
        this.splashEndMillis = System.currentTimeMillis() + 1000L;
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
        
        // Botón para volver al menú
        this.menuButton = Button.builder(
            Component.translatable("gui.indexer.manual.menu"),
            b -> switchToMenu()
        ).bounds(leftPos + 5, topPos + 5, 40, 20).build();
        
        // Botones del menú principal
        this.menuBtnWidth = 80;
        this.menuBtnHeight =70;
        this.menuStartX = centerX - (menuBtnWidth * 3 + 20) / 2; // 3 botones + separaciones
        this.menuY = topPos + 60;
        this.craftingButton = new TexturedMenuButton(menuStartX, menuY, menuBtnWidth, menuBtnHeight, Component.translatable("gui.indexer.manual.crafting"), b -> switchToCrafting(), MENU_CRAFT_TEXTURE);
        this.tutorialButton = new TexturedMenuButton(menuStartX + menuBtnWidth + 10, menuY, menuBtnWidth, menuBtnHeight, Component.translatable("gui.indexer.manual.tutorial"), b -> switchToTutorial(), MENU_TUTORIAL_TEXTURE);
        this.wikiButton = new TexturedMenuButton(menuStartX + (menuBtnWidth + 10) * 2, menuY, menuBtnWidth, menuBtnHeight, Component.translatable("gui.indexer.manual.wiki"), b -> openWiki(), MENU_WIKI_TEXTURE);
        
        // Caja de búsqueda para crafteos
        this.searchBox = new EditBox(this.font, leftPos + 20, topPos + 30, SCREEN_WIDTH - 40, 18, Component.translatable("gui.indexer.manual.search_placeholder"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(s -> applyCraftingFilter(s));
        
        // Añadir botones a la pantalla
        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(closeButton);
        this.addRenderableWidget(menuButton);
        this.addRenderableWidget(craftingButton);
        this.addRenderableWidget(tutorialButton);
        this.addRenderableWidget(wikiButton);
        this.addRenderableWidget(searchBox);
        
        // Actualizar estado de los botones
        updateButtonStates();
        updateVisibilityForView();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xA0000000);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int leftPos = centerX - (SCREEN_WIDTH / 2);
        int topPos = centerY - (SCREEN_HEIGHT / 2);
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500);
        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, SCREEN_WIDTH, SCREEN_HEIGHT);

        if (inSplash) {
            long now = System.currentTimeMillis();
            if (now < splashEndMillis) {
                int logoSize = 80;
                long elapsed = Math.max(0L, 1000L - (splashEndMillis - now));
                float angle = (elapsed * 180f) / 1000f;
                var pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(centerX, topPos + SCREEN_HEIGHT / 2, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(angle));
                guiGraphics.blit(LOGO, -logoSize / 2, -logoSize / 2, 0, 0, logoSize, logoSize, logoSize, logoSize);
                pose.popPose();
                guiGraphics.pose().popPose();
                return;
            } else {
                inSplash = false;
                currentView = ViewMode.MENU;
                updateVisibilityForView();
            }
        }

        switch (currentView) {
            case MENU -> {
                String title = net.minecraft.client.resources.language.I18n.get("item.indexer.indexer_manual");
                guiGraphics.drawString(this.font, title, centerX - (this.font.width(title) / 2), topPos + 30, 0xFFFFFF, false);
            }
            case TUTORIAL -> {
                String pageText = (currentPage + 1) + "/" + totalPages;
                guiGraphics.drawString(this.font, pageText, centerX - (this.font.width(pageText) / 2), topPos + SCREEN_HEIGHT - 15, 0xFFFFFF, false);
                try {
                    guiGraphics.blit(pageImages[currentPage], leftPos + 28, topPos + 30, 0, 0, 200, 120, 200, 120);
                } catch (Exception e) {
                    guiGraphics.drawString(this.font, net.minecraft.client.resources.language.I18n.get("gui.indexer.manual.error_image"), leftPos + 28, topPos + 70, 0xFF0000, false);
                }
            }
            case CRAFTING -> {
                renderCraftingList(guiGraphics, leftPos, topPos, mouseX, mouseY);
            }
        }
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 600);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().popPose();
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xA0000000);
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
        if (currentView == ViewMode.TUTORIAL) {
            if (currentPage < totalPages - 1) {
                currentPage++;
            }
        } else if (currentView == ViewMode.CRAFTING) {
            int maxPage = Math.max(0, (int)Math.ceil((double)filteredCraftingRecipes.size() / recipesPerPage) - 1);
            if (craftingPage < maxPage) {
                craftingPage++;
            }
        }
        updateButtonStates();
    }
    
    /**
     * Goes back to the previous page
     */
    private void previousPage() {
        if (currentView == ViewMode.TUTORIAL) {
            if (currentPage > 0) {
                currentPage--;
            }
        } else if (currentView == ViewMode.CRAFTING) {
            if (craftingPage > 0) {
                craftingPage--;
            }
        }
        updateButtonStates();
    }
    
    /**
     * Updates the state of navigation buttons
     */
    private void updateButtonStates() {
        if (currentView == ViewMode.TUTORIAL) {
            this.prevButton.active = currentPage > 0;
            this.nextButton.active = currentPage < totalPages - 1;
        } else if (currentView == ViewMode.CRAFTING) {
            int maxPage = Math.max(0, (int)Math.ceil((double)filteredCraftingRecipes.size() / recipesPerPage) - 1);
            this.prevButton.active = craftingPage > 0;
            this.nextButton.active = craftingPage < maxPage;
        } else {
            this.prevButton.active = false;
            this.nextButton.active = false;
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // No pausar el juego cuando se muestra esta pantalla
    }
    
    // ---- Helpers de vista ----
    private void switchToMenu() {
        currentView = ViewMode.MENU;
        updateVisibilityForView();
    }
    private void switchToTutorial() {
        currentView = ViewMode.TUTORIAL;
        updateVisibilityForView();
    }
    private void switchToCrafting() {
        currentView = ViewMode.CRAFTING;
        if (this.minecraft != null && this.minecraft.level != null) {
            loadModCraftingRecipes();
            applyCraftingFilter(this.searchBox.getValue());
        }
        craftingPage = 0;
        updateVisibilityForView();
    }
    private void openWiki() {
        this.minecraft.setScreen(new ConfirmLinkScreen(accepted -> {
            if (accepted) {
                Util.getPlatform().openUri(WIKI_URL);
            }
            this.minecraft.setScreen(this);
        }, WIKI_URL, true));
    }
    private void updateVisibilityForView() {
        if (inSplash) {
            // Ocultar todo durante el splash
            this.prevButton.visible = false;
            this.nextButton.visible = false;
            this.menuButton.visible = false;
            this.craftingButton.visible = false;
            this.tutorialButton.visible = false;
            this.wikiButton.visible = false;
            this.searchBox.visible = false;
            return;
        }
        boolean isMenu = currentView == ViewMode.MENU;
        boolean isTutorial = currentView == ViewMode.TUTORIAL;
        boolean isCrafting = currentView == ViewMode.CRAFTING;
        // Navegación
        this.prevButton.visible = isTutorial || isCrafting;
        this.nextButton.visible = isTutorial || isCrafting;
        this.menuButton.visible = !isMenu; // mostrar volver al menú fuera del menú
        // Menú principal
        this.craftingButton.visible = isMenu;
        this.tutorialButton.visible = isMenu;
        this.wikiButton.visible = isMenu;
        // Búsqueda crafteos
        this.searchBox.visible = isCrafting;
        updateButtonStates();
    }
    
    // ---- Crafteos ----
    private void loadModCraftingRecipes() {
        try {
            var manager = this.minecraft.level.getRecipeManager();
            var recipes = manager.getAllRecipesFor(RecipeType.CRAFTING);
            java.util.List<Recipe<?>> modList = new java.util.ArrayList<>();
            for (net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> holder : recipes) {
                Recipe<?> recipe = holder.value();
                ItemStack result = recipe.getResultItem(this.minecraft.level.registryAccess());
                if (!result.isEmpty()) {
                    var key = BuiltInRegistries.ITEM.getKey(result.getItem());
                    if (key != null && IndexerMod.MOD_ID.equals(key.getNamespace())) {
                        modList.add(recipe);
                    }
                }
            }
            this.modCraftingRecipes = modList;
            this.filteredCraftingRecipes = modList;
        } catch (Exception e) {
            // En caso de error, mantener listas vacías
            this.modCraftingRecipes = java.util.Collections.emptyList();
            this.filteredCraftingRecipes = java.util.Collections.emptyList();
        }
    }
    private void applyCraftingFilter(String query) {
        if (query == null) query = "";
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) {
            this.filteredCraftingRecipes = this.modCraftingRecipes;
        } else {
            java.util.List<Recipe<?>> filtered = new java.util.ArrayList<>();
            for (Recipe<?> recipe : this.modCraftingRecipes) {
                ItemStack result = recipe.getResultItem(this.minecraft.level.registryAccess());
                String name = I18n.get(result.getDescriptionId()).toLowerCase();
                if (name.contains(q)) {
                    filtered.add(recipe);
                }
            }
            this.filteredCraftingRecipes = filtered;
        }
        this.craftingPage = 0;
        updateButtonStates();
    }
    private void renderCraftingList(GuiGraphics g, int leftPos, int topPos, int mouseX, int mouseY) {
        // Título y caja búsqueda ya posicionados
        String title = net.minecraft.client.resources.language.I18n.get("gui.indexer.manual.crafting");
        int centerX = this.width / 2;
        g.drawString(this.font, title, centerX - (this.font.width(title) / 2), topPos + 12, 0xFFFFFF, false);
        this.searchBox.render(g, mouseX, mouseY, 0f);
        
        // Calcular ventana de recetas
        int startIndex = craftingPage * recipesPerPage;
        int endIndex = Math.min(startIndex + recipesPerPage, filteredCraftingRecipes.size());

        int baseY = topPos + 60;
        int columnWidth = (SCREEN_WIDTH - 60) / 2; // dos columnas dentro del marco
        int columnGap = 20;
        int rowHeight = 24 + (3 * 18) + 12; // nombre/icono + grid 3x3 + margen

        for (int i = startIndex; i < endIndex; i++) {
            int localIndex = i - startIndex;
            int col = localIndex % 2;
            int row = localIndex / 2;
            int baseX = leftPos + 20 + col * (columnWidth + columnGap);
            int y = baseY + row * rowHeight;

            Recipe<?> recipe = filteredCraftingRecipes.get(i);
            ItemStack result = recipe.getResultItem(this.minecraft.level.registryAccess());

            // icono y nombre con recorte si es demasiado largo
            g.renderItem(result, baseX, y);
            String name = I18n.get(result.getDescriptionId());
            int nameMaxWidth = columnWidth - 26; // margen por icono y separación
            String shownName = ellipsize(name, nameMaxWidth);
            g.drawString(this.font, shownName, baseX + 22, y + 6, 0xFFFFFF, false);

            // grid bajo el nombre
            int gridX = baseX;
            int gridY = y + 24;
            renderRecipeGrid(g, recipe, gridX, gridY);
        }
        
        // Mostrar página crafteos
        String pageText = (filteredCraftingRecipes.isEmpty() ? 0 : (craftingPage + 1)) + "/" + Math.max(1, (int)Math.ceil((double)filteredCraftingRecipes.size() / recipesPerPage));
        g.drawString(this.font, pageText, centerX - (this.font.width(pageText) / 2), topPos + SCREEN_HEIGHT - 15, 0xFFFFFF, false);
    }
    private void renderRecipeGrid(GuiGraphics g, Recipe<?> recipe, int gridX, int gridY) {
        // Siempre dibujar un grid 3x3 manteniendo huecos vacíos
        if (recipe instanceof ShapedRecipe shaped) {
            int w = shaped.getWidth();
            int h = shaped.getHeight();
            java.util.List<Ingredient> ing = shaped.getIngredients();
            int offsetX = (3 - Math.min(3, w)) / 2;
            int offsetY = (3 - Math.min(3, h)) / 2;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int slotX = gridX + c * 18;
                    int slotY = gridY + r * 18;
                    boolean inside = (c >= offsetX && c < offsetX + Math.min(3, w)) && (r >= offsetY && r < offsetY + Math.min(3, h));
                    if (inside) {
                        int rr = r - offsetY;
                        int cc = c - offsetX;
                        int idx = rr * w + cc;
                        ItemStack stack = ItemStack.EMPTY;
                        if (idx >= 0 && idx < ing.size()) {
                            ItemStack[] stacks = ing.get(idx).getItems();
                            if (stacks.length > 0) stack = stacks[0];
                        }
                        if (!stack.isEmpty()) {
                            g.renderItem(stack, slotX, slotY);
                        } else {
                            drawEmptySlot(g, slotX, slotY);
                        }
                    } else {
                        drawEmptySlot(g, slotX, slotY);
                    }
                }
            }
        } else {
            // Shapeless o genérico: colocar en orden y rellenar el resto con vacíos
            java.util.List<Ingredient> ing = recipe.getIngredients();
            int count = Math.min(9, ing.size());
            for (int idx = 0; idx < 9; idx++) {
                int c = idx % 3;
                int r = idx / 3;
                int slotX = gridX + c * 18;
                int slotY = gridY + r * 18;
                if (idx < count) {
                    ItemStack[] stacks = ing.get(idx).getItems();
                    ItemStack stack = stacks.length > 0 ? stacks[0] : ItemStack.EMPTY;
                    if (!stack.isEmpty()) {
                        g.renderItem(stack, slotX, slotY);
                    } else {
                        drawEmptySlot(g, slotX, slotY);
                    }
                } else {
                    drawEmptySlot(g, slotX, slotY);
                }
            }
        }
    }

    private void drawEmptySlot(GuiGraphics g, int x, int y) {
        // Simple slot placeholder para mantener la forma
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF1A1A1A);
        g.fill(x, y, x + 16, y + 16, 0xFF2D2D30);
    }

    private String ellipsize(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        String ell = "...";
        int ellW = this.font.width(ell);
        int target = Math.max(0, maxWidth - ellW);
        int cut = Math.max(0, text.length());
        while (cut > 0 && this.font.width(text.substring(0, cut)) > target) {
            cut--;
        }
        return (cut <= 0 ? ell : text.substring(0, cut) + ell);
    }
}
