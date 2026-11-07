package com.bandybus.infwhat.mekanism.gas;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasBuilder;
import mekanism.api.chemical.gas.attribute.GasAttributes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mekanism gas registry entries owned by this add-on.
 *
 * <p>The unknown radiation substance is a normal, visible Mekanism gas so creative tanks, JEI and
 * pipe networks recognise it. The Gas Tap explicitly forbids this gas id instead of hiding it.
 */
public final class ModGases {

    public static final DeferredRegister<Gas> GASES =
            DeferredRegister.create(MekanismAPI.GAS_REGISTRY_NAME, InfinityWhatMekanism.MOD_ID);

    public static final DeferredHolder<Gas, Gas> UNKNOWN_RADIATION = GASES.register(
            "unknown_radiation",
            () -> {
                GasBuilder builder = GasBuilder.builder(
                        new ResourceLocation("mekanism", "liquid/liquid"))
                        .tint(0xFF8A5CF6);
                double strength = MekanismAdditionConfig.unknownRadiationStrength();
                if (strength > 0) {
                    //Strength is configurable so packs can dial the hazard level without a rebuild.
                    //The attribute rejects non-positive values, and zero means "harmless", so it is
                    //only attached when there is radiation to model.
                    builder.with(new GasAttributes.Radiation(strength));
                }
                return new Gas(builder);
            });

    private ModGases() {
    }

    public static void register(IEventBus eventBus) {
        GASES.register(eventBus);
    }
}
