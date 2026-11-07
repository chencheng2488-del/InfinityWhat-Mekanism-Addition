package com.bandybus.infwhat.mekanism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * A saturated Radiation Sponge. It can be placed back as the saturated block and is the input of
 * the radiation extraction line.
 */
public class SaturatedRadiationSpongeItem extends BlockItem {

    /** Component key holding the number of radiation chunks the sponge absorbed. */
    public static final String TAG_CHUNKS = "Chunks";

    /** Component key holding the total radiation magnitude the sponge absorbed. */
    public static final String TAG_RADIATION = "Radiation";

    /** Component key holding the pre-computed yield of a player-dose absorption. */
    public static final String TAG_PLAYER_YIELD = "PlayerYield";

    public SaturatedRadiationSpongeItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** Returns the absorbed chunk count stored on the stack, or zero for plain stacks. */
    public static int getChunks(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : Math.max(0, data.copyTag().getInt(TAG_CHUNKS));
    }

    /** Returns the absorbed radiation magnitude stored on the stack. */
    public static double getRadiation(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0.0D : Math.max(0.0D, data.copyTag().getDouble(TAG_RADIATION));
    }

    /** Returns the pre-computed player-dose yield, or zero for environmental sponges. */
    public static double getPlayerYield(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0.0D
                : Math.max(0.0D, data.copyTag().getDouble(TAG_PLAYER_YIELD));
    }

    /** Writes the pre-computed player-dose yield onto the stack. */
    public static void setPlayerYield(ItemStack stack, double yield) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putDouble(TAG_PLAYER_YIELD, Math.max(0.0D, yield)));
    }

    /** Writes the absorbed data onto the stack. */
    public static void setAbsorbed(ItemStack stack, int chunks, double radiation) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(TAG_CHUNKS, Math.max(0, chunks));
            tag.putDouble(TAG_RADIATION, Math.max(0.0D, radiation));
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                        "item.infwhat_mekanism.saturated_radiation_sponge.tooltip")
                .withStyle(ChatFormatting.GRAY));
        int chunks = getChunks(stack);
        if (chunks > 0) {
            tooltip.add(Component.translatable(
                            "item.infwhat_mekanism.saturated_radiation_sponge.chunks", chunks)
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        double radiation = getRadiation(stack);
        if (radiation > 0.0D) {
            tooltip.add(Component.translatable(
                            "item.infwhat_mekanism.saturated_radiation_sponge.radiation",
                            String.format(java.util.Locale.ROOT, "%.2f", radiation))
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
