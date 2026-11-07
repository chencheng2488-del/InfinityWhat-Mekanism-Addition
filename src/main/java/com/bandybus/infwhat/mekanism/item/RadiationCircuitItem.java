package com.bandybus.infwhat.mekanism.item;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.recipe.FractionationRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A reusable selector for the radiation fractionator. Each stack stores a circuit number in its
 * NBT, and right-clicking cycles through the six available recipes.
 */
public class RadiationCircuitItem extends Item {

    public static final String TAG_CIRCUIT = "Circuit";
    public static final int MIN_CIRCUIT = 1;
    public static final int MAX_CIRCUIT = 6;

    public RadiationCircuitItem(Properties properties) {
        super(properties);
    }

    public static int getCircuit(ItemStack stack) {
        if (stack.isEmpty() || stack.getTag() == null || !stack.getTag().contains(TAG_CIRCUIT)) {
            return MIN_CIRCUIT;
        }
        return Math.max(MIN_CIRCUIT, Math.min(MAX_CIRCUIT, stack.getTag().getInt(TAG_CIRCUIT)));
    }

    public static void setCircuit(ItemStack stack, int circuit) {
        int clamped = Math.max(MIN_CIRCUIT, Math.min(MAX_CIRCUIT, circuit));
        stack.getOrCreateTag().putInt(TAG_CIRCUIT, clamped);
    }

    /** Advances the circuit number, wrapping around at both ends. */
    public static int cycle(ItemStack stack, boolean backwards) {
        int current = getCircuit(stack);
        int next = backwards ? current - 1 : current + 1;
        if (next > MAX_CIRCUIT) {
            next = MIN_CIRCUIT;
        } else if (next < MIN_CIRCUIT) {
            next = MAX_CIRCUIT;
        }
        setCircuit(stack, next);
        return next;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int circuit = cycle(stack, player.isShiftKeyDown());
            FractionationRecipe recipe = FractionationRecipe.byCircuit(circuit);
            player.displayClientMessage(Component.translatable(
                            "item." + InfinityWhatMekanism.MOD_ID + ".radiation_circuit.selected",
                            circuit,
                            recipe == null ? Component.empty() : recipe.getDisplayName())
                    .withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int circuit = getCircuit(stack);
        FractionationRecipe recipe = FractionationRecipe.byCircuit(circuit);
        tooltip.add(Component.translatable(
                        "item." + InfinityWhatMekanism.MOD_ID + ".radiation_circuit.current",
                        circuit,
                        recipe == null ? Component.empty() : recipe.getDisplayName())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                        "item." + InfinityWhatMekanism.MOD_ID + ".radiation_circuit.usage")
                .withStyle(ChatFormatting.GRAY));
    }
}
