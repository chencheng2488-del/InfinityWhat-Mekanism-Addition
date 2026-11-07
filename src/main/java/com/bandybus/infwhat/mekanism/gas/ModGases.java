package com.bandybus.infwhat.mekanism.gas;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasBuilder;
import mekanism.api.chemical.gas.attribute.GasAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Mekanism gas registry entries owned by this add-on.
 *
 * <p>The unknown radiation substance is a normal, visible Mekanism gas so creative tanks, JEI and
 * pipe networks recognise it. The Gas Tap explicitly forbids this gas id instead of hiding it.
 */
public final class ModGases {

    public static final DeferredRegister<Gas> GASES =
            DeferredRegister.create(MekanismAPI.GAS_REGISTRY_NAME, InfinityWhatMekanism.MOD_ID);

    public static final RegistryObject<Gas> UNKNOWN_RADIATION = GASES.register(
            "unknown_radiation",
            () -> new Gas(GasBuilder.builder(
                    ResourceLocation.fromNamespaceAndPath("mekanism", "liquid/liquid"))
                    .tint(0xFF8A5CF6)
                    //Strength is configurable so packs can dial the hazard level without a rebuild.
                    .with(new GasAttributes.Radiation(MekanismAdditionConfig.unknownRadiationStrength()))));

    private ModGases() {
    }

    public static void register(IEventBus eventBus) {
        GASES.register(eventBus);
    }
}
