package com.bandybus.infwhat.mekanism.client.jei;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.item.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** JEI integration for the radiation production line. */
@JeiPlugin
public class MekanismAdditionJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(InfinityWhatMekanism.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FractionationRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FractionationRecipeCategory.TYPE,
                FractionationJeiRecipe.createAll());
        registration.addItemStackInfo(new ItemStack(ModItems.SATURATED_RADIATION_SPONGE.get()),
                Component.translatable("jei.infwhat_mekanism.extractor.info"),
                Component.translatable("jei.infwhat_mekanism.extractor.formula"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(ModMekMachines.RADIATION_FRACTIONATOR.getSecondary()),
                FractionationRecipeCategory.TYPE);
    }
}
