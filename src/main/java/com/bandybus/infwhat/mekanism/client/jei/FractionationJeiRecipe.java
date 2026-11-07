package com.bandybus.infwhat.mekanism.client.jei;

import com.bandybus.infwhat.mekanism.item.ModItems;
import com.bandybus.infwhat.mekanism.item.RadiationCircuitItem;
import com.bandybus.infwhat.mekanism.recipe.FractionationRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** One JEI entry of the fractionator: a configured circuit plus the resulting gas. */
public record FractionationJeiRecipe(FractionationRecipe recipe, ItemStack circuit) {

    public static List<FractionationJeiRecipe> createAll() {
        List<FractionationJeiRecipe> recipes = new ArrayList<>();
        for (FractionationRecipe recipe : FractionationRecipe.values()) {
            ItemStack stack = new ItemStack(ModItems.RADIATION_CIRCUIT.get());
            RadiationCircuitItem.setCircuit(stack, recipe.getCircuit());
            recipes.add(new FractionationJeiRecipe(recipe, stack));
        }
        return recipes;
    }

    public long inputAmount() {
        return this.recipe.getInputPerOperation();
    }

    public Component outputName() {
        return this.recipe.getDisplayName();
    }
}
