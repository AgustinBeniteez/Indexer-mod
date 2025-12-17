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
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID,
            "textures/gui/indexer_manager.png");
    private static final ResourceLocation FILTER_CUANTITY_ICON = ResourceLocation
            .fromNamespaceAndPath(IndexerMod.MOD_ID, "textures/gui/filter_cuantity.png");
    private static final ResourceLocation FILTER_AZ_ICON = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID,
            "textures/gui/filter_az.png");
    private static final ResourceLocation FILTER_BLOCK_ICON = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID,
            "textures/gui/filter_block.png");
    private static final ResourceLocation FILTER_ITEM_ICON = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID,
            "textures/gui/filter_item.png");
    private static final ResourceLocation FILTER_MOD_ICON = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID,
            "textures/gui/filter_mods.png");
    private static final ResourceLocation CONTROL_STACK_ICON = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID,
            "textures/gui/controllstack.png");
    private static final ResourceLocation CONTROL_CLICK_ICON = ResourceLocation.fromNamespaceAndPath(IndexerMod.MOD_ID,
            "textures/gui/controllclick.png");
    private static final ResourceLocation CONTROL_CLICK_RIGHT_ICON = ResourceLocation
            .fromNamespaceAndPath(IndexerMod.MOD_ID, "textures/gui/controllclickright.png");
    private static final int STATS_PANEL_WIDTH = 100;
    private EditBox searchBox;
    private final List<ItemVariantEntry> items = new ArrayList<>();
    private final List<ItemVariantEntry> filtered = new ArrayList<>();
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
        int panelBottom = this.topPos + (int) (this.imageHeight * 0.8f);
        graphics.fill(statsX, this.topPos, statsX + STATS_PANEL_WIDTH, panelBottom, 0xC0101010);
        renderStats(graphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        boolean isConnected = this.menu.isControllerConnected();
        if (this.searchBox != null) {
            this.searchBox.setVisible(isConnected);
        }

        if (!isConnected) {
            filtered.clear();
            int areaX = this.leftPos + 8;
            int areaW = 176 - 16;
            int areaY = this.topPos + 24;
            Component errorText = Component.translatable("gui.indexer.manager.controller_not_found");
            int textWidth = this.font.width(errorText);
            int tx = areaX + (areaW - textWidth) / 2;
            int ty = areaY + 20;
            graphics.drawString(this.font, errorText, tx, ty, 0xFF5555, false);
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        String query = searchBox.getValue() == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT);
        filtered.clear();
        for (ItemVariantEntry e : items) {
            String name = Component.translatable(e.stack.getItem().getDescriptionId()).getString()
                    .toLowerCase(Locale.ROOT);
            String idStr = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.stack.getItem()).toString();
            String enchTokens = getEnchantSearchTokens(e.stack);
            if (query.isEmpty() || idStr.toLowerCase(Locale.ROOT).contains(query) || name.contains(query)
                    || enchTokens.contains(query)) {
                filtered.add(e);
            }
        }
        sortFiltered();
        int areaX = this.leftPos + 8;
        int areaY = this.topPos + 24;
        int areaW = 176 - 16;

        if (filtered.isEmpty()) {
            Component emptyText = Component.translatable("gui.indexer.controller.container_empty");
            int textWidth = this.font.width(emptyText);
            int tx = areaX + (areaW - textWidth) / 2;
            int ty = areaY + 5;
            graphics.drawString(this.font, emptyText, tx, ty, 0xFFFFFF, false);
        }

        int areaH = 60;
        int itemSpacing = 20;
        int itemsPerRow = Math.max(1, areaW / itemSpacing);
        int visibleRows = Math.max(1, areaH / itemSpacing);
        int totalRows = (int) Math.ceil(filtered.size() / (double) itemsPerRow);
        if (scrollRowOffset < 0)
            scrollRowOffset = 0;
        int maxRowOffset = Math.max(0, totalRows - visibleRows);
        if (scrollRowOffset > maxRowOffset)
            scrollRowOffset = maxRowOffset;
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
        List<Component> tooltipToRender = null;
        for (int i = startIndex; i < endIndex; i++) {
            ItemVariantEntry e = filtered.get(i);
            int col = idx % itemsPerRow;
            int row = idx / itemsPerRow;
            int ix = areaX + col * itemSpacing;
            int iy = areaY + row * itemSpacing;
            ItemStack stack = e.stack.copy();
            graphics.renderItem(stack, ix, iy);
            if (e.pending && !overMenu) {
                long gt = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime()
                        : System.currentTimeMillis() / 50L;
                int animPeriod = 20;
                int offset = (int) ((gt % animPeriod) * (16.0 / animPeriod));
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 190);
                graphics.fill(ix, iy, ix + 16, iy + 16, 0x40202020);
                int fillHeight = Math.min(16, offset);
                graphics.fill(ix + 1, iy + 15 - fillHeight, ix + 15, iy + 15, 0x8033AAFF);
                graphics.pose().popPose();
            }
            String countStr = formatCount(e.count);
            graphics.renderItemDecorations(this.font, stack, ix, iy, countStr);
            if (!overMenu && mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                tooltipToRender = new ArrayList<>();
                boolean full = isManagerInventoryFull();
                if (!stack.isEmpty()) {
                    MutableComponent name = Component
                            .literal(Component.translatable(stack.getItem().getDescriptionId()).getString());
                    tooltipToRender.add(full ? name.withStyle(ChatFormatting.RED) : name);
                }
                tooltipToRender.add(Component.literal("x" + e.count));

                // Normal Enchantments
                var enchantments = stack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
                if (enchantments != null) {
                    for (var entry : enchantments.entrySet()) {
                        var holder = entry.getKey();
                        int lvl = entry.getIntValue();
                        String nameTxt = holder.value().description().getString();
                        String lvlTxt = Component.translatable("enchantment.level." + lvl).getString();
                        tooltipToRender.add(Component.literal(nameTxt + " " + lvlTxt));
                    }
                }

                // Stored Enchantments
                var storedEnchantments = stack.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
                if (storedEnchantments != null) {
                    for (var entry : storedEnchantments.entrySet()) {
                        var holder = entry.getKey();
                        int lvl = entry.getIntValue();
                        String nameTxt = holder.value().description().getString();
                        String lvlTxt = Component.translatable("enchantment.level." + lvl).getString();
                        tooltipToRender.add(Component.literal(nameTxt + " " + lvlTxt));
                    }
                }

                // Container Contents
                var container = stack.get(net.minecraft.core.component.DataComponents.CONTAINER);
                if (container != null) {
                    var items = container.stream().toList();
                    if (!items.isEmpty()) {
                        tooltipToRender.add(Component.literal("Contents:").withStyle(ChatFormatting.GRAY));
                        int limit = 5;
                        int countShown = 0;
                        for (var itemStack : items) {
                            if (itemStack.isEmpty())
                                continue;
                            if (countShown >= limit) {
                                tooltipToRender
                                        .add(Component.literal("... and " + (items.size() - limit) + " more")
                                                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
                                break;
                            }

                            MutableComponent line = Component.literal("- ");
                            line.append(Component.translatable(itemStack.getDescriptionId()))
                                    .append(" x" + itemStack.getCount());

                            tooltipToRender.add(line.withStyle(ChatFormatting.GRAY));
                            countShown++;
                        }
                    }
                }
            }
            idx++;
        }
        if (totalRows > visibleRows) {
            int scrollbarWidth = 6;
            int scrollbarX = areaX + areaW - scrollbarWidth + 2;
            int scrollbarY = areaY;
            int scrollbarHeight = areaH;
            // Fondo del scrollbar: #171717
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight,
                    0xFF171717);
            int thumbHeight = Math.max(10, (visibleRows * scrollbarHeight) / totalRows);
            int thumbY = scrollbarY
                    + (scrollRowOffset * (scrollbarHeight - thumbHeight)) / Math.max(1, (totalRows - visibleRows));
            // Barra del scrollbar: #2e5d70
            graphics.fill(scrollbarX + 1, thumbY, scrollbarX + scrollbarWidth - 1, thumbY + thumbHeight, 0xFF2e5d70);
        }
        if (failTicks > 0 && failX >= 0 && failY >= 0) {
            graphics.drawCenteredString(this.font, "X", failX, failY, 0xFF0000);
            failTicks--;
        }

        ResourceLocation iconToRender;
        switch (currentSort) {
            case ALFABETO:
                iconToRender = FILTER_AZ_ICON;
                break;
            case BLOCK:
                iconToRender = FILTER_BLOCK_ICON;
                break;
            case ITEM:
                iconToRender = FILTER_ITEM_ICON;
                break;
            case MOD:
                iconToRender = FILTER_MOD_ICON;
                break;
            case CANTIDAD:
            case NONE:
            default:
                iconToRender = FILTER_CUANTITY_ICON;
                break;
        }
        graphics.blit(iconToRender, filterBtnX, filterBtnY, 0, 0, 16, 16, 16, 16);

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
            graphics.fill(menuX, menuY, menuX + menuW, menuY + menuH, 0xF2202020);
            graphics.fill(menuX, menuY, menuX + menuW, menuY + 1, 0xFF2e5d70);
            graphics.fill(menuX, menuY + menuH - 1, menuX + menuW, menuY + menuH, 0xFF2e5d70);
            int ty = menuY + 4;
            boolean hoverCantidad = mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= ty && mouseY <= ty + itemH;
            drawMenuItem(graphics, Component.translatable("gui.indexer.manager.sort_cantidad"), menuX + 6, ty,
                    currentSort == SortMode.CANTIDAD, hoverCantidad);
            ty += itemH;
            boolean hoverAlfabeto = mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= ty && mouseY <= ty + itemH;
            drawMenuItem(graphics, Component.translatable("gui.indexer.manager.sort_alfabeto"), menuX + 6, ty,
                    currentSort == SortMode.ALFABETO, hoverAlfabeto);
            ty += itemH;
            boolean hoverBlock = mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= ty && mouseY <= ty + itemH;
            drawMenuItem(graphics, Component.translatable("gui.indexer.manager.sort_block"), menuX + 6, ty,
                    currentSort == SortMode.BLOCK, hoverBlock);
            ty += itemH;
            boolean hoverItem = mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= ty && mouseY <= ty + itemH;
            drawMenuItem(graphics, Component.translatable("gui.indexer.manager.sort_item"), menuX + 6, ty,
                    currentSort == SortMode.ITEM, hoverItem);
            ty += itemH;
            boolean hoverMod = mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= ty && mouseY <= ty + itemH;
            drawMenuItem(graphics, Component.translatable("gui.indexer.manager.sort_mod"), menuX + 6, ty,
                    currentSort == SortMode.MOD, hoverMod);
            graphics.pose().popPose();
        }

        if (tooltipToRender != null) {
            graphics.renderComponentTooltip(this.font, tooltipToRender, mouseX, mouseY);
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderStats(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int statsX = this.leftPos + 176 + 6;
        int statsY = this.topPos + 8;
        Component statsTitle = Component.translatable("gui.indexer.stats");
        guiGraphics.drawString(this.font, statsTitle, statsX, statsY, 0xFFFFFF, false);

        if (!this.menu.isControllerConnected()) {
            statsY += 20;
            Component errorText = Component.translatable("gui.indexer.manager.controller_not_found");
            guiGraphics.drawWordWrap(this.font, errorText, statsX, statsY, STATS_PANEL_WIDTH - 12, 0xFF5555);
            return;
        }

        int occupiedSlots = this.menu.getOccupiedSlots();
        int totalCapacity = this.menu.getTotalCapacity();
        int connectedContainers = this.menu.getConnectedContainersCount();
        int upgradeLevel = this.menu.getUpgradeLevel();

        statsY += 15;
        String speedMultiplier;
        switch (upgradeLevel) {
            case 1:
                speedMultiplier = "x5";
                break;
            case 2:
                speedMultiplier = "x10";
                break;
            case 3:
                speedMultiplier = "x20";
                break;
            case 4:
                speedMultiplier = "x64";
                break;
            case 5:
                speedMultiplier = "x256";
                break;
            default:
                speedMultiplier = "x1";
                break;
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

        statsY += 20;
        float scale = 0.65f;
        int clickW = 16;
        int clickH = 16;
        int stackW = 32;
        int stackH = 16;
        guiGraphics.blit(CONTROL_STACK_ICON, statsX, statsY, 0, 0, stackW, stackH, 32, 16);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1f);
        guiGraphics.drawString(this.font, Component.translatable("gui.indexer.control.move_stack"),
                (int) ((statsX + stackW + 6) / scale), (int) ((statsY + 2) / scale), 0xFFFFFF, false);
        guiGraphics.pose().popPose();
        statsY += stackH + 6;
        guiGraphics.blit(CONTROL_CLICK_ICON, statsX, statsY, 0, 0, clickW, clickH, 16, 16);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1f);
        guiGraphics.drawString(this.font, Component.translatable("gui.indexer.control.move_one"),
                (int) ((statsX + clickW + 6) / scale), (int) ((statsY + 2) / scale), 0xFFFFFF, false);
        guiGraphics.pose().popPose();
        statsY += clickH + 6;
        guiGraphics.blit(CONTROL_CLICK_RIGHT_ICON, statsX, statsY, 0, 0, clickW, clickH, 16, 16);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1f);
        guiGraphics.drawString(this.font, Component.translatable("gui.indexer.control.cancel_stack"),
                (int) ((statsX + clickW + 6) / scale), (int) ((statsY + 2) / scale), 0xFFFFFF, false);
        guiGraphics.pose().popPose();
    }

    private net.minecraft.world.item.ItemStack getUpgradeItemForLevel(int level) {
        switch (level) {
            case 0:
                return new net.minecraft.world.item.ItemStack(
                        com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_ZERO.get());
            case 1:
                return new net.minecraft.world.item.ItemStack(
                        com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_BASIC.get());
            case 2:
                return new net.minecraft.world.item.ItemStack(
                        com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_COPPER.get());
            case 3:
                return new net.minecraft.world.item.ItemStack(
                        com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_ADVANCED.get());
            case 4:
                return new net.minecraft.world.item.ItemStack(
                        com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_ELITE.get());
            case 5:
                return new net.minecraft.world.item.ItemStack(
                        com.agustinbenitez.indexer.init.ModItems.TRANSFER_SPEED_UPGRADE_DEFINITIVE.get());
            default:
                return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    private String formatCount(int value) {
        if (value < 1000)
            return String.valueOf(value);
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
            if (Minecraft.getInstance().options.keyInventory
                    .isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                return true;
            }
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void updateSuggestion() {
        if (this.searchBox == null)
            return;
        boolean show = !this.searchBox.isFocused()
                && (this.searchBox.getValue() == null || this.searchBox.getValue().isEmpty());
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
                if (idx == 0)
                    currentSort = SortMode.CANTIDAD;
                else if (idx == 1)
                    currentSort = SortMode.ALFABETO;
                else if (idx == 2)
                    currentSort = SortMode.BLOCK;
                else if (idx == 3)
                    currentSort = SortMode.ITEM;
                else if (idx == 4)
                    currentSort = SortMode.MOD;
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
                ItemVariantEntry e = filtered.get(i);
                if (e.pending) {
                    ModNetworking.sendToServer(new com.agustinbenitez.indexer.network.CancelExtractionFromManagerPacket(
                            this.menu.getBlockEntity().getBlockPos(), e.stack));
                    return true;
                }
                if (isManagerInventoryFull()) {
                    failX = ix + 8;
                    failY = iy + 8;
                    failTicks = 10;
                    return true;
                }
                int amount = hasShiftDown() ? Math.min(64, e.count) : Math.min(1, e.count);
                requestExtract(e.stack, amount);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double delta = scrollY;
        if (delta < 0)
            scrollRowOffset++;
        if (delta > 0)
            scrollRowOffset--;
        return true;
    }

    private boolean isManagerInventoryFull() {
        // First 9 slots belong to the manager container
        int managerSlots = 9;
        for (int i = 0; i < managerSlots && i < this.menu.slots.size(); i++) {
            var slot = this.menu.slots.get(i);
            ItemStack s = slot.getItem();
            if (s.isEmpty())
                return false;
            if (s.getCount() < s.getMaxStackSize())
                return false;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!draggingScrollbar)
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        int areaX = this.leftPos + 8;
        int areaY = this.topPos + 24;
        int areaW = 176 - 16;
        int areaH = 60;
        int itemSpacing = 20;
        int itemsPerRow = Math.max(1, areaW / itemSpacing);
        int visibleRows = Math.max(1, areaH / itemSpacing);
        int totalRows = (int) Math.ceil(filtered.size() / (double) itemsPerRow);
        if (totalRows <= visibleRows)
            return true;
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

    public void updateItemListFromServer(
            java.util.List<com.agustinbenitez.indexer.network.ManagerItemsUpdatePacket.Entry> data) {
        items.clear();
        for (var entry : data) {
            items.add(new ItemVariantEntry(entry.stackVariant().copy(), entry.count(), entry.pending()));
        }
    }

    public void requestExtract(ItemStack stackVariant, int count) {
        ModNetworking.sendToServer(new ExtractItemFromManagerPacket(this.menu.getBlockEntity().getBlockPos(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stackVariant.getItem()), count,
                stackVariant));
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
                filtered.sort(Comparator.comparingInt((ItemVariantEntry e) -> isConstruction(e) ? 0 : 1)
                        .thenComparing((ItemVariantEntry e) -> getItemName(e), String.CASE_INSENSITIVE_ORDER));
                break;
            case ITEM:
                filtered.sort(Comparator.comparingInt((ItemVariantEntry e) -> isConstruction(e) ? 1 : 0)
                        .thenComparing((ItemVariantEntry e) -> getItemName(e), String.CASE_INSENSITIVE_ORDER));
                break;
            case MOD:
                filtered.sort(Comparator.comparing((ItemVariantEntry e) -> {
                    var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.stack.getItem());
                    return key == null ? "" : key.getNamespace();
                }, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing((ItemVariantEntry e) -> getItemName(e), String.CASE_INSENSITIVE_ORDER));
                break;
            case NONE:
            default:
                break;
        }
    }

    private String getItemName(ItemVariantEntry e) {
        return Component.translatable(e.stack.getItem().getDescriptionId()).getString();
    }

    private String getEnchantSearchTokens(ItemStack stack) {
        StringBuilder sb = new StringBuilder();

        // Normal Enchantments
        var enchantments = stack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
        if (enchantments != null) {
            for (var entry : enchantments.entrySet()) {
                var holder = entry.getKey();
                int lvl = entry.getIntValue();

                sb.append(holder.unwrapKey().map(k -> k.location().toString()).orElse("").toLowerCase(Locale.ROOT))
                        .append(" ");
                sb.append(holder.value().description().getString().toLowerCase(Locale.ROOT)).append(" ");
                sb.append(Component.translatable("enchantment.level." + lvl).getString().toLowerCase(Locale.ROOT))
                        .append(" ");
            }
        }

        // Stored Enchantments
        var storedEnchantments = stack.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
        if (storedEnchantments != null) {
            for (var entry : storedEnchantments.entrySet()) {
                var holder = entry.getKey();
                int lvl = entry.getIntValue();

                sb.append(holder.unwrapKey().map(k -> k.location().toString()).orElse("").toLowerCase(Locale.ROOT))
                        .append(" ");
                sb.append(holder.value().description().getString().toLowerCase(Locale.ROOT)).append(" ");
                sb.append(Component.translatable("enchantment.level." + lvl).getString().toLowerCase(Locale.ROOT))
                        .append(" ");
            }
        }

        return sb.toString();
    }

    private boolean isConstruction(ItemVariantEntry e) {
        return e.stack.getItem() instanceof BlockItem;
    }

    private boolean isTool(ItemVariantEntry e) {
        Item item = e.stack.getItem();
        return item instanceof TieredItem || item instanceof SwordItem || item instanceof ShearsItem;
    }

    private void drawMenuItem(GuiGraphics g, Component text, int x, int y, boolean selected, boolean hovered) {
        if (selected) {
            g.fill(x - 4, y - 2, x + this.font.width(text) + 4, y + 10, 0x402e5d70);
        }
        int color = hovered ? 0xCCCCCC : 0xFFFFFF;
        g.drawString(this.font, text, x, y, color, false);
    }

    public static class ItemVariantEntry {
        public final ItemStack stack;
        public final int count;
        public final boolean pending;

        public ItemVariantEntry(ItemStack stack, int count, boolean pending) {
            this.stack = stack;
            this.count = count;
            this.pending = pending;
        }
    }

    private enum SortMode {
        NONE, CANTIDAD, ALFABETO, BLOCK, ITEM, MOD
    }
}
