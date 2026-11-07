package com.bandybus.infwhat.mekanism.client.screen;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.entity.InfiniteSlurryGeneratorTileEntity;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiSlurryGauge;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Screen for the infinite slurry generator. */
public class InfiniteSlurryGeneratorScreen
        extends AbstractImportTapScreen<InfiniteSlurryGeneratorTileEntity> {

    public InfiniteSlurryGeneratorScreen(
            MekanismTileContainer<InfiniteSlurryGeneratorTileEntity> container,
            Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected void addTankGauge() {
        addRenderableWidget(new GuiSlurryGauge(() -> tile.getTank(),
                () -> tile.getSlurryTanks(null), GaugeType.STANDARD, this, 8, 8));
    }

    @Override
    protected Component kindLabel() {
        return Component.translatable("gui." + InfinityWhatMekanism.MOD_ID
                + ".chemical.slurry");
    }
}
