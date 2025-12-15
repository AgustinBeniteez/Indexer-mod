package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.menu.IndexerManagerMenu;
import com.agustinbenitez.indexer.network.ModNetworking;
import com.agustinbenitez.indexer.network.RequestManagerItemsPacket;
import com.agustinbenitez.indexer.network.ExtractItemFromManagerPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.player.Inventory;

import java.util.*;
import java.text.DecimalFormat;

public class IndexerManagerScreen extends AbstractContainerScreen<IndexerManagerMenu> {
    private EditBox searchBox;
    private final List<ItemEntry> items = new ArrayList<>();
    private final List<ItemEntry> filtered = new ArrayList<>();
    private int scrollRowOffset = 0;

    public IndexerManagerScreen(IndexerManagerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        int x = this.leftPos + 8;
        int y = this.topPos + 6;
        this.searchBox = new EditBox(this.font, x, y, 120, 12, Component.literal(""));
        this.searchBox.setSuggestion("Buscar");
        this.searchBox.setResponder(s -> {});
        this.addRenderableWidget(this.searchBox);
        int requestX = this.leftPos + 132;
        int requestY = this.topPos + 6;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.indexer.refresh"), b -> {
            ModNetworking.sendToServer(new RequestManagerItemsPacket(this.menu.getBlockEntity().getBlockPos()));
        }).bounds(requestX, requestY, 36, 12).build());
        ModNetworking.sendToServer(new RequestManagerItemsPacket(this.menu.getBlockEntity().getBlockPos()));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
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
        int areaX = this.leftPos + 8;
        int areaY = this.topPos + 24;
        int areaW = this.imageWidth - 16;
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
            if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                List<Component> tooltip = new ArrayList<>();
                if (!stack.isEmpty()) tooltip.add(stack.getHoverName());
                tooltip.add(Component.literal("x" + e.count));
                graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
            idx++;
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
        graphics.drawString(this.font, "Your Inventory", 8, 94, 0x404040, false);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int areaX = this.leftPos + 8;
        int areaY = this.topPos + 24;
        int areaW = this.imageWidth - 16;
        int areaH = 60;
        int itemSpacing = 20;
        int itemsPerRow = Math.max(1, areaW / itemSpacing);
        int visibleRows = Math.max(1, areaH / itemSpacing);
        int startIndex = scrollRowOffset * itemsPerRow;
        int endIndex = Math.min(filtered.size(), startIndex + visibleRows * itemsPerRow);
        int idx = 0;
        for (int i = startIndex; i < endIndex; i++) {
            int col = idx % itemsPerRow;
            int row = idx / itemsPerRow;
            int ix = areaX + col * itemSpacing;
            int iy = areaY + row * itemSpacing;
            if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                ItemEntry e = filtered.get(i);
                int amount = Math.min(64, e.count);
                requestExtract(e.id, amount);
                return true;
            }
            idx++;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta < 0) scrollRowOffset++;
        if (delta > 0) scrollRowOffset--;
        return true;
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

    public static class ItemEntry {
        public final ResourceLocation id;
        public final int count;
        public ItemEntry(ResourceLocation id, int count) {
            this.id = id;
            this.count = count;
        }
    }
}
