package com.bandybus.infwhat.mekanism.client.screen;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.entity.GasTapBlockEntity;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.bandybus.infwhat.mekanism.network.MekanismAdditionNetwork;
import mekanism.api.chemical.Chemical;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Locale;

/** Infinite chemical generator screen: gases and pigments share one selection, rate and output. */
public class GasTapScreen extends GuiConfigurableTile<GasTapBlockEntity,
        MekanismTileContainer<GasTapBlockEntity>> implements ChemicalTapScreenAccess {

    private static final int GAS_X = 28;
    private static final int GAS_Y = 18;
    private static final int GAS_WIDTH = 90;
    private static final int GAS_HEIGHT = 16;

    private static final int OUTPUT_X = 28;
    private static final int OUTPUT_Y = 36;
    private static final int OUTPUT_WIDTH = 50;
    private static final int OUTPUT_HEIGHT = 14;

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
    private static final int BUTTON_LOCKED = 0xFF5A2A2A;
    private static final int BUTTON_HOVER = 0xFF456C83;
    private static final int BUTTON_BORDER = 0xFF070B0E;
    private static final int BUTTON_TEXT = 0xFFF0F5F7;
    /** Only for text drawn inside the dark buttons; the panel itself is light. */
    private static final int COLOR_TEXT = 0xFF9FB8C2;
    private static final int PANEL_OK = 0xFF1F7A3D;
    private static final int PANEL_WARN = 0xFF9A6A00;
    private static final int PANEL_BAD = 0xFFA03030;

    private GuiTextField rateField;
    private RateUnit rateUnit = RateUnit.MILLIBUCKETS;

    public GasTapScreen(MekanismTileContainer<GasTapBlockEntity> container, Inventory inventory,
                        Component title) {
        super(container, inventory, title);
        dynamicSlots = true;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX,
                            int mouseY) {
        MekanismRenderer.resetColor(graphics);
        GuiUtils.renderBackgroundTexture(graphics, GuiMekanism.BASE_BACKGROUND, 4, 4,
                this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiChemicalGauge(() -> tile.getChemicalTank(),
                () -> tile.getChemicalTanks(null), GaugeType.STANDARD, this, 8, 8));
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
    protected void drawForegroundText(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        renderTitleText(graphics);
        graphics.drawString(this.font(), this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, this.titleTextColor(), false);

        drawChemicalButton(graphics, tile.getSelectedChemical(),
                isHovering(GAS_X, GAS_Y, GAS_WIDTH, GAS_HEIGHT, mouseX, mouseY));
        boolean locked = tile.isOutputLocked();
        drawButton(graphics, OUTPUT_X, OUTPUT_Y, OUTPUT_WIDTH, OUTPUT_HEIGHT,
                Component.translatable("gui." + InfinityWhatMekanism.MOD_ID + ".output."
                        + (locked ? "locked" : tile.isOutputEnabled() ? "on" : "off")),
                !locked && tile.isOutputEnabled(), locked,
                !locked && isHovering(OUTPUT_X, OUTPUT_Y, OUTPUT_WIDTH, OUTPUT_HEIGHT,
                        mouseX, mouseY));

        graphics.drawString(this.font(),
                Component.translatable("gui." + InfinityWhatMekanism.MOD_ID + ".rate"),
                RATE_LABEL_X, RATE_LABEL_Y, this.titleTextColor(), false);
        drawButton(graphics, RATE_UNIT_X, RATE_UNIT_Y, RATE_UNIT_WIDTH,
                RATE_UNIT_HEIGHT, unitLabel(), false, false,
                isHovering(RATE_UNIT_X, RATE_UNIT_Y, RATE_UNIT_WIDTH,
                        RATE_UNIT_HEIGHT, mouseX, mouseY));

        Component upgrade = Component.translatable("gui." + InfinityWhatMekanism.MOD_ID
                + (tile.hasContainmentUpgrade() ? ".upgrade.installed" : ".upgrade.missing"));
        graphics.drawString(this.font(), upgrade, 116, 45,
                tile.hasContainmentUpgrade() ? PANEL_OK : this.titleTextColor(), false);

        drawStatus(graphics);
        super.drawForegroundText(graphics, mouseX, mouseY);
    }

    /** Status shares the inventory label row, right aligned, the way Mekanism's own screens do it. */
    private void drawStatus(GuiGraphics graphics) {
        Component text = status();
        graphics.drawString(this.font(), text,
                this.imageWidth - 8 - this.font().width(text),
                this.inventoryLabelY, statusColor(), false);
    }

    private Component status() {
        if (tile.getSelectedChemical() == null) {
            return Component.translatable("gui." + InfinityWhatMekanism.MOD_ID
                    + ".status.no_gas");
        }
        if (tile.selectedChemicalRequiresUpgrade()) {
            return Component.translatable("gui." + InfinityWhatMekanism.MOD_ID
                    + ".status.upgrade_required");
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
        if (!tile.isOutputEnabled() || tile.selectedChemicalRequiresUpgrade()) {
            return PANEL_WARN;
        }
        return tile.isProviding() ? PANEL_OK : PANEL_BAD;
    }

    private void drawChemicalButton(GuiGraphics graphics, @Nullable Chemical chemical,
                                    boolean hovered) {
        int x = GAS_X;
        int y = GAS_Y;
        graphics.fill(x - 1, y - 1, x + GAS_WIDTH + 1, y + GAS_HEIGHT + 1, BUTTON_BORDER);
        graphics.fill(x, y, x + GAS_WIDTH, y + GAS_HEIGHT, hovered ? BUTTON_HOVER : BUTTON_OFF);

        int swatch = chemical == null ? 0xFF59666D : 0xFF000000 | (chemical.getTint() & 0xFFFFFF);
        graphics.fill(x + 4, y + 3, x + 12, y + 11, swatch);

        Component label = chemical == null
                ? Component.translatable("gui." + InfinityWhatMekanism.MOD_ID + ".gas.none")
                : Component.translatable(chemical.getTranslationKey());
        String text = label.getString();
        int maxWidth = GAS_WIDTH - 28;
        if (this.font().width(text) > maxWidth) {
            text = this.font().plainSubstrByWidth(text, maxWidth
                    - this.font().width("...")) + "...";
        }
        graphics.drawString(this.font(), text, x + 16, y + 4, BUTTON_TEXT, false);
        graphics.drawString(this.font(), ">", x + GAS_WIDTH - 10, y + 4, COLOR_TEXT,
                false);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int width, int height,
                            Component label, boolean enabled, boolean locked, boolean hovered) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, BUTTON_BORDER);
        int fill = locked ? BUTTON_LOCKED
                : (hovered ? BUTTON_HOVER : (enabled ? BUTTON_ON : BUTTON_OFF));
        graphics.fill(x, y, x + width, y + height, fill);
        String text = label.getString();
        if (this.font().width(text) > width - 4) {
            text = this.font().plainSubstrByWidth(text, width - 4
                    - this.font().width("...")) + "...";
        }
        graphics.drawCenteredString(this.font(), text, x + width / 2,
                y + (height - 8) / 2, BUTTON_TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (int) mouseX - this.leftPos;
            int y = (int) mouseY - this.topPos;
            if (isInside(x, y, GAS_X, GAS_Y, GAS_WIDTH, GAS_HEIGHT)) {
                playButtonSound();
                openChemicalSelection();
                return true;
            }
            if (isInside(x, y, OUTPUT_X, OUTPUT_Y, OUTPUT_WIDTH, OUTPUT_HEIGHT)) {
                playButtonSound();
                if (tile.isOutputLocked()) {
                    this.minecraft.player.displayClientMessage(Component.translatable(
                            "gui." + InfinityWhatMekanism.MOD_ID + ".status.upgrade_required")
                            .withStyle(ChatFormatting.RED), true);
                    return true;
                }
                MekanismAdditionNetwork.sendOutputEnabled(this.menu.containerId,
                        !tile.isOutputEnabled());
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

    private void openChemicalSelection() {
        if (tile.getAvailableChemicals().isEmpty()) {
            this.minecraft.player.displayClientMessage(Component.translatable(
                    "gui." + InfinityWhatMekanism.MOD_ID + ".gas.no_gases")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        this.minecraft.setScreen(new GasSelectionScreen(this));
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

    public GasTapBlockEntity getGasTap() {
        return this.tile;
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
            this.rateField.setText(formatRate(tile.getOutputAmountPerTick(),
                    this.rateUnit));
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

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
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
