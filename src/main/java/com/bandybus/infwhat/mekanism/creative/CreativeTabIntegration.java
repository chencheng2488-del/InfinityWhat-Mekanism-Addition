package com.bandybus.infwhat.mekanism.creative;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModBlocks;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.item.ModItems;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Adds the add-on items to the core Infinity What tab instead of registering a second tab.
 * The core tab is a registry object compiled against the core jar; the event fires on the mod
 * bus after all mods have registered their tabs.
 */
@EventBusSubscriber(modid = InfinityWhatMekanism.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class CreativeTabIntegration {

    private CreativeTabIntegration() {
    }

    @SubscribeEvent
    public static void addItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(
                com.bandybus.infwhat.creative.ModCreativeTabs.INFINITY_WHAT.getKey())) {
            event.accept(ModMekMachines.GAS_TAP.getSecondary());
            event.accept(ModItems.UPGRADE_CONTAINMENT.get());
            event.accept(ModBlocks.RADIATION_SPONGE_ITEM.get());
            event.accept(ModItems.SATURATED_RADIATION_SPONGE.get());
            event.accept(ModMekMachines.RADIATION_EXTRACTOR.getSecondary());
            event.accept(ModMekMachines.RADIATION_FRACTIONATOR.getSecondary());
            event.accept(ModMekMachines.UNKNOWN_RADIATION_TANK.getSecondary());
            event.accept(ModItems.RADIATION_CIRCUIT.get());
        }
    }
}
