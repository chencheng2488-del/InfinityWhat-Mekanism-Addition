package com.bandybus.infwhat.mekanism.gas;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import mekanism.api.chemical.attribute.ChemicalAttributes;
import mekanism.common.registration.impl.ChemicalDeferredRegister;
import mekanism.common.registration.impl.DeferredChemical;
import net.minecraft.resources.ResourceLocation;

/**
 * Mekanism chemical registry entries owned by this add-on.
 *
 * <p>The unknown radiation substance is a normal, visible Mekanism chemical so creative tanks,
 * JEI and pipe networks recognise it. The chemical generator explicitly forbids this id instead of
 * hiding it.
 */
public final class ModChemicals {

    public static final ChemicalDeferredRegister CHEMICALS =
            new ChemicalDeferredRegister(InfinityWhatMekanism.MOD_ID);

    public static final DeferredChemical<Chemical> UNKNOWN_RADIATION = CHEMICALS.register(
            "unknown_radiation",
            () -> new Chemical(ChemicalBuilder
                    .builder(ResourceLocation.fromNamespaceAndPath("mekanism", "liquid/liquid"))
                    .tint(0xFF8A5CF6)));

    private ModChemicals() {
    }

    /**
     * Applies the configured radioactivity once the common config is available.
     *
     * <p>Registry entries are created during the register event, which happens before NeoForge
     * loads the config file. Reading the config inside the registry supplier throws
     * {@code Cannot get config value before config is loaded}, so the attribute is attached in
     * common setup instead. A non-positive configured strength leaves the chemical harmless.
     */
    public static void applyRadiationStrength() {
        double strength = MekanismAdditionConfig.unknownRadiationStrength();
        if (strength > 0.0D) {
            UNKNOWN_RADIATION.get().addAttribute(new ChemicalAttributes.Radiation(strength));
        }
    }
}
