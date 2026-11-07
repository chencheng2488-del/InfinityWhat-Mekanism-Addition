package com.bandybus.infwhat.mekanism.client.screen;

import com.bandybus.infwhat.mekanism.block.entity.RadiationExtractorBlockEntity;
import com.bandybus.infwhat.mekanism.gas.ModChemicals;
import mekanism.api.chemical.IChemicalTank;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/** Radiation Extractor screen built on Mekanism's configurable machine GUI. */
public class RadiationExtractorScreen extends GuiConfigurableTile<RadiationExtractorBlockEntity,
        MekanismTileContainer<RadiationExtractorBlockEntity>> {

    public RadiationExtractorScreen(MekanismTileContainer<RadiationExtractorBlockEntity> container,
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
        addRenderableWidget(new GuiChemicalGauge(() -> tile.getOutputTank(),
                () -> tile.getChemicalTanks(null), GaugeType.STANDARD, this, 112, 8));
        addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.RIGHT,
                this, 54, 30));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        renderTitleText(graphics);
        graphics.drawString(this.font(), this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, this.titleTextColor(), false);

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
        if (tile.getInputSlot().getStack().isEmpty()) {
            return Component.translatable("gui.infwhat_mekanism.machine.status.waiting_input");
        }
        if (!tile.canAcceptOutput()) {
            return Component.translatable("gui.infwhat_mekanism.machine.status.output_slot_full");
        }
        IChemicalTank tank = tile.getOutputTank();
        if (!tank.isEmpty()
                && tank.getStack().getChemical() != ModChemicals.UNKNOWN_RADIATION.get()) {
            return Component.translatable("gui.infwhat_mekanism.machine.status.wrong_gas");
        }
        long amount = RadiationExtractorBlockEntity.chemicalAmountFor(tile.getInputSlot().getStack());
        if (tank.getNeeded() < amount) {
            return Component.translatable("gui.infwhat_mekanism.machine.status.tank_full");
        }
        if (tile.getEnergyContainer().getEnergy()
                < tile.getEnergyContainer().getEnergyPerTick()) {
            return Component.translatable("gui.infwhat_mekanism.machine.status.no_energy");
        }
        if (tile.getOperatingTicks() > 0) {
            return Component.translatable("gui.infwhat_mekanism.machine.status.running");
        }
        return Component.translatable("gui.infwhat_mekanism.machine.status.ready");
    }

    private int statusColor() {
        if (tile.getInputSlot().getStack().isEmpty()) {
            return this.screenTextColor();
        }
        if (!tile.canAcceptOutput()
                || tile.getOutputTank().getNeeded() < RadiationExtractorBlockEntity
                .chemicalAmountFor(tile.getInputSlot().getStack())
                || (!tile.getOutputTank().isEmpty()
                && tile.getOutputTank().getStack().getChemical()
                != ModChemicals.UNKNOWN_RADIATION.get())
                || tile.getEnergyContainer().getEnergy()
                < tile.getEnergyContainer().getEnergyPerTick()) {
            return 0xFF9A6A00;
        }
        return tile.getOperatingTicks() > 0 ? 0xFF1F7A3D : this.screenTextColor();
    }
}
