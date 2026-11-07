package com.bandybus.infwhat.mekanism;

import com.bandybus.infwhat.mekanism.block.ModBlocks;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.block.entity.ModBlockEntities;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.bandybus.infwhat.mekanism.gas.ModChemicals;
import com.bandybus.infwhat.mekanism.item.ModItems;
import com.bandybus.infwhat.mekanism.network.MekanismAdditionNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional Mekanism add-on for Infinity What.
 *
 * <p>The add-on only loads when both Infinity What and Mekanism are installed. It contains the
 * Gas Tap, its menu/screen, its config card and the server-side governance config. The core mod
 * remains completely unaware of Mekanism.
 */
@Mod(InfinityWhatMekanism.MOD_ID)
public class InfinityWhatMekanism {

    public static final String MOD_ID = "infwhat_mekanism";
    public static final Logger LOGGER = LoggerFactory.getLogger("InfinityWhatMekanism");

    public InfinityWhatMekanism(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModMekMachines.register(modEventBus);
        ModItems.register(modEventBus);
        ModChemicals.CHEMICALS.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        MekanismAdditionNetwork.register(modEventBus);
        MekanismAdditionConfig.register(modContainer);
        modEventBus.addListener((ModConfigEvent.Loading event) -> onConfigChanged(event));
        modEventBus.addListener((ModConfigEvent.Reloading event) -> onConfigChanged(event));
        modEventBus.addListener((FMLCommonSetupEvent event) ->
                event.enqueueWork(ModChemicals::applyRadiationStrength));
        LOGGER.info("Infinity What: Mekanism Addition loaded");
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == MekanismAdditionConfig.SPEC) {
            ModChemicals.applyRadiationStrength();
        }
    }
}
