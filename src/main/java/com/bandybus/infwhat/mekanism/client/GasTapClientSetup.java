package com.bandybus.infwhat.mekanism.client;

import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.client.render.RadiationExtractorPortRenderer;
import com.bandybus.infwhat.mekanism.client.screen.GasTapScreen;
import com.bandybus.infwhat.mekanism.client.screen.RadiationExtractorScreen;
import com.bandybus.infwhat.mekanism.client.screen.RadiationFractionatorScreen;
import com.bandybus.infwhat.mekanism.client.screen.UnknownRadiationTankScreen;
import com.bandybus.infwhat.mekanism.item.ModItems;
import com.bandybus.infwhat.mekanism.item.RadiationCircuitItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only menu screen registration. */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GasTapClientSetup {

    private GasTapClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMekMachines.GAS_TAP_CONTAINER.get(), GasTapScreen::new);
        event.register(ModMekMachines.RADIATION_EXTRACTOR_CONTAINER.get(),
                RadiationExtractorScreen::new);
        event.register(ModMekMachines.RADIATION_FRACTIONATOR_CONTAINER.get(),
                RadiationFractionatorScreen::new);
        event.register(ModMekMachines.UNKNOWN_RADIATION_TANK_CONTAINER.get(),
                UnknownRadiationTankScreen::new);
    }

    @SubscribeEvent
    public static void registerItemProperties(FMLClientSetupEvent event) {
        //The circuit chip colour follows the selected formula. The property lives in the vanilla
        //namespace so the model predicate key can stay the plain "circuit".
        event.enqueueWork(() -> ItemProperties.register(ModItems.RADIATION_CIRCUIT.get(),
                ResourceLocation.withDefaultNamespace("circuit"),
                (stack, level, entity, seed) -> RadiationCircuitItem.getCircuit(stack)));
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModMekMachines.RADIATION_EXTRACTOR_TILE.get(),
                context -> new RadiationExtractorPortRenderer());
    }
}
