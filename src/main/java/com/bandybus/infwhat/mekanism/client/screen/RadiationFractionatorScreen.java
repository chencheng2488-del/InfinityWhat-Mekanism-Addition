package com.bandybus.infwhat.mekanism.client.screen;

import com.bandybus.infwhat.mekanism.block.entity.RadiationFractionatorBlockEntity;
import com.bandybus.infwhat.mekanism.recipe.FractionationRecipe;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/** Radiation Fractionator screen built on Mekanism's configurable machine GUI. */
public class RadiationFractionatorScreen extends GuiConfigurableTile<
        RadiationFractionatorBlockEntity,
        MekanismTileContainer<RadiationFractionatorBlockEntity>> {

    public RadiationFractionatorScreen(
            MekanismTileContainer<RadiationFractionatorBlockEntity> container,
            Inventory inventory, Component title) {
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
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 8, 18, 52));
        addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), tile::getActive));
        addRenderableWidget(new GuiGasGauge(() -> tile.getInputTank(),
                () -> tile.getGasTanks(null), GaugeType.STANDARD, this, 28, 8));
        addRenderableWidget(new GuiGasGauge(() -> tile.getOutputTank(),
                () -> tile.getGasTanks(null), GaugeType.STANDARD, this, 112, 8));
        addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.RIGHT,
                this, 74, 30));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        renderTitleText(graphics);
        graphics.drawString(this.getFont(), this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, this.titleTextColor(), false);

        drawStatus(graphics);
        super.drawForegroundText(graphics, mouseX, mouseY);
    }

    /** Status shares the inventory label row, right aligned, the way Mekanism's own screens do it. */
    private void drawStatus(GuiGraphics graphics) {
        Component text = status();
        graphics.drawString(this.getFont(), text,
                this.imageWidth - 8 - this.getFont().width(text),
                this.inventoryLabelY, statusColor(), false);
    }

    private Component status() {
        FractionationRecipe recipe = tile.getSelectedRecipe();
        if (recipe == null) {
            return Component.translatable(
                    "gui.infwhat_mekanism.machine.status.waiting_circuit");
        }
        Gas product = recipe.getOutputGas();
        if (product == null) {
            return Component.translatable("gui.infwhat_mekanism.machine.status.missing_recipe");
        }
        IGasTank outputTank = tile.getOutputTank();
        if (!outputTank.isEmpty() && outputTank.getType() != product) {
            return Component.translatable("gui.infwhat_mekanism.machine.status.wrong_gas");
        }
        if (outputTank.getNeeded() < FractionationRecipe.OUTPUT_PER_OPERATION) {
            return Component.translatable("gui.infwhat_mekanism.machine.status.tank_full");
        }
        if (tile.getInputTank().getStored() < recipe.getInputPerOperation()) {
            return Component.translatable("gui.infwhat_mekanism.machine.status.waiting_gas");
        }
        if (tile.getEnergyContainer().getEnergy()
                .smallerThan(tile.getEnergyContainer().getEnergyPerTick())) {
            return Component.translatable("gui.infwhat_mekanism.machine.status.no_energy");
        }
        if (tile.getOperatingTicks() > 0) {
            return Component.translatable("gui.infwhat_mekanism.machine.status.running");
        }
        return Component.translatable("gui.infwhat_mekanism.machine.status.ready");
    }

    private int statusColor() {
        FractionationRecipe recipe = tile.getSelectedRecipe();
        if (recipe == null || recipe.getOutputGas() == null) {
            return 0xFF9A6A00;
        }
        Gas product = recipe.getOutputGas();
        IGasTank outputTank = tile.getOutputTank();
        if ((!outputTank.isEmpty() && outputTank.getType() != product)
                || outputTank.getNeeded() < FractionationRecipe.OUTPUT_PER_OPERATION
                || tile.getInputTank().getStored() < recipe.getInputPerOperation()
                || tile.getEnergyContainer().getEnergy()
                .smallerThan(tile.getEnergyContainer().getEnergyPerTick())) {
            return 0xFF9A6A00;
        }
        return tile.getOperatingTicks() > 0 ? 0xFF1F7A3D : this.screenTextColor();
    }
}
