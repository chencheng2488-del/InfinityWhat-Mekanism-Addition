package com.bandybus.infwhat.mekanism;

import com.bandybus.infwhat.mekanism.block.ModBlocks;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.block.entity.ModBlockEntities;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.bandybus.infwhat.mekanism.gas.ModGases;
import com.bandybus.infwhat.mekanism.item.ModItems;
import com.bandybus.infwhat.mekanism.network.MekanismAdditionNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    public InfinityWhatMekanism(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModBlocks.register(modEventBus);
        ModMekMachines.register(modEventBus);
        ModItems.register(modEventBus);
        ModGases.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        MekanismAdditionNetwork.register();
        MekanismAdditionConfig.register(context);
        LOGGER.info("Infinity What: Mekanism Addition loaded");
    }
}
