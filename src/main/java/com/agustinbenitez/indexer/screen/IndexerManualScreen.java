package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.IndexerMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import com.mojang.math.Axis;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pantalla GUI para el manual del Indexer.
 */
@OnlyIn(Dist.CLIENT)
public class IndexerManualScreen extends Screen {
    private static final int SCREEN_WIDTH = 271;
    private static final int SCREEN_HEIGHT = 180;
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID,
            "textures/gui/manual/manualgui.png");
    private static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID,
            "textures/block/indexer_controller_top.png");
    private static final String WIKI_URL = "https://agustinbeniteez.github.io/Wikimods/mod/index.html?id=indexer&game=minecraft";

    private Button nextButton;
    private Button prevButton;
    private Button closeButton;
    private Button menuButton;
    private Button craftingButton;
    private Button tutorialButton;
    private Button wikiButton;
    private EditBox searchBox;

    private int currentPage = 0;
    private final int totalPages = 13;
    private final ResourceLocation[] pageImages = new ResourceLocation[totalPages];

    private enum ViewMode {
        MENU, TUTORIAL, CRAFTING
    }

    private ViewMode currentView = ViewMode.MENU;
    private boolean inSplash = true;
    private long splashEndMillis = 0L;

    private static final ResourceLocation MENU_CRAFT_TEXTURE = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID,
            "textures/gui/manual/menu/menucraft.png");
    private static final ResourceLocation MENU_TUTORIAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            IndexerMod.MOD_ID,
            "textures/gui/manual/menu/menututorial.png");
    private static final ResourceLocation MENU_WIKI_TEXTURE = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID,
            "textures/gui/manual/menu/menuwiki.png");

    // Clase interna para representar una receta manualmente cargada desde el JSON
    private static class ManualRecipe {
        ItemStack result;
        // Grid flattenado de 9 items (3x3). Si es shapeless, se rellena
        // secuencialmente.
        List<ItemStack> ingredients = new ArrayList<>(9);

        public ManualRecipe(ItemStack result) {
            this.result = result;
            for (int i = 0; i < 9; i++)
                ingredients.add(ItemStack.EMPTY);
        }
    }

    private static class TexturedMenuButton extends Button {
        private final ResourceLocation texture;

        public TexturedMenuButton(int x, int y, int width, int height, Component message, OnPress onPress,
                ResourceLocation texture) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.texture = texture;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();

            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, texture);
            com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10241, 9728);
            com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10240, 9728);

            if (this.isHoveredOrFocused()) {
                int dw = (int) Math.round(w * 1.06);
                int dh = (int) Math.round(h * 1.06);
                int dx = x - (dw - w) / 2;
                int dy = y - (dh - h) / 2;
                guiGraphics.blit(texture, dx, dy, 0, 0, dw, dh, w, h);
                guiGraphics.fill(x, y, x + w, y + h, 0x66000000);
                guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), x + w / 2,
                        y + (h - 8) / 2, 0xFFFFFF);
            } else {
                guiGraphics.blit(texture, x, y, 0, 0, w, h, w, h);
            }
            com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10241, 9729);
            com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10240, 9729);
        }
    }

    private List<ManualRecipe> allManualRecipes = Collections.emptyList();
    private List<ManualRecipe> filteredRecipes = Collections.emptyList();
    private int craftingPage = 0;
    private static final int recipesPerPage = 2; // Dos columnas

    public IndexerManualScreen() {
        super(Component.translatable("item.indexer.indexer_manual"));
        for (int i = 0; i < totalPages; i++) {
            pageImages[i] = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID,
                    "textures/gui/manual/manual" + (i + 1) + ".png");
        }
        this.splashEndMillis = System.currentTimeMillis() + 1000L;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int leftPos = centerX - (SCREEN_WIDTH / 2);
        int topPos = centerY - (SCREEN_HEIGHT / 2);

        this.prevButton = Button.builder(Component.literal("<"), button -> previousPage())
                .bounds(leftPos + 10, topPos + SCREEN_HEIGHT - 30, 20, 20).build();
        this.nextButton = Button.builder(Component.literal(">"), button -> nextPage())
                .bounds(leftPos + SCREEN_WIDTH - 30, topPos + SCREEN_HEIGHT - 30, 20, 20).build();
        this.closeButton = Button.builder(Component.literal("X"), button -> this.onClose())
                .bounds(leftPos + SCREEN_WIDTH - 25, topPos + 5, 20, 20).build();
        this.menuButton = Button.builder(Component.translatable("gui.indexer.manual.menu"), b -> switchToMenu())
                .bounds(leftPos + 5, topPos + 5, 40, 20).build();

        int menuBtnWidth = 80;
        int menuBtnHeight = 70;
        int menuStartX = centerX - (menuBtnWidth * 3 + 20) / 2;
        int menuY = topPos + 60;

        this.craftingButton = new TexturedMenuButton(menuStartX, menuY, menuBtnWidth, menuBtnHeight,
                Component.translatable("gui.indexer.manual.crafting"), b -> switchToCrafting(), MENU_CRAFT_TEXTURE);
        this.tutorialButton = new TexturedMenuButton(menuStartX + menuBtnWidth + 10, menuY, menuBtnWidth, menuBtnHeight,
                Component.translatable("gui.indexer.manual.tutorial"), b -> switchToTutorial(), MENU_TUTORIAL_TEXTURE);
        this.wikiButton = new TexturedMenuButton(menuStartX + (menuBtnWidth + 10) * 2, menuY, menuBtnWidth,
                menuBtnHeight, Component.translatable("gui.indexer.manual.wiki"), b -> openWiki(), MENU_WIKI_TEXTURE);

        this.searchBox = new EditBox(this.font, leftPos + 20, topPos + 30, SCREEN_WIDTH - 40, 18,
                Component.translatable("gui.indexer.manual.search_placeholder"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(s -> applyCraftingFilter(s));

        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(closeButton);
        this.addRenderableWidget(menuButton);
        this.addRenderableWidget(craftingButton);
        this.addRenderableWidget(tutorialButton);
        this.addRenderableWidget(wikiButton);
        this.addRenderableWidget(searchBox);

        // Cargar recetas desde JSON manual
        loadManualRecipesFromFile();

        updateButtonStates();
        updateVisibilityForView();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x44000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int leftPos = centerX - (SCREEN_WIDTH / 2);
        int topPos = centerY - (SCREEN_HEIGHT / 2);

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 10);

        // --- FONDO (manualgui.png) ---
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, BACKGROUND);
        com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10241, 9728);
        com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10240, 9728);
        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, SCREEN_WIDTH, SCREEN_HEIGHT);

        if (inSplash) {
            long now = System.currentTimeMillis();
            if (now < splashEndMillis) {
                int logoSize = 80;
                long elapsed = Math.max(0L, 1000L - (splashEndMillis - now));
                float angle = (elapsed * 180f) / 1000f;
                pose.pushPose();
                pose.translate(centerX, topPos + SCREEN_HEIGHT / 2, 50);
                pose.mulPose(Axis.ZP.rotationDegrees(angle));
                com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, LOGO);
                guiGraphics.blit(LOGO, -(int) (logoSize / 2), -(int) (logoSize / 2), 0, 0, logoSize, logoSize, logoSize,
                        logoSize);
                pose.popPose();
                pose.popPose();
                return;
            } else {
                inSplash = false;
                currentView = ViewMode.MENU;
                updateVisibilityForView();
            }
        }

        pose.pushPose();
        pose.translate(0, 0, 30);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 100);

        switch (currentView) {
            case MENU -> {
                String title = I18n.get("item.indexer.indexer_manual");
                guiGraphics.drawString(this.font, title, centerX - (this.font.width(title) / 2), topPos + 30, 0x404040,
                        false);
            }
            case TUTORIAL -> {
                String pageText = (currentPage + 1) + "/" + totalPages;
                guiGraphics.drawString(this.font, pageText, centerX - (this.font.width(pageText) / 2),
                        topPos + SCREEN_HEIGHT - 17, 0x404040, false);

                try {
                    com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, pageImages[currentPage]);
                    com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10241, 9728);
                    com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10240, 9728);
                    guiGraphics.blit(pageImages[currentPage], leftPos + 35, topPos + 30, 0, 0, 200, 120, 200, 120);
                } catch (Exception e) {
                    guiGraphics.drawString(this.font, I18n.get("gui.indexer.manual.error_image"), leftPos + 50,
                            topPos + 70, 0xAA0000, false);
                }
            }
            case CRAFTING -> {
                renderCraftingList(guiGraphics, leftPos, topPos, mouseX, mouseY);
            }
        }
        pose.popPose();
        pose.popPose();

        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderCraftingList(GuiGraphics g, int leftPos, int topPos, int mouseX, int mouseY) {
        String title = I18n.get("gui.indexer.manual.crafting");
        int centerX = this.width / 2;
        g.drawString(this.font, title, centerX - (this.font.width(title) / 2), topPos + 12, 0xFFFFFF, false);

        if (filteredRecipes.isEmpty()) {
            String noRecipes = "No recipes found";
            g.drawString(this.font, noRecipes, centerX - (this.font.width(noRecipes) / 2), topPos + 80, 0xFFFFFF,
                    false);
        } else {
            int startIndex = craftingPage * recipesPerPage;
            int endIndex = Math.min(startIndex + recipesPerPage, filteredRecipes.size());

            int baseY = topPos + 55;
            int columnWidth = (SCREEN_WIDTH - 60) / 2;
            int columnGap = 10;
            int rowHeight = 70;

            for (int i = startIndex; i < endIndex; i++) {
                int localIndex = i - startIndex;
                int col = localIndex % 2;
                int row = localIndex / 2;
                int baseX = leftPos + 25 + col * (columnWidth + columnGap);
                int y = baseY + row * rowHeight;

                ManualRecipe recipe = filteredRecipes.get(i);

                // 1. Render result item
                g.renderItem(recipe.result, baseX, y);

                // 2. Render name
                String name = I18n.get(recipe.result.getDescriptionId());
                String shownName = ellipsize(name, columnWidth - 20);
                g.drawString(this.font, shownName, baseX + 20, y + 4, 0xFFFFFF, false);

                // 3. Render grid
                int gridX = baseX;
                int gridY = y + 20;
                renderRecipeGrid(g, recipe, gridX, gridY);
            }
        }

        String pageText = (filteredRecipes.isEmpty() ? 0 : (craftingPage + 1)) + "/"
                + Math.max(1, (int) Math.ceil((double) filteredRecipes.size() / recipesPerPage));
        g.drawString(this.font, pageText, centerX - (this.font.width(pageText) / 2), topPos + SCREEN_HEIGHT - 17,
                0xFFFFFF, false);
    }

    private void renderRecipeGrid(GuiGraphics g, ManualRecipe recipe, int gridX, int gridY) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int idx = r * 3 + c;
                int slotX = gridX + c * 18;
                int slotY = gridY + r * 18;

                if (idx < recipe.ingredients.size()) {
                    ItemStack stack = recipe.ingredients.get(idx);
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
        g.fill(x, y, x + 16, y + 16, 0xFF2E2E2E);
        g.fill(x, y, x + 16, y + 1, 0xFF1A1A1A);
        g.fill(x, y, x + 1, y + 16, 0xFF1A1A1A);
    }

    private void nextPage() {
        if (currentView == ViewMode.TUTORIAL) {
            if (currentPage < totalPages - 1)
                currentPage++;
        } else if (currentView == ViewMode.CRAFTING) {
            int maxPage = Math.max(0, (int) Math.ceil((double) filteredRecipes.size() / recipesPerPage) - 1);
            if (craftingPage < maxPage)
                craftingPage++;
        }
        updateButtonStates();
    }

    private void previousPage() {
        if (currentView == ViewMode.TUTORIAL) {
            if (currentPage > 0)
                currentPage--;
        } else if (currentView == ViewMode.CRAFTING) {
            if (craftingPage > 0)
                craftingPage--;
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (currentView == ViewMode.TUTORIAL) {
            this.prevButton.active = currentPage > 0;
            this.nextButton.active = currentPage < totalPages - 1;
        } else if (currentView == ViewMode.CRAFTING) {
            int maxPage = Math.max(0, (int) Math.ceil((double) filteredRecipes.size() / recipesPerPage) - 1);
            this.prevButton.active = craftingPage > 0;
            this.nextButton.active = craftingPage < maxPage;
        } else {
            this.prevButton.active = this.nextButton.active = false;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

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
        // La carga ya se hizo en init
        applyCraftingFilter(this.searchBox.getValue());
        craftingPage = 0;
        updateVisibilityForView();
    }

    private void openWiki() {
        if (this.minecraft == null)
            return;
        this.minecraft.setScreen(new ConfirmLinkScreen(accepted -> {
            if (accepted)
                Util.getPlatform().openUri(WIKI_URL);
            this.minecraft.setScreen(this);
        }, WIKI_URL, true));
    }

    private void updateVisibilityForView() {
        if (inSplash) {
            this.prevButton.visible = this.nextButton.visible = this.menuButton.visible = this.craftingButton.visible = this.tutorialButton.visible = this.wikiButton.visible = this.searchBox.visible = false;
            return;
        }
        boolean isMenu = currentView == ViewMode.MENU;
        boolean isTutorial = currentView == ViewMode.TUTORIAL;
        boolean isCrafting = currentView == ViewMode.CRAFTING;
        this.prevButton.visible = this.nextButton.visible = isTutorial || isCrafting;
        this.menuButton.visible = !isMenu;
        this.craftingButton.visible = this.tutorialButton.visible = this.wikiButton.visible = isMenu;
        this.searchBox.visible = isCrafting;
        updateButtonStates();
    }

    // --- LOGICA DE CARGA MANUAL ---

    private void loadManualRecipesFromFile() {
        if (this.minecraft == null)
            return;
        List<ManualRecipe> loaded = new ArrayList<>();
        try {
            ResourceLocation jsonLoc = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID, "manual_recipes.json");
            Optional<Resource> resource = this.minecraft.getResourceManager().getResource(jsonLoc);

            if (resource.isPresent()) {
                try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                    JsonElement root = JsonParser.parseReader(reader);
                    if (root.isJsonArray()) {
                        JsonArray arr = root.getAsJsonArray();
                        for (JsonElement el : arr) {
                            ManualRecipe r = parseRecipe(el.getAsJsonObject());
                            if (r != null)
                                loaded.add(r);
                        }
                    }
                }
            } else {
                IndexerMod.LOGGER.error("Manual JSON not found at: " + jsonLoc);
            }
        } catch (Exception e) {
            IndexerMod.LOGGER.error("Error loading manual recipes JSON", e);
        }
        this.allManualRecipes = loaded;
        this.filteredRecipes = new ArrayList<>(loaded);
        IndexerMod.LOGGER.info("Manual loaded {} recipes from JSON.", loaded.size());
    }

    private ManualRecipe parseRecipe(JsonObject json) {
        try {
            // Result
            JsonObject resObj = json.getAsJsonObject("result");
            String itemId = resObj.get("item").getAsString();
            int count = resObj.has("count") ? resObj.get("count").getAsInt() : 1;
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            if (item == Items.AIR)
                return null; // Item no existe o error

            ManualRecipe recipe = new ManualRecipe(new ItemStack(item, count));

            String type = json.get("type").getAsString();

            if (type.equals("minecraft:crafting_shaped")) {
                JsonArray pattern = json.getAsJsonArray("pattern");
                JsonObject keyMap = json.getAsJsonObject("key");

                int height = pattern.size();
                int width = pattern.get(0).getAsString().length();

                // Mapear chars a ItemStacks
                Map<Character, ItemStack> charMap = new HashMap<>();
                for (String k : keyMap.keySet()) {
                    JsonObject entry = keyMap.getAsJsonObject(k);
                    charMap.put(k.charAt(0), parseIngredient(entry));
                }
                charMap.put(' ', ItemStack.EMPTY);

                // Rellenar grid 3x3 centrado o top-left?
                // Vamos a mapearlo al grid 3x3 de nuestra clase ManualRecipe.
                // Simple implementation: put at top-left.
                for (int r = 0; r < height; r++) {
                    String rowStr = pattern.get(r).getAsString();
                    for (int c = 0; c < width; c++) {
                        char ch = rowStr.charAt(c);
                        ItemStack stack = charMap.getOrDefault(ch, ItemStack.EMPTY);
                        // ManualRecipe tiene lista plana de 9 (0..8).
                        // Pos = r*3 + c.
                        if (r < 3 && c < 3) {
                            recipe.ingredients.set(r * 3 + c, stack);
                        }
                    }
                }
            } else if (type.equals("minecraft:crafting_shapeless")) {
                JsonArray ings = json.getAsJsonArray("ingredients");
                for (int i = 0; i < Math.min(ings.size(), 9); i++) {
                    JsonObject entry = ings.get(i).getAsJsonObject();
                    recipe.ingredients.set(i, parseIngredient(entry));
                }
            }
            return recipe;
        } catch (Exception e) {
            IndexerMod.LOGGER.warn("Failed to parse manual recipe", e);
            return null;
        }
    }

    private ItemStack parseIngredient(JsonObject obj) {
        if (obj.has("item")) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(obj.get("item").getAsString()));
            return new ItemStack(item);
        } else if (obj.has("tag")) {
            // Pick first item from tag to display
            String tagName = obj.get("tag").getAsString();
            // Esto es client side, a veces es tricky obtener tags.
            // Para simplificar, hardcodeamos los más comunes o intentamos resolver.
            if (tagName.equals("forge:ingots/iron"))
                return new ItemStack(Items.IRON_INGOT);
            if (tagName.equals("forge:nuggets/gold"))
                return new ItemStack(Items.GOLD_NUGGET);

            // Intento generico:
            TagKey<Item> key = TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.parse(tagName));
            var tag = BuiltInRegistries.ITEM.getTag(key);
            if (tag.isPresent()) {
                var first = tag.get().stream().findFirst();
                if (first.isPresent()) {
                    return new ItemStack(first.get().value());
                }
            }
            // Si falla el tag, devolver barrier o algo visible? O air.
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    private void applyCraftingFilter(String query) {
        String q = (query == null ? "" : query.trim().toLowerCase());
        if (q.isEmpty()) {
            this.filteredRecipes = this.allManualRecipes;
        } else {
            List<ManualRecipe> filtered = new ArrayList<>();
            for (ManualRecipe r : this.allManualRecipes) {
                String localeName = I18n.get(r.result.getDescriptionId()).toLowerCase();
                if (localeName.contains(q))
                    filtered.add(r);
            }
            this.filteredRecipes = filtered;
        }
        this.craftingPage = 0;
        updateButtonStates();
    }

    private String ellipsize(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth)
            return text;
        String ell = "...";
        int cut = text.length();
        while (cut > 0 && this.font.width(text.substring(0, cut) + ell) > maxWidth)
            cut--;
        return (cut <= 0 ? ell : text.substring(0, cut) + ell);
    }
}
