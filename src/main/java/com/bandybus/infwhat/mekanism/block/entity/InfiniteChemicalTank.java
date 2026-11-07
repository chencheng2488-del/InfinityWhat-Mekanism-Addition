package com.bandybus.infwhat.mekanism.block.entity;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * Synthetic, non-persisted chemical tank used by every chemical tap machine.
 *
 * <p>It reports an effectively infinite amount of the machine's selected (or imported) chemical
 * and limits every extraction to the machine's per-tick budget. Selection driven taps reject all
 * inserts; import driven taps accept any valid chemical and lock onto its type, which is what
 * turns them into an infinite source.
 */
public class InfiniteChemicalTank implements IChemicalTank {

    private final ChemicalTapTileEntity owner;

    public InfiniteChemicalTank(ChemicalTapTileEntity owner) {
        this.owner = owner;
    }

    @Override
    public ChemicalStack getStack() {
        Chemical chemical = this.owner.getSelectedChemical();
        if (chemical == null || !this.owner.isChemicalPermitted()) {
            return ChemicalStack.EMPTY;
        }
        return new ChemicalStack(chemical.getAsHolder(),
                ChemicalTapTileEntity.REPORTED_AMOUNT);
    }

    @Override
    public void setStack(@NotNull ChemicalStack stack) {
        if (!stack.isEmpty()) {
            this.owner.lockChemical(stack.getChemical());
        }
    }

    @Override
    public void setStackUnchecked(@NotNull ChemicalStack stack) {
        setStack(stack);
    }

    @Override
    public ChemicalStack insert(@NotNull ChemicalStack stack, @NotNull Action action,
                                @NotNull AutomationType automationType) {
        if (stack.isEmpty() || !this.owner.isFillMode() || !isValid(stack)) {
            //Selection driven taps are pure sources and never accept chemicals.
            return stack;
        }
        if (action.execute()) {
            this.owner.lockChemical(stack.getChemical());
        }
        //Like the infinite fluid generator the whole offered amount is consumed: the stored
        //chemical only records which type the machine locked onto.
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extract(long amount, @NotNull Action action,
                                 @NotNull AutomationType automationType) {
        Chemical chemical = this.owner.getSelectedChemical();
        if (chemical == null || amount <= 0 || !this.owner.isChemicalPermitted()
                || !this.owner.isProviding()) {
            return ChemicalStack.EMPTY;
        }
        long allowed = this.owner.takeBudget(amount);
        if (allowed <= 0) {
            return ChemicalStack.EMPTY;
        }
        if (action.execute()) {
            this.owner.consumeBudget(allowed);
        }
        return new ChemicalStack(chemical.getAsHolder(), allowed);
    }

    @Override
    public long getCapacity() {
        return ChemicalTapTileEntity.REPORTED_AMOUNT;
    }

    @Override
    public boolean isValid(@NotNull ChemicalStack stack) {
        if (stack.isEmpty() || !this.owner.isFillMode()) {
            return false;
        }
        Chemical stored = this.owner.getSelectedChemical();
        return stored == null || stack.is(stored);
    }

    @Override
    public void onContentsChanged() {
    }

    @Override
    public CompoundTag serializeNBT(@NotNull HolderLookup.Provider provider) {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(@NotNull HolderLookup.Provider provider,
                               @NotNull CompoundTag nbt) {
        //The reported content is derived from the machine state, nothing to restore here.
    }
}
