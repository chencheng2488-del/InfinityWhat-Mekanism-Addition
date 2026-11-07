package com.bandybus.infwhat.mekanism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/** Block item for the one-shot Super Radiation Sponge. */
public class RadiationSpongeBlockItem extends BlockItem {

    public RadiationSpongeBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(
                        "item.infwhat_mekanism.radiation_sponge.tooltip")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        "item.infwhat_mekanism.radiation_sponge.saturated_tooltip")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
