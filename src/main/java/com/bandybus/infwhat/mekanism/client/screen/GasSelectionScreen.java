package com.bandybus.infwhat.mekanism.client.screen;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.entity.ChemicalTapTileEntity;
import com.bandybus.infwhat.mekanism.block.entity.GasTapBlockEntity;
import com.bandybus.infwhat.mekanism.network.MekanismAdditionNetwork;
import mekanism.api.chemical.Chemical;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Secondary screen opened from the chemical selector box. Gases and pigments are listed together;
 * pressing a row sends the selection to the server and returns to the generator screen.
 *
 * <p>Dangerous gases are marked red. Without the containment upgrade they cannot be selected. With
 * the upgrade installed they require an explicit confirmation before the packet is sent.
 */
public class GasSelectionScreen extends Screen {

    private static final int PREFERRED_WIDTH = 200;
    private static final int PREFERRED_HEIGHT = 220;
    private static final int MIN_HEIGHT = 140;
    private static final int HEADER_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 26;
    private static final int ENTRY_HEIGHT = 18;
    private static final int BACK_BUTTON_WIDTH = 60;
    private static final int FOOTER_BUTTON_WIDTH = 50;
    private static final int FOOTER_BUTTON_HEIGHT = 16;
    private static final int FOOTER_BUTTON_GAP = 4;

    private static final int BUTTON_OFF = 0xFF303B42;
    private static final int BUTTON_ON = 0xFF256B43;
    private static final int BUTTON_HOVER = 0xFF456C83;
    private static final int BUTTON_BORDER = 0xFF070B0E;
    private static final int BUTTON_TEXT = 0xFFF0F5F7;
    private static final int BUTTON_DANGER = 0xFF5A2B2B;
    private static final int BUTTON_LOCKED = 0xFF3A2A2A;
    private static final int COLOR_TITLE = 0xFFE9F1F4;
    private static final int COLOR_TEXT = 0xFF9FB8C2;
    private static final int COLOR_DANGER = 0xFFE07A7A;
    private static final int PANEL_BORDER = 0xFF080D10;
    private static final int PANEL_INNER = 0xFF1D272C;
    private static final int PANEL_HEADER = 0xFF243238;

    private final GasTapScreen parent;
    private final GasTapBlockEntity tile;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    @Nullable
    private ChemicalListWidget list;
    private Button backButton;
    @Nullable
    private Chemical pendingChemical;

