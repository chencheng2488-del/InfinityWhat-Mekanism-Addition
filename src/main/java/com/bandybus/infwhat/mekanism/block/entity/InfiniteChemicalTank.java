package com.bandybus.infwhat.mekanism.block.entity;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.chemical.infuse.IInfusionTank;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.IPigmentTank;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.ISlurryTank;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Synthetic, non-persisted chemical tank used by every chemical tap machine.
 *
 * <p>It reports an effectively infinite amount of the machine's selected (or imported) chemical
 * and limits every extraction to the machine's per-tick budget. Selection driven taps reject all
 * inserts; import driven taps accept any valid chemical and lock onto its type, which is what
 * turns them into an infinite source.
 *
 * <p>Mekanism's per-type tank interfaces only add default methods on top of the generic
 * {@link IChemicalTank}, so the implementation lives here and the typed subclasses at the bottom
 * of this file simply satisfy the concrete interface.
 */
public class InfiniteChemicalTank<C extends Chemical<C>, S extends ChemicalStack<C>>
        implements IChemicalTank<C, S> {

    private final ChemicalTapTileEntity<?, ?, ?> owner;
    private final ChemicalType chemicalType;

    public InfiniteChemicalTank(ChemicalTapTileEntity<?, ?, ?> owner, ChemicalType chemicalType) {
        this.owner = owner;
        this.chemicalType = chemicalType;
    }

    /** Builds a typed stack of this tank's chemical type. */
    protected S makeStack(@NotNull C chemical, long amount) {
        throw new UnsupportedOperationException("Typed tank subclass must implement makeStack");
    }

    /** The empty stack of this tank's chemical type. */
    protected S empty() {
        throw new UnsupportedOperationException("Typed tank subclass must implement empty");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private C permittedChemical() {
        Chemical<?> selected = this.owner.getSelectedChemical();
        if (selected == null || !this.chemicalType.isInstance(selected)
                || !this.owner.isChemicalPermitted()) {
            return null;
        }
        return (C) selected;
    }

    @Override
    public S createStack(S stack, long amount) {
        return makeStack(stack.getType(), amount);
    }

    @Override
    public S getStack() {
        C chemical = permittedChemical();
        return chemical == null ? empty()
                : makeStack(chemical, ChemicalTapTileEntity.REPORTED_AMOUNT);
    }

    @Override
    public void setStack(S stack) {
        if (!stack.isEmpty()) {
            this.owner.lockChemical(stack.getType());
        }
    }

    @Override
    public void setStackUnchecked(S stack) {
        setStack(stack);
    }

    @Override
    public S insert(S stack, Action action, AutomationType automationType) {
        if (stack.isEmpty() || !this.owner.isFillMode() || !isValid(stack)) {
            //Selection driven taps are pure sources and never accept chemicals.
            return stack;
        }
        if (action.execute()) {
            this.owner.lockChemical(stack.getType());
        }
        //Like the infinite fluid generator the whole offered amount is consumed: the stored
        //chemical only records which type the machine locked onto.
        return empty();
    }

    @Override
    public S extract(long amount, Action action, AutomationType automationType) {
        C chemical = permittedChemical();
        if (chemical == null || amount <= 0 || !this.owner.isProviding()) {
            return empty();
        }
        long allowed = this.owner.takeBudget(amount);
        if (allowed <= 0) {
            return empty();
        }
        if (action.execute()) {
            this.owner.consumeBudget(allowed);
        }
        return makeStack(chemical, allowed);
    }

    @Override
    public long getCapacity() {
        return ChemicalTapTileEntity.REPORTED_AMOUNT;
    }

    @Override
    public boolean isValid(S stack) {
        if (stack.isEmpty() || !this.owner.isFillMode()) {
            return false;
        }
        C stored = permittedChemical();
        return stored == null || stack.isTypeEqual(stored);
    }

    @Override
    public void onContentsChanged() {
    }

    @Override
    public S getEmptyStack() {
        return empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        //The reported content is derived from the machine state, nothing to restore here.
    }

    public static final class GasTank extends InfiniteChemicalTank<Gas, GasStack> implements IGasTank {

        public GasTank(ChemicalTapTileEntity<?, ?, ?> owner) {
            super(owner, ChemicalType.GAS);
        }

        @Override
        protected GasStack makeStack(Gas chemical, long amount) {
            return new GasStack(chemical, amount);
        }

        @Override
        protected GasStack empty() {
            return GasStack.EMPTY;
        }
    }

    public static final class PigmentTank extends InfiniteChemicalTank<Pigment, PigmentStack>
            implements IPigmentTank {

        public PigmentTank(ChemicalTapTileEntity<?, ?, ?> owner) {
            super(owner, ChemicalType.PIGMENT);
        }

        @Override
        protected PigmentStack makeStack(Pigment chemical, long amount) {
            return new PigmentStack(chemical, amount);
        }

        @Override
        protected PigmentStack empty() {
            return PigmentStack.EMPTY;
        }
    }

    public static final class InfusionTank extends InfiniteChemicalTank<InfuseType, InfusionStack>
            implements IInfusionTank {

        public InfusionTank(ChemicalTapTileEntity<?, ?, ?> owner) {
            super(owner, ChemicalType.INFUSION);
        }

        @Override
        protected InfusionStack makeStack(InfuseType chemical, long amount) {
            return new InfusionStack(chemical, amount);
        }

        @Override
        protected InfusionStack empty() {
            return InfusionStack.EMPTY;
        }
    }

    public static final class SlurryTank extends InfiniteChemicalTank<Slurry, SlurryStack>
            implements ISlurryTank {

        public SlurryTank(ChemicalTapTileEntity<?, ?, ?> owner) {
            super(owner, ChemicalType.SLURRY);
        }

        @Override
        protected SlurryStack makeStack(Slurry chemical, long amount) {
            return new SlurryStack(chemical, amount);
        }

        @Override
        protected SlurryStack empty() {
            return SlurryStack.EMPTY;
        }
    }
}
