package com.bandybus.infwhat.mekanism.recipe;

import com.bandybus.infwhat.mekanism.item.RadiationCircuitItem;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The six fractionation selections encoded by a Radiation Configured Circuit.
 *
 * <p>The ratio is the amount of unknown radiation substance consumed per unit of product. Cheap
 * products use a 1:1 ratio while antimatter needs sixteen times as much input, so the machine
 * cannot be used as a shortcut around Mekanism's own nuclear processing chain.
 */
public enum FractionationRecipe {

    NUCLEAR_WASTE(1, "nuclear_waste", 1),
    SPENT_NUCLEAR_WASTE(2, "spent_nuclear_waste", 2),
    POLONIUM(3, "polonium", 4),
    PLUTONIUM(4, "plutonium", 4),
    FISSILE_FUEL(5, "fissile_fuel", 8),
    ANTIMATTER(6, "antimatter", 16);

    /** Product amount produced by one completed operation. */
    public static final int OUTPUT_PER_OPERATION = 10;

    private final int circuit;
    private final ResourceLocation gasId;
    private final int inputRatio;

    FractionationRecipe(int circuit, String gasPath, int inputRatio) {
        this.circuit = circuit;
        this.gasId = ResourceLocation.fromNamespaceAndPath("mekanism", gasPath);
        this.inputRatio = inputRatio;
    }

    public int getCircuit() {
        return this.circuit;
    }

    public int getInputRatio() {
        return this.inputRatio;
    }

    public long getInputPerOperation() {
        return (long) OUTPUT_PER_OPERATION * this.inputRatio;
    }

    /** Resolves the product chemical. Returns null if Mekanism does not have it registered. */
    @Nullable
    public Chemical getOutputChemical() {
        Chemical chemical = MekanismAPI.CHEMICAL_REGISTRY.get(this.gasId);
        return chemical == null || chemical == MekanismAPI.EMPTY_CHEMICAL ? null : chemical;
    }

    public Component getDisplayName() {
        Chemical chemical = getOutputChemical();
        return chemical == null
                ? Component.translatable("gui.infwhat_mekanism.machine.recipe.missing")
                : Component.translatable(chemical.getTranslationKey());
    }

    @Nullable
    public static FractionationRecipe byCircuit(int circuit) {
        for (FractionationRecipe recipe : values()) {
            if (recipe.circuit == circuit) {
                return recipe;
            }
        }
        return null;
    }

    /** Returns the selected recipe for the circuit stack, or null when no circuit is installed. */
    @Nullable
    public static FractionationRecipe forStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof RadiationCircuitItem)) {
            return null;
        }
        return byCircuit(RadiationCircuitItem.getCircuit(stack));
    }
}
