package com.bandybus.infwhat.mekanism.client;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.client.render.RadiationExtractorPortRenderer;
import com.bandybus.infwhat.mekanism.client.screen.GasTapScreen;
import com.bandybus.infwhat.mekanism.client.screen.InfiniteInfusionGeneratorScreen;
import com.bandybus.infwhat.mekanism.client.screen.InfiniteSlurryGeneratorScreen;
import com.bandybus.infwhat.mekanism.client.screen.RadiationExtractorScreen;
import com.bandybus.infwhat.mekanism.client.screen.RadiationFractionatorScreen;
import com.bandybus.infwhat.mekanism.client.screen.UnknownRadiationTankScreen;
import com.bandybus.infwhat.mekanism.item.ModItems;
import com.bandybus.infwhat.mekanism.item.RadiationCircuitItem;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only menu screen registration. */
@Mod.EventBusSubscriber(modid = InfinityWhatMekanism.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GasTapClientSetup {

    private GasTapClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(
                ModMekMachines.GAS_TAP_CONTAINER.get(), GasTapScreen::new));
        event.enqueueWork(() -> MenuScreens.register(
                ModMekMachines.RADIATION_EXTRACTOR_CONTAINER.get(),
                RadiationExtractorScreen::new));
        event.enqueueWork(() -> MenuScreens.register(
                ModMekMachines.RADIATION_FRACTIONATOR_CONTAINER.get(),
                RadiationFractionatorScreen::new));
        event.enqueueWork(() -> MenuScreens.register(
                ModMekMachines.INFINITE_INFUSION_GENERATOR_CONTAINER.get(),
                InfiniteInfusionGeneratorScreen::new));
        event.enqueueWork(() -> MenuScreens.register(
                ModMekMachines.INFINITE_SLURRY_GENERATOR_CONTAINER.get(),
                InfiniteSlurryGeneratorScreen::new));
        event.enqueueWork(() -> MenuScreens.register(
                ModMekMachines.UNKNOWN_RADIATION_TANK_CONTAINER.get(),
                UnknownRadiationTankScreen::new));
        //The circuit chip colour follows the selected formula. The property lives in the vanilla
        //namespace so the model predicate key can stay the plain "circuit".
        event.enqueueWork(() -> ItemProperties.register(ModItems.RADIATION_CIRCUIT.get(),
                new ResourceLocation("circuit"),
                (stack, level, entity, seed) -> RadiationCircuitItem.getCircuit(stack)));
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModMekMachines.RADIATION_EXTRACTOR_TILE.get(),
                context -> new RadiationExtractorPortRenderer());
    }
}
