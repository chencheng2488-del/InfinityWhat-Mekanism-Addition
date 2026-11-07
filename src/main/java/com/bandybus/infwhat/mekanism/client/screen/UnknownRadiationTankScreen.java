package com.bandybus.infwhat.mekanism.client.screen;

import com.bandybus.infwhat.mekanism.block.entity.UnknownRadiationTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/** Mekanism style screen for the unknown radiation storage tank. */
public class UnknownRadiationTankScreen extends GuiConfigurableTile<UnknownRadiationTankBlockEntity,
        MekanismTileContainer<UnknownRadiationTankBlockEntity>> {

    public UnknownRadiationTankScreen(
            MekanismTileContainer<UnknownRadiationTankBlockEntity> container,
            Inventory inventory, Component title) {
        super(container, inventory, title);
        dynamicSlots = true;
    }

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
        addRenderableWidget(new GuiGasGauge(() -> tile.getGasTank(),
                () -> tile.getGasTanks(null), GaugeType.STANDARD, this, 30, 8));
    }
}
