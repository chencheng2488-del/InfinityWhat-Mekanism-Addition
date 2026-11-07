package com.bandybus.infwhat.mekanism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Machine-upgrade-style item for the chemical generator.
 *
 * <p>Mekanism's {@code mekanism.api.Upgrade} is a fixed enum, so a third-party mod cannot register
 * a new upgrade type that Mekanism machines recognise. This item therefore follows the visual and
 * recipe pattern of a Mekanism upgrade but is installed into the add-on's generator only. It is
 * stored inside the block entity and can be removed again.
 */
public class ContainmentUpgradeItem extends Item {

    public ContainmentUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                        "item.infwhat_mekanism.upgrade_containment.effect")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        "item.infwhat_mekanism.upgrade_containment.usage")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