    public GasSelectionScreen(GasTapScreen parent) {
        super(Component.translatable("gui." + InfinityWhatMekanism.MOD_ID + ".gas.select_title"));
        this.parent = parent;
        this.tile = parent.getGasTap();
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(PREFERRED_WIDTH, this.width - 32);
        this.panelHeight = Math.min(PREFERRED_HEIGHT, Math.max(MIN_HEIGHT, this.height - 32));
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;

        int listTop = this.panelTop + HEADER_HEIGHT;
        int listHeight = Math.max(ENTRY_HEIGHT,
                this.panelTop + this.panelHeight - FOOTER_HEIGHT - listTop);
        this.list = new ChemicalListWidget(this.minecraft, this.panelWidth, listHeight,
                listTop, ENTRY_HEIGHT);
        this.list.setX(this.panelLeft);
        this.list.setY(listTop);

        List<Chemical> chemicals = new ArrayList<>(this.tile.getAvailableChemicals());
        chemicals.sort(Comparator.comparing(
                chemical -> Component.translatable(chemical.getTranslationKey()).getString(),
                String.CASE_INSENSITIVE_ORDER));
        for (Chemical chemical : chemicals) {
            this.list.addChemical(chemical);
        }
        this.addWidget(this.list);

        this.backButton = Button.builder(
                        Component.translatable("gui." + InfinityWhatMekanism.MOD_ID + ".gas.back"),
                        button -> this.onClose())
                .bounds(this.panelLeft + (this.panelWidth - BACK_BUTTON_WIDTH) / 2,
                        this.panelTop + this.panelHeight - 20, BACK_BUTTON_WIDTH, 16)
                .build();
        this.addRenderableWidget(this.backButton);
        this.updateBackButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(this.panelLeft - 1, this.panelTop - 1,
                this.panelLeft + this.panelWidth + 1, this.panelTop + this.panelHeight + 1,
                PANEL_BORDER);
        graphics.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth,
                this.panelTop + this.panelHeight, PANEL_INNER);
        graphics.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth,
                this.panelTop + 18, PANEL_HEADER);
        graphics.drawString(this.font, this.title, this.panelLeft + 8, this.panelTop + 5,
                COLOR_TITLE, false);

        if (this.list != null) {
            if (this.list.isEmpty()) {
                graphics.drawString(this.font, Component.translatable(
                                "gui." + InfinityWhatMekanism.MOD_ID + ".gas.no_gases"),
                        this.panelLeft + 10, this.panelTop + HEADER_HEIGHT + 6, COLOR_TEXT,
                        false);
            } else {
                this.list.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        if (this.pendingChemical != null) {
            this.renderConfirmBar(graphics, mouseX, mouseY);
        } else if (this.backButton != null) {
            this.backButton.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderConfirmBar(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = this.footerButtonY();
        int confirmX = this.confirmButtonX();
        int cancelX = this.cancelButtonX();
        drawFooterButton(graphics, confirmX, y, FOOTER_BUTTON_WIDTH, FOOTER_BUTTON_HEIGHT,
                Component.translatable("gui." + InfinityWhatMekanism.MOD_ID + ".gas.confirm"),
                BUTTON_ON, isHovering(confirmX, y, FOOTER_BUTTON_WIDTH, FOOTER_BUTTON_HEIGHT,
                        mouseX, mouseY));
        drawFooterButton(graphics, cancelX, y, FOOTER_BUTTON_WIDTH, FOOTER_BUTTON_HEIGHT,
                Component.translatable("gui." + InfinityWhatMekanism.MOD_ID + ".gas.cancel"),
                BUTTON_OFF, isHovering(cancelX, y, FOOTER_BUTTON_WIDTH, FOOTER_BUTTON_HEIGHT,
                        mouseX, mouseY));
    }

    private void drawFooterButton(GuiGraphics graphics, int x, int y, int width, int height,
                                  Component label, int fill, boolean hovered) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, BUTTON_BORDER);
        graphics.fill(x, y, x + width, y + height, hovered ? BUTTON_HOVER : fill);
        String text = label.getString();
        if (this.font.width(text) > width - 4) {
            text = this.font.plainSubstrByWidth(text, width - 4 - this.font.width("..."))
                    + "...";
        }
        graphics.drawCenteredString(this.font, text, x + width / 2,
                y + (height - 8) / 2, BUTTON_TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.pendingChemical != null) {
            int y = this.footerButtonY();
            int confirmX = this.confirmButtonX();
            int cancelX = this.cancelButtonX();
            if (isInside(mouseX, mouseY, confirmX, y, FOOTER_BUTTON_WIDTH,
                    FOOTER_BUTTON_HEIGHT)) {
                this.confirmPending();
                return true;
            }
            if (isInside(mouseX, mouseY, cancelX, y, FOOTER_BUTTON_WIDTH,
                    FOOTER_BUTTON_HEIGHT)) {
                this.cancelPending();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.pendingChemical != null) {
                this.cancelPending();
                return true;
            }
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void handleChemicalSelection(Chemical chemical) {
        boolean dangerous = GasTapBlockEntity.isDangerousChemical(chemical);
        if (dangerous && !this.tile.hasContainmentUpgrade()) {
            this.pendingChemical = null;
            this.updateBackButton();
            this.minecraft.player.displayClientMessage(Component.translatable(
                            "message." + InfinityWhatMekanism.MOD_ID + ".upgrade.required")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        playButtonSound();
        if (dangerous) {
            this.pendingChemical = chemical;
            this.updateBackButton();
            return;
        }
        this.pendingChemical = null;
        this.updateBackButton();
        this.sendAndClose(chemical);
    }

    private void confirmPending() {
        Chemical chemical = this.pendingChemical;
        if (chemical == null) {
            return;
        }
        this.pendingChemical = null;
        this.updateBackButton();
        playButtonSound();
        this.sendAndClose(chemical);
    }

    private void cancelPending() {
        this.pendingChemical = null;
        this.updateBackButton();
        playButtonSound();
    }

    private void sendAndClose(Chemical chemical) {
        ResourceLocation id = ChemicalTapTileEntity.idOf(chemical);
        if (id == null) {
            return;
        }
        MekanismAdditionNetwork.sendChemical(this.parent.getContainerId(), id);
        this.minecraft.setScreen(this.parent);
    }

    private void updateBackButton() {
        if (this.backButton != null) {
            boolean show = this.pendingChemical == null;
            this.backButton.visible = show;
            this.backButton.active = show;
        }
    }

    private void playButtonSound() {
        this.minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private boolean isSelected(Chemical chemical) {
        Chemical selected = this.tile.getSelectedChemical();
        if (selected != null) {
            return selected == chemical;
        }
        ResourceLocation id = ChemicalTapTileEntity.idOf(chemical);
        return id != null && id.toString().equals(this.tile.getSelectedChemicalId());
    }

    private int footerButtonY() {
        return this.panelTop + this.panelHeight - 20;
    }

    private int cancelButtonX() {
        return this.panelLeft + this.panelWidth - 8 - FOOTER_BUTTON_WIDTH;
    }

    private int confirmButtonX() {
        return this.cancelButtonX() - FOOTER_BUTTON_WIDTH - FOOTER_BUTTON_GAP;
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return isInside(mouseX, mouseY, x, y, width, height);
    }

    private static boolean isInside(double x, double y, int left, int top, int width,
                                    int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    /** The list is a separate subclass so it can use the protected addEntry/getItemCount API. */
    private final class ChemicalListWidget extends ObjectSelectionList<ChemicalEntry> {

        private ChemicalListWidget(Minecraft minecraft, int width, int height, int top,
                                   int itemHeight) {
            super(minecraft, width, height, top, itemHeight);
        }

        private void addChemical(Chemical chemical) {
            this.addEntry(new ChemicalEntry(chemical));
        }

        private boolean isEmpty() {
            return this.getItemCount() == 0;
        }

        @Override
        public int getRowWidth() {
            return GasSelectionScreen.this.panelWidth - 24;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRight() - 8;
        }
    }

    /** One chemical row; the whole row behaves like a button. */
    private final class ChemicalEntry extends ObjectSelectionList.Entry<ChemicalEntry> {

        private final Chemical chemical;

        private ChemicalEntry(Chemical chemical) {
            this.chemical = chemical;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width,
                           int height, int mouseX, int mouseY, boolean hovered,
                           float partialTick) {
            boolean dangerous = GasTapBlockEntity.isDangerousChemical(this.chemical);
            boolean locked = dangerous
                    && !GasSelectionScreen.this.tile.hasContainmentUpgrade();
            boolean pending = GasSelectionScreen.this.pendingChemical == this.chemical;
            int bottom = top + height - 2;
            int fill = pending ? BUTTON_DANGER
                    : (isSelected(this.chemical) ? BUTTON_ON
                    : (hovered ? BUTTON_HOVER : (locked ? BUTTON_LOCKED : BUTTON_OFF)));
            graphics.fill(left - 1, top - 1, left + width + 1, bottom + 1, BUTTON_BORDER);
            graphics.fill(left, top, left + width, bottom, fill);

            int swatchColor = 0xFF000000 | (this.chemical.getTint() & 0xFFFFFF);
            graphics.fill(left + 4, top + 3, left + 14, top + 11, swatchColor);

            String text = Component.translatable(this.chemical.getTranslationKey()).getString();
            int maxWidth = width - 64;
            if (GasSelectionScreen.this.font.width(text) > maxWidth) {
                text = GasSelectionScreen.this.font.plainSubstrByWidth(text,
                        maxWidth - GasSelectionScreen.this.font.width("...")) + "...";
            }
            int textColor = dangerous && !pending && !isSelected(this.chemical)
                    ? COLOR_DANGER : BUTTON_TEXT;
            graphics.drawString(GasSelectionScreen.this.font, text, left + 18,
                    top + (height - 2 - 8) / 2, textColor, false);

            graphics.drawString(GasSelectionScreen.this.font, Component.translatable(
                            "gui." + InfinityWhatMekanism.MOD_ID + ".chemical.chemical"),
                    left + width - 46, top + (height - 2 - 8) / 2, COLOR_TEXT, false);

            if (isSelected(this.chemical)) {
                graphics.drawString(GasSelectionScreen.this.font, "✓",
                        left + width - 12, top + (height - 2 - 8) / 2, BUTTON_TEXT, false);
            } else if (locked) {
                graphics.drawString(GasSelectionScreen.this.font, "×",
                        left + width - 12, top + (height - 2 - 8) / 2, COLOR_DANGER, false);
            } else if (dangerous) {
                graphics.drawString(GasSelectionScreen.this.font, "!",
                        left + width - 12, top + (height - 2 - 8) / 2, COLOR_DANGER, false);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                handleChemicalSelection(this.chemical);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.translatable(this.chemical.getTranslationKey());
        }
    }
}
