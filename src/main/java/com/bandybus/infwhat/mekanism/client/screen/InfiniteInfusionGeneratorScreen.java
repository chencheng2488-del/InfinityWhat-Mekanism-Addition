package com.bandybus.infwhat.mekanism.client.screen;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.entity.InfiniteInfusionGeneratorTileEntity;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiInfusionGauge;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Screen for the infinite infusion generator. */
public class InfiniteInfusionGeneratorScreen
        extends AbstractImportTapScreen<InfiniteInfusionGeneratorTileEntity> {

    public InfiniteInfusionGeneratorScreen(
            MekanismTileContainer<InfiniteInfusionGeneratorTileEntity> container,
            Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected void addTankGauge() {
        addRenderableWidget(new GuiInfusionGauge(() -> tile.getTank(),
                () -> tile.getInfusionTanks(null), GaugeType.STANDARD, this, 8, 8));
    }

    @Override
    protected Component kindLabel() {
        return Component.translatable("gui." + InfinityWhatMekanism.MOD_ID
                + ".chemical.infusion");
    }
}
