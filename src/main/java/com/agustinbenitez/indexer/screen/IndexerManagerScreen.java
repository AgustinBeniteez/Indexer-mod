package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.menu.IndexerManagerMenu;
import com.agustinbenitez.indexer.network.ModNetworking;
import com.agustinbenitez.indexer.network.RequestManagerItemsPacket;
import com.agustinbenitez.indexer.network.ExtractItemFromManagerPacket;
import com.agustinbenitez.indexer.IndexerMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ShearsItem;

import java.util.*;
import java.text.DecimalFormat;

public class IndexerManagerScreen extends AbstractContainerScreen<IndexerManagerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(IndexerMod.MOD_ID, "textures/gui/indexer_manager.png");
    private static final ResourceLocation FILTER_ICON = new ResourceLocation(IndexerMod.MOD_ID, "textures/gui/filter.png");
    private static final int STATS_PANEL_WIDTH = 100;
    private EditBox searchBox;
    private final List<ItemEntry> items = new ArrayList<>();
    private final List<ItemEntry> filtered = new ArrayList<>();
    private int scrollRowOffset = 0;
    private boolean draggingScrollbar = false;
    private String searchPlaceholder = "";
    private int failX = -1;
    private int failY = -1;
    private int failTicks = 0;
    private boolean sortMenuOpen = false;
    private SortMode currentSort = SortMode.NONE;
    private int filterBtnX = 0;
    private int filterBtnY = 0;

    public IndexerManagerScreen(IndexerManagerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176 + STATS_PANEL_WIDTH;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        int x = this.leftPos + 8;
        int y = this.topPos + 6;
        this.searchBox = new EditBox(this.font, x, y, 120, 12, Component.literal(""));
        this.searchPlaceholder = Component.translatable("gui.indexer.manager.search_placeholder").getString();
        this.searchBox.setSuggestion(this.searchPlaceholder);
        this.searchBox.setResponder(s -> updateSuggestion());
        this.addRenderableWidget(this.searchBox);
        ModNetworking.sendToServer(new RequestManagerItemsPacket(this.menu.getBlockEntity().getBlockPos()));
        filterBtnX = x + 124;
        filterBtnY = y;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 176, this.imageHeight);
        int statsX = this.leftPos + 176;
        graphics.fill(statsX, this.topPos, statsX + STATS_PANEL_WIDTH, this.topPos + 166, 0xC0101010);
        renderStats(graphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        String query = searchBox.getValue() == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT);
        filtered.clear();
        for (ItemEntry e : items) {
            Item item = ForgeRegistries.ITEMS.getValue(e.id);
            String name = "";
            if (item != null) {
                name = new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT);
            }
            if (query.isEmpty() || e.id.toString().toLowerCase(Locale.ROOT).contains(query) || name.contains(query)) {
                filtered.add(e);
            }
        }
        sortFiltered();
        int areaX = this.leftPos + 8;
        int areaY = this.topPos + 24;
        int areaW = 176 - 16;
        int areaH = 60;
        int itemSpacing = 20;
        int itemsPerRow = Math.max(1, areaW / itemSpacing);
        int visibleRows = Math.max(1, areaH / itemSpacing);
        int totalRows = (int) Math.ceil(filtered.size() / (double) itemsPerRow);
        if (scrollRowOffset < 0) scrollRowOffset = 0;
        int maxRowOffset = Math.max(0, totalRows - visibleRows);
        if (scrollRowOffset > maxRowOffset) scrollRowOffset = maxRowOffset;
        int startIndex = scrollRowOffset * itemsPerRow;
        int endIndex = Math.min(filtered.size(), startIndex + visibleRows * itemsPerRow);
        int idx = 0;
        boolean overMenu = false;
        if (sortMenuOpen) {
            int menuX = filterBtnX - 2;
            int itemH = 12;
            int itemsCount = 5;
            int menuW = 120;
            int menuH = itemH * itemsCount + 6;
            int belowY = filterBtnY + 18;
            int aboveY = filterBtnY - menuH - 2;
            int menuY = belowY;
            if (menuY + menuH > this.topPos + this.imageHeight - 8) {
                menuY = aboveY;
            }
            overMenu = mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= menuY && mouseY <= menuY + menuH;
        }
        for (int i = startIndex; i < endIndex; i++) {
            ItemEntry e = filtered.get(i);
            int col = idx % itemsPerRow;
            int row = idx / itemsPerRow;
            int ix = areaX + col * itemSpacing;
            int iy = areaY + row * itemSpacing;
            Item item = ForgeRegistries.ITEMS.getValue(e.id);
            ItemStack stack = ItemStack.EMPTY;
            if (item != null) stack = new ItemStack(item);
            graphics.renderItem(stack, ix, iy);
            String countStr = formatCount(e.count);
            graphics.renderItemDecorations(this.font, stack, ix, iy, countStr);
            if (!overMenu && mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                List<Component> tooltip = new ArrayList<>();
                boolean full = isManagerInventoryFull();
                if (!stack.isEmpty()) {
                    MutableComponent name = Component.literal(stack.getHoverName().getString());
                    tooltip.add(full ? name.withStyle(ChatFormatting.RED) : name);
                }
                tooltip.add(Component.literal("x" + e.count));
                graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
            idx++;
        }
        if (totalRows > visibleRows) {
            int scrollbarWidth = 6;
            int scrollbarX = areaX + areaW - scrollbarWidth + 2;
            int scrollbarY = areaY;
            int scrollbarHeight = areaH;
            // Fondo del scrollbar: #171717
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, 0xFF171717);
            int thumbHeight = Math.max(10, (visibleRows * scrollbarHeight) / totalRows);
            int thumbY = scrollbarY + (scrollRowOffset * (scrollbarHeight - thumbHeight)) / Math.max(1, (totalRows - visibleRows));
            // Barra del scrollbar: #2e5d70
            graphics.fill(scrollbarX + 1, thumbY, scrollbarX + scrollbarWidth - 1, thumbY + thumbHeight, 0xFF2e5d70);
        }
        if (failTicks > 0 && failX >= 0 && failY >= 0) {
            graphics.drawCenteredString(this.font, "X", failX, failY, 0xFF0000);
            failTicks--;
        }
        graphics.blit(FILTER_ICON, filterBtnX, filterBtnY, 0, 0, 16, 16, 16, 16);
        if (sortMenuOpen) {
            int menuX = filterBtnX - 2;
            int itemH = 12;
            int itemsCount = 5;
            int menuW = 120;
            int menuH = itemH * itemsCount + 6;
            int belowY = filterBtnY + 18;
            int aboveY = filterBtnY - menuH - 2;
            int menuY = belowY;
            if (menuY + menuH > this.topPos + this.imageHeight - 8) {
                menuY = aboveY;
            }
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            graphics.fill(menuX, menuY, menuX + menuW, menuY + menuH, 0xC0202020);
            graphics.fill(menuX, menuY, menuX + menuW, menuY + 1, 0xFF2e5d70);
            graphics.fill(menuX, menuY + menuH - 1, menuX + menuW, menuY + menuH, 0xFF2e5d70);
            int ty = menuY + 4;
            drawMenuItem(graphics, Component.translatable("gui.indexer.manager.sort_cantidad"), menuX + 6, ty, currentSort == SortMode.CANTIDAD); ty += itemH;
            drawMenuItem(graphics, Component.translatable("gui.indexer.manager.sort_alfabeto"), menuX + 6, ty, currentSort == SortMode.ALFABETO); ty += itemH;
            drawMenuItem(graphics, Component.translatable("gui.indexer.manager.sort_block"), menuX + 6, ty, currentSort == SortMode.BLOCK); ty += itemH;
            drawMenuItem(graphics, Component.translatable("gui.indexer.manager.sort_item"), menuX + 6, ty, currentSort == SortMode.ITEM); ty += itemH;
            drawMenuItem(graphics, Component.translatable("gui.indexer.manager.sort_mod"), menuX + 6, ty, currentSort == SortMode.MOD);
            graphics.pose().popPose();
        }
    }
    
    private void renderStats(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int statsX = this.leftPos + 176 + 6;
        int statsY = this.topPos + 8;
        Component statsTitle = Component.translatable("gui.indexer.stats");
        guiGraphics.drawString(this.font, statsTitle, statsX, statsY, 0xFFFFFF, false);
        
        int occupiedSlots = this.menu.getOccupiedSlots();
        int totalCapacity = this.menu.getTotalCapacity();
        int connectedContainers = this.menu.getConnectedContainersCount();
        int upgradeLevel = this.menu.getUpgradeLevel();
        
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
        
        statsY += 20;
        Component capacityTitle = Component.translatable("gui.indexer.capacity");
        guiGraphics.drawString(this.font, capacityTitle, statsX, statsY, 0xFFFFFF, false);
        
        statsY += 12;
        int barWidth = 88;
        int barHeight = 6;
        guiGraphics.fill(statsX, statsY, statsX + barWidth, statsY + barHeight, 0xFF333333);
        if (totalCapacity > 0) {
            float fillPercentage = (float) occupiedSlots / totalCapacity;
            int fillWidth = (int) (barWidth * fillPercentage);
            int barColor;
            if (fillPercentage < 0.5f) {
                barColor = 0xFF55FF55;
            } else if (fillPercentage < 0.8f) {
                barColor = 0xFFFFAA00;
            } else {
                barColor = 0xFFFF5555;
            }
            guiGraphics.fill(statsX, statsY, statsX + fillWidth, statsY + barHeight, barColor);
        }
        statsY += 10;
        String capacityText = occupiedSlots + " / " + totalCapacity;
        if (totalCapacity > 0) {
            int percentage = (int) ((float) occupiedSlots / totalCapacity * 100);
            capacityText += " (" + percentage + "%)";
        }
        guiGraphics.drawString(this.font, capacityText, statsX, statsY, 0xCCCCCC, false);
        
        statsY += 20;
        Component containersTitle = Component.translatable("gui.indexer.connected_containers");
        guiGraphics.drawString(this.font, containersTitle, statsX, statsY, 0xFFFFFF, false);
        
        statsY += 12;
        Component containersCount = Component.literal(String.valueOf(connectedContainers));
        guiGraphics.drawString(this.font, containersCount, statsX, statsY, 0xCCCCCC, false);
    }
    
    private net.minecraft.world.item.ItemStack getUpgradeItemForLevel(int level) {
        switch (level) {
            case 0: return new net.minecraft.world.item.ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_ZERO.get());
            case 1: return new net.minecraft.world.item.ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_BASIC.get());
            case 2: return new net.minecraft.world.item.ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_COPPER.get());
            case 3: return new net.minecraft.world.item.ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_ADVANCED.get());
            case 4: return new net.minecraft.world.item.ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_ELITE.get());
            case 5: return new net.minecraft.world.item.ItemStack(com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_DEFINITIVE.get());
            default: return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }
    
    private String formatCount(int value) {
        if (value < 1000) return String.valueOf(value);
        String suffix;
        double base;
        if (value >= 1_000_000_000) {
            suffix = "B";
            base = 1_000_000_000.0;
        } else if (value >= 1_000_000) {
            suffix = "M";
            base = 1_000_000.0;
        } else {
            suffix = "k";
            base = 1_000.0;
        }
        double v = value / base;
        DecimalFormat df = new DecimalFormat(v >= 100 ? "#0" : "#.#");
        return df.format(v) + suffix;
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // no label
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        updateSuggestion();
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
                this.searchBox.setFocused(false);
                updateSuggestion();
                return true;
            }
            if (Minecraft.getInstance().options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                return true;
            }
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void updateSuggestion() {
        if (this.searchBox == null) return;
        boolean show = !this.searchBox.isFocused() && (this.searchBox.getValue() == null || this.searchBox.getValue().isEmpty());
        this.searchBox.setSuggestion(show ? this.searchPlaceholder : "");
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= filterBtnX && mouseX <= filterBtnX + 12 && mouseY >= filterBtnY && mouseY <= filterBtnY + 12) {
            sortMenuOpen = !sortMenuOpen;
            return true;
        }
        if (sortMenuOpen) {
            int menuX = filterBtnX - 2;
            int itemH = 12;
            int itemsCount = 5;
            int menuW = 120;
            int menuH = itemH * itemsCount + 6;
            int belowY = filterBtnY + 18;
            int aboveY = filterBtnY - menuH - 2;
            int menuY = belowY;
            if (menuY + menuH > this.topPos + this.imageHeight - 8) {
                menuY = aboveY;
            }
            if (mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= menuY && mouseY <= menuY + menuH) {
                int idx = (int) ((mouseY - (menuY + 4)) / itemH);
                if (idx == 0) currentSort = SortMode.CANTIDAD;
                else if (idx == 1) currentSort = SortMode.ALFABETO;
                else if (idx == 2) currentSort = SortMode.BLOCK;
                else if (idx == 3) currentSort = SortMode.ITEM;
                else if (idx == 4) currentSort = SortMode.MOD;
                sortMenuOpen = false;
                return true;
            }
            sortMenuOpen = false;
            return true;
        }
        int areaX = this.leftPos + 8;
        int areaY = this.topPos + 24;
        int areaW = 176 - 16;
        int areaH = 60;
        int itemSpacing = 20;
        int itemsPerRow = Math.max(1, areaW / itemSpacing);
        int visibleRows = Math.max(1, areaH / itemSpacing);
        int totalRows = (int) Math.ceil(filtered.size() / (double) itemsPerRow);
        int startIndex = scrollRowOffset * itemsPerRow;
        int endIndex = Math.min(filtered.size(), startIndex + visibleRows * itemsPerRow);
        int idx = 0;
        for (int i = startIndex; i < endIndex; i++) {
            int col = idx % itemsPerRow;
            int row = idx / itemsPerRow;
            int ix = areaX + col * itemSpacing;
            int iy = areaY + row * itemSpacing;
            if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                if (isManagerInventoryFull()) {
                    failX = ix + 8;
                    failY = iy + 8;
                    failTicks = 10;
                    return true;
                }
                ItemEntry e = filtered.get(i);
                int amount = hasShiftDown() ? Math.min(64, e.count) : Math.min(1, e.count);
                requestExtract(e.id, amount);
                return true;
            }
            idx++;
        }
        if (totalRows > visibleRows) {
            int scrollbarWidth = 6;
            int scrollbarX = areaX + areaW - scrollbarWidth + 2;
            int scrollbarY = areaY;
            int scrollbarHeight = areaH;
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
                int thumbHeight = Math.max(10, (visibleRows * scrollbarHeight) / totalRows);
                int track = Math.max(1, scrollbarHeight - thumbHeight);
                int pos = (int) Math.max(0, Math.min(track, mouseY - scrollbarY - thumbHeight / 2));
                scrollRowOffset = (pos * Math.max(1, (totalRows - visibleRows))) / track;
                draggingScrollbar = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta < 0) scrollRowOffset++;
        if (delta > 0) scrollRowOffset--;
        return true;
    }
    
    private boolean isManagerInventoryFull() {
        // First 9 slots belong to the manager container
        int managerSlots = 9;
        for (int i = 0; i < managerSlots && i < this.menu.slots.size(); i++) {
            var slot = this.menu.slots.get(i);
            ItemStack s = slot.getItem();
            if (s.isEmpty()) return false;
            if (s.getCount() < s.getMaxStackSize()) return false;
        }
        return true;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!draggingScrollbar) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        int areaX = this.leftPos + 8;
        int areaY = this.topPos + 24;
        int areaW = 176 - 16;
        int areaH = 60;
        int itemSpacing = 20;
        int itemsPerRow = Math.max(1, areaW / itemSpacing);
        int visibleRows = Math.max(1, areaH / itemSpacing);
        int totalRows = (int) Math.ceil(filtered.size() / (double) itemsPerRow);
        if (totalRows <= visibleRows) return true;
        int scrollbarWidth = 6;
        int scrollbarX = areaX + areaW - scrollbarWidth + 2;
        int scrollbarY = areaY;
        int scrollbarHeight = areaH;
        if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth) {
            int thumbHeight = Math.max(10, (visibleRows * scrollbarHeight) / totalRows);
            int track = Math.max(1, scrollbarHeight - thumbHeight);
            int pos = (int) Math.max(0, Math.min(track, mouseY - scrollbarY - thumbHeight / 2));
            scrollRowOffset = (pos * Math.max(1, (totalRows - visibleRows))) / track;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public void updateItemListFromServer(Map<String, Integer> data) {
        items.clear();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            items.add(new ItemEntry(new ResourceLocation(entry.getKey()), entry.getValue()));
        }
    }

    public void requestExtract(ResourceLocation id, int count) {
        ModNetworking.sendToServer(new ExtractItemFromManagerPacket(this.menu.getBlockEntity().getBlockPos(), id, count));
    }
    
    private void sortFiltered() {
        switch (currentSort) {
            case CANTIDAD:
                filtered.sort((a, b) -> Integer.compare(b.count, a.count));
                break;
            case ALFABETO:
                filtered.sort(Comparator.comparing(this::getItemName, String.CASE_INSENSITIVE_ORDER));
                break;
            case BLOCK:
                filtered.sort(Comparator.comparingInt((ItemEntry e) -> isConstruction(e) ? 0 : 1)
                        .thenComparing((ItemEntry e) -> getItemName(e), String.CASE_INSENSITIVE_ORDER));
                break;
            case ITEM:
                filtered.sort(Comparator.comparingInt((ItemEntry e) -> isConstruction(e) ? 1 : 0)
                        .thenComparing((ItemEntry e) -> getItemName(e), String.CASE_INSENSITIVE_ORDER));
                break;
            case MOD:
                filtered.sort(Comparator.comparing((ItemEntry e) -> e.id.getNamespace(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing((ItemEntry e) -> getItemName(e), String.CASE_INSENSITIVE_ORDER));
                break;
            case NONE:
            default:
                break;
        }
    }
    
    private String getItemName(ItemEntry e) {
        Item item = ForgeRegistries.ITEMS.getValue(e.id);
        if (item == null) return e.id.toString();
        return new ItemStack(item).getHoverName().getString();
    }
    
    private boolean isConstruction(ItemEntry e) {
        Item item = ForgeRegistries.ITEMS.getValue(e.id);
        return item instanceof BlockItem;
    }
    
    private boolean isTool(ItemEntry e) {
        Item item = ForgeRegistries.ITEMS.getValue(e.id);
        if (item == null) return false;
        return item instanceof TieredItem || item instanceof SwordItem || item instanceof ShearsItem;
    }
    
    private void drawMenuItem(GuiGraphics g, Component text, int x, int y, boolean selected) {
        if (selected) {
            g.fill(x - 4, y - 2, x + this.font.width(text) + 4, y + 10, 0x402e5d70);
        }
        g.drawString(this.font, text, x, y, 0xFFFFFF, false);
    }

    public static class ItemEntry {
        public final ResourceLocation id;
        public final int count;
        public ItemEntry(ResourceLocation id, int count) {
            this.id = id;
            this.count = count;
        }
    }
    
    private enum SortMode {
        NONE, CANTIDAD, ALFABETO, BLOCK, ITEM, MOD
    }
}
