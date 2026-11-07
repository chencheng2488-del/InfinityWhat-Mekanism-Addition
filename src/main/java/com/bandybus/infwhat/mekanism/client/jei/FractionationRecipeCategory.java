package com.bandybus.infwhat.mekanism.client.jei;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI view of the radiation fractionator. Gases are not JEI ingredients, so the gas side is shown
 * as text while the configured circuit is displayed in a real input slot.
 */
public class FractionationRecipeCategory extends AbstractRecipeCategory<FractionationJeiRecipe> {

    public static final RecipeType<FractionationJeiRecipe> TYPE = RecipeType.create(
            InfinityWhatMekanism.MOD_ID, "fractionation", FractionationJeiRecipe.class);

    private static final int WIDTH = 178;
    private static final int HEIGHT = 40;
    private static final int TEXT_COLOR = 0xFF9FB8C2;
    private static final int OUTPUT_COLOR = 0xFF22C7B4;

    public FractionationRecipeCategory(IGuiHelper guiHelper) {
        super(TYPE,
                Component.translatable("jei.infwhat_mekanism.fractionation"),
                guiHelper.createDrawableItemStack(
                        new ItemStack(ModMekMachines.RADIATION_FRACTIONATOR.getSecondary())),
                WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FractionationJeiRecipe recipe,
                          IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 12).addItemStack(recipe.circuit());
    }

    @Override
    public void draw(FractionationJeiRecipe recipe, IRecipeSlotsView slots, PoseStack pose,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        font.draw(pose,
                Component.translatable("jei.infwhat_mekanism.fractionation.input",
                        recipe.inputAmount()),
                22, 3, TEXT_COLOR);
        font.draw(pose,
                Component.translatable("jei.infwhat_mekanism.fractionation.output",
                        10, recipe.outputName()),
                22, 21, OUTPUT_COLOR);
    }
}
