package com.bandybus.infwhat.mekanism.client.screen;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.entity.ChemicalTapTileEntity;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.bandybus.infwhat.mekanism.network.MekanismAdditionNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.api.chemical.Chemical;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Shared screen for the import driven taps (infusion and slurry). The machine has no selection
 * list: the player feeds it through its slot or an adjacent pipe, and the clear button releases
 * the locked type again.
 */
public abstract class AbstractImportTapScreen<TILE extends ChemicalTapTileEntity<?, ?, ?>>
        extends GuiConfigurableTile<TILE, MekanismTileContainer<TILE>>
        implements ChemicalTapScreenAccess {

    private static final int OUTPUT_X = 28;
    private static final int OUTPUT_Y = 32;
    private static final int OUTPUT_WIDTH = 50;
    private static final int OUTPUT_HEIGHT = 14;

    private static final int CLEAR_X = 82;
    private static final int CLEAR_Y = 32;
    private static final int CLEAR_WIDTH = 50;
    private static final int CLEAR_HEIGHT = 14;

    private static final int RATE_LABEL_X = 28;
    private static final int RATE_LABEL_Y = 58;
    private static final int RATE_FIELD_X = 52;
    private static final int RATE_FIELD_Y = 54;
    private static final int RATE_FIELD_WIDTH = 80;
    private static final int RATE_FIELD_HEIGHT = 14;
    private static final int RATE_UNIT_X = 142;
    private static final int RATE_UNIT_Y = 55;
    private static final int RATE_UNIT_WIDTH = 26;
    private static final int RATE_UNIT_HEIGHT = 12;

    private static final int BUTTON_OFF = 0xFF303B42;
    private static final int BUTTON_ON = 0xFF256B43;
    private static final int BUTTON_DANGER = 0xFF5A2A2A;
    private static final int BUTTON_HOVER = 0xFF456C83;
    private static final int BUTTON_BORDER = 0xFF070B0E;
    private static final int BUTTON_TEXT = 0xFFF0F5F7;
    private static final int COLOR_TEXT = 0xFF9FB8C2;
    private static final int PANEL_OK = 0xFF1F7A3D;
    private static final int PANEL_WARN = 0xFF9A6A00;

    private GuiTextField rateField;
    private RateUnit rateUnit = RateUnit.MILLIBUCKETS;

    protected AbstractImportTapScreen(MekanismTileContainer<TILE> container,
                                      Inventory inventory, Component title) {
        super(container, inventory, title);
        dynamicSlots = true;
    }

    /** Adds the gauge for this machine's chemical type at the shared position. */
    protected abstract void addTankGauge();

    /** Localised name of this machine's chemical kind, used in the status row. */
    protected abstract Component kindLabel();

    @Override
    protected void renderBg(@NotNull PoseStack matrix, float partialTick, int mouseX,
                            int mouseY) {
        MekanismRenderer.resetColor();
        GuiUtils.renderBackgroundTexture(matrix, GuiMekanism.BASE_BACKGROUND, 4, 4,
                this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addTankGauge();
        this.rateField = addRenderableWidget(new GuiTextField(this, RATE_FIELD_X,
                RATE_FIELD_Y, RATE_FIELD_WIDTH, RATE_FIELD_HEIGHT)
                .configureDigitalBorderInput(this::submitRate)
                .setInputValidator(this::isValidRateCharacter));
        this.rateField.setMaxLength(19);
        this.rateField.setText(formatRate(tile.getOutputAmountPerTick(), this.rateUnit));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.rateField != null && !this.rateField.isTextFieldFocused()) {
            String value = formatRate(tile.getOutputAmountPerTick(), this.rateUnit);
            if (!value.equals(this.rateField.getText())) {
                this.rateField.setText(value);
            }
        }
    }

    @Override
    protected void drawForegroundText(@NotNull PoseStack matrix, int mouseX, int mouseY) {
        renderTitleText(matrix);
        this.getFont().draw(matrix, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, this.titleTextColor());

        drawSourceLine(matrix);

        drawButton(matrix, OUTPUT_X, OUTPUT_Y, OUTPUT_WIDTH, OUTPUT_HEIGHT,
                Component.translatable("gui." + InfinityWhatMekanism.MOD_ID + ".output."
                        + (tile.isOutputEnabled() ? "on" : "off")),
                tile.isOutputEnabled(), false,
                isHovering(mouseX, mouseY, OUTPUT_X, OUTPUT_Y, OUTPUT_WIDTH, OUTPUT_HEIGHT));
        drawButton(matrix, CLEAR_X, CLEAR_Y, CLEAR_WIDTH, CLEAR_HEIGHT,
                Component.translatable("gui." + InfinityWhatMekanism.MOD_ID + ".import.clear"),
                false, false,
                tile.getSelectedChemical() != null
                        && isHovering(mouseX, mouseY, CLEAR_X, CLEAR_Y, CLEAR_WIDTH,
                        CLEAR_HEIGHT));

        this.getFont().draw(matrix, Component.translatable(
                        "gui." + InfinityWhatMekanism.MOD_ID + ".rate"),
                RATE_LABEL_X, RATE_LABEL_Y, this.titleTextColor());
        drawButton(matrix, RATE_UNIT_X, RATE_UNIT_Y, RATE_UNIT_WIDTH,
                RATE_UNIT_HEIGHT, unitLabel(), false, false,
                isHovering(mouseX, mouseY, RATE_UNIT_X, RATE_UNIT_Y, RATE_UNIT_WIDTH,
                        RATE_UNIT_HEIGHT));

        drawStatus(matrix);
        super.drawForegroundText(matrix, mouseX, mouseY);
    }

    private void drawSourceLine(PoseStack matrix) {
        Chemical<?> chemical = tile.getSelectedChemical();
        Component text = chemical == null
                ? Component.translatable("gui." + InfinityWhatMekanism.MOD_ID + ".import.empty")
                : Component.translatable(chemical.getTranslationKey());
        String value = text.getString();
        int maxWidth = 130;
        if (this.getFont().width(value) > maxWidth) {
            value = this.getFont().plainSubstrByWidth(value,
                    maxWidth - this.getFont().width("...")) + "...";
        }
        this.getFont().draw(matrix, value, 28, 16,
                chemical == null ? this.titleTextColor() : PANEL_OK);
    }

    private void drawStatus(PoseStack matrix) {
        Component text = status();
        this.getFont().draw(matrix, text,
                this.imageWidth - 8 - this.getFont().width(text),
                this.inventoryLabelY, statusColor());
    }

    private Component status() {
        if (tile.getSelectedChemical() == null) {
            return Component.translatable("gui." + InfinityWhatMekanism.MOD_ID
                    + ".import.waiting", kindLabel());
        }
        if (!tile.isOutputEnabled()) {
            return Component.translatable("gui." + InfinityWhatMekanism.MOD_ID
                    + ".status.output_off");
        }
        if (!tile.isProviding()) {
            return Component.translatable("gui." + InfinityWhatMekanism.MOD_ID
                    + ".status.disabled");
        }
        return Component.translatable("gui." + InfinityWhatMekanism.MOD_ID
                + ".status.providing");
    }

    private int statusColor() {
        if (tile.getSelectedChemical() == null) {
            return this.titleTextColor();
        }
        return tile.isProviding() ? PANEL_OK : PANEL_WARN;
    }

    private void drawButton(PoseStack matrix, int x, int y, int width, int height,
                            Component label, boolean enabled, boolean danger, boolean hovered) {
        fill(matrix, x - 1, y - 1, x + width + 1, y + height + 1, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (danger ? BUTTON_DANGER : (enabled ? BUTTON_ON : BUTTON_OFF));
        fill(matrix, x, y, x + width, y + height, fill);
        String text = label.getString();
        if (this.getFont().width(text) > width - 4) {
            text = this.getFont().plainSubstrByWidth(text, width - 4
                    - this.getFont().width("...")) + "...";
        }
        drawCenteredString(matrix, this.getFont(), text, x + width / 2,
                y + (height - 8) / 2, BUTTON_TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (int) mouseX - this.leftPos;
            int y = (int) mouseY - this.topPos;
            if (isInside(x, y, OUTPUT_X, OUTPUT_Y, OUTPUT_WIDTH, OUTPUT_HEIGHT)) {
                playButtonSound();
                MekanismAdditionNetwork.sendOutputEnabled(this.menu.containerId,
                        !tile.isOutputEnabled());
                return true;
            }
            if (isInside(x, y, CLEAR_X, CLEAR_Y, CLEAR_WIDTH, CLEAR_HEIGHT)) {
                if (tile.getSelectedChemical() != null) {
                    playButtonSound();
                    MekanismAdditionNetwork.sendClearChemical(this.menu.containerId);
                }
                return true;
            }
            if (isInside(x, y, RATE_UNIT_X, RATE_UNIT_Y, RATE_UNIT_WIDTH,
                    RATE_UNIT_HEIGHT)) {
                playButtonSound();
                toggleRateUnit();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.rateField != null && this.rateField.isTextFieldFocused()
                && this.rateField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void submitRate() {
        try {
            long amount = parseRate(this.rateField.getText(), this.rateUnit);
            if (amount > MekanismAdditionConfig.maxRatePerTick()) {
                throw new NumberFormatException("rate out of range");
            }
            MekanismAdditionNetwork.sendRate(this.menu.containerId, amount);
        } catch (NumberFormatException | ArithmeticException exception) {
            this.minecraft.player.displayClientMessage(Component.translatable(
                    "gui." + InfinityWhatMekanism.MOD_ID + ".rate.invalid")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    private void toggleRateUnit() {
        long stored;
        try {
            stored = parseRate(this.rateField.getText(), this.rateUnit);
            if (stored > MekanismAdditionConfig.maxRatePerTick()) {
                stored = tile.getOutputAmountPerTick();
            }
        } catch (NumberFormatException | ArithmeticException exception) {
            stored = tile.getOutputAmountPerTick();
        }
        this.rateUnit = this.rateUnit == RateUnit.MILLIBUCKETS
                ? RateUnit.BUCKETS : RateUnit.MILLIBUCKETS;
        this.rateField.setInputValidator(this::isValidRateCharacter);
        this.rateField.setText(formatRate(stored, this.rateUnit));
    }

    private boolean isValidRateCharacter(char character) {
        return Character.isDigit(character)
                || (this.rateUnit == RateUnit.BUCKETS && character == '.');
    }

    private Component unitLabel() {
        return Component.translatable("gui." + InfinityWhatMekanism.MOD_ID
                + (this.rateUnit == RateUnit.BUCKETS ? ".rate.unit.b" : ".rate.unit.mb"));
    }

    private static long parseRate(String text, RateUnit unit) {
        String value = text.trim();
        if (value.isEmpty()) {
            throw new NumberFormatException("empty rate");
        }
        long amount;
        if (unit == RateUnit.MILLIBUCKETS) {
            amount = Long.parseLong(value);
        } else {
            amount = new BigDecimal(value).multiply(BigDecimal.valueOf(1000L))
                    .longValueExact();
        }
        if (amount < 1L) {
            throw new NumberFormatException("rate below minimum");
        }
        return amount;
    }

    private static String formatRate(long amount, RateUnit unit) {
        if (unit == RateUnit.MILLIBUCKETS) {
            return Long.toString(amount);
        }
        long whole = amount / 1000L;
        int fraction = (int) (amount % 1000L);
        if (fraction == 0) {
            return Long.toString(whole);
        }
        String digits = String.format(Locale.ROOT, "%03d", fraction);
        int end = digits.length();
        while (end > 0 && digits.charAt(end - 1) == '0') {
            end--;
        }
        return whole + "." + digits.substring(0, end);
    }

    @Override
    public int getContainerId() {
        return this.menu.containerId;
    }

    @Override
    public void onRateResult(boolean success, long amount) {
        if (success) {
            this.rateField.setText(formatRate(amount, this.rateUnit));
            this.minecraft.player.displayClientMessage(Component.translatable(
                    "gui." + InfinityWhatMekanism.MOD_ID + ".rate.saved")
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            this.rateField.setText(formatRate(tile.getOutputAmountPerTick(), this.rateUnit));
            this.minecraft.player.displayClientMessage(Component.translatable(
                    "gui." + InfinityWhatMekanism.MOD_ID + ".rate.rejected")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    @Override
    public void onChemicalResult(boolean success) {
        if (!success) {
            this.minecraft.player.displayClientMessage(Component.translatable(
                    "gui." + InfinityWhatMekanism.MOD_ID + ".gas.rejected")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    private void playButtonSound() {
        this.minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private boolean isHovering(int mouseX, int mouseY, int x, int y, int width, int height) {
        return isInside(mouseX - this.leftPos, mouseY - this.topPos, x, y, width, height);
    }

    private static boolean isInside(double x, double y, int left, int top, int width,
                                    int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private enum RateUnit {
        MILLIBUCKETS,
        BUCKETS
    }
}
