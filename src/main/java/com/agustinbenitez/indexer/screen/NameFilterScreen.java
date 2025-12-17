package com.agustinbenitez.indexer.screen;

import com.agustinbenitez.indexer.init.ModDataComponents;
import com.agustinbenitez.indexer.network.NameFilterPacket;
import com.agustinbenitez.indexer.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class NameFilterScreen extends Screen {
    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 200;

    private EditBox nameEditBox;
    private Button confirmButton;
    private Button cancelButton;
    private final ItemStack filterItem;
    private final int slotIndex;

    public NameFilterScreen(ItemStack filterItem, int slotIndex) {
        super(Component.translatable("gui.indexer.name_filter.title"));
        this.filterItem = filterItem;
        this.slotIndex = slotIndex;
    }

    @Override
    protected void init() {
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        // Campo de texto para el nombre personalizado
        this.nameEditBox = new EditBox(this.font, guiLeft + 20, guiTop + 60, GUI_WIDTH - 40, 20,
                Component.translatable("gui.indexer.name_filter.name_input"));

        // Configurar el campo de texto para mejor visibilidad (igual que otros filtros)
        this.nameEditBox.setBordered(true);
        this.nameEditBox.setVisible(true);
        this.nameEditBox.setEditable(true);
        this.nameEditBox.setTextColor(0xFFFFFF); // Texto blanco
        this.nameEditBox.setTextColorUneditable(0xA0A0A0);
        this.nameEditBox.setMaxLength(50);

        this.addRenderableWidget(this.nameEditBox);
        this.setInitialFocus(this.nameEditBox);

        // Botón de confirmar (igual posición que otros filtros)
        this.confirmButton = Button.builder(Component.translatable("gui.indexer.name_filter.confirm"),
                button -> this.confirmName())
                .bounds(guiLeft + 20, guiTop + 150, 80, 20)
                .build();
        this.addRenderableWidget(this.confirmButton);

        // Botón de cancelar (igual posición que otros filtros)
        this.cancelButton = Button.builder(Component.translatable("gui.indexer.name_filter.cancel"),
                button -> this.onClose())
                .bounds(guiLeft + 120, guiTop + 150, 80, 20)
                .build();
        this.addRenderableWidget(this.cancelButton);

        // Configurar el responder DESPUÉS de crear los botones
        this.nameEditBox.setResponder(this::onNameChanged);

        // Cargar el nombre actual si existe
        if (this.filterItem.has(ModDataComponents.CUSTOM_NAME.get())) {
            String currentName = this.filterItem.get(ModDataComponents.CUSTOM_NAME.get());
            this.nameEditBox.setValue(currentName);
        }

        // Actualizar estado del botón confirmar
        this.updateConfirmButton();
    }

    private void onNameChanged(String text) {
        this.updateConfirmButton();
    }

    private void updateConfirmButton() {
        // Verificar que el botón existe antes de intentar modificarlo
        if (this.confirmButton != null) {
            // Habilitar el botón solo si hay texto
            this.confirmButton.active = !this.nameEditBox.getValue().trim().isEmpty();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        // Fondo de la GUI (igual que otros filtros)
        guiGraphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xC0101010);
        guiGraphics.fill(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, 0xFF2D2D30);

        // Título (centrado como otros filtros)
        Component title = Component.translatable("gui.indexer.name_filter.title");
        guiGraphics.drawCenteredString(this.font, title, guiLeft + GUI_WIDTH / 2, guiTop + 20, 0xFFFFFF);

        // Instrucciones (igual que otros filtros)
        Component instructions = Component.translatable("gui.indexer.name_filter.instructions");
        guiGraphics.drawString(this.font, instructions, guiLeft + 20, guiTop + 40, 0xCCCCCC, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void confirmName() {
        String customName = this.nameEditBox.getValue().trim();
        if (!customName.isEmpty()) {
            // Enviar el paquete al servidor
            ModNetworking.sendToServer(new NameFilterPacket(this.slotIndex, customName));
        }
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}