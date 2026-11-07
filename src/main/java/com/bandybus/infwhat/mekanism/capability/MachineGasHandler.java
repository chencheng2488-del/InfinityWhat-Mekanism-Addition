package com.bandybus.infwhat.mekanism.capability;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.gas.IGasTank;

/**
 * Exposes the machines' internal gas tanks through Mekanism's gas capability.
 *
 * <p>Automation is charged as {@link AutomationType#EXTERNAL}, so an input tank accepts inserts
 * only and an output tank accepts extractions only, matching the tank builders used by the
 * machines.
 */
public final class MachineGasHandler implements IGasHandler {

    private final IGasTank[] tanks;

    public MachineGasHandler(IGasTank... tanks) {
        this.tanks = tanks;
    }

    private boolean isValidIndex(int tank) {
        return tank >= 0 && tank < this.tanks.length;
    }

    @Override
    public int getTanks() {
        return this.tanks.length;
    }

    @Override
    public GasStack getChemicalInTank(int tank) {
        return isValidIndex(tank) ? this.tanks[tank].getStack() : GasStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, GasStack stack) {
        if (isValidIndex(tank)) {
            this.tanks[tank].setStack(stack);
        }
    }

    @Override
    public long getTankCapacity(int tank) {
        return isValidIndex(tank) ? this.tanks[tank].getCapacity() : 0L;
    }

    @Override
    public boolean isValid(int tank, GasStack stack) {
        return isValidIndex(tank) && this.tanks[tank].isValid(stack);
    }

    @Override
    public GasStack insertChemical(int tank, GasStack stack, Action action) {
        if (!isValidIndex(tank) || stack.isEmpty()) {
            return stack;
        }
        return this.tanks[tank].insert(stack, action, AutomationType.EXTERNAL);
    }

    @Override
    public GasStack extractChemical(int tank, long amount, Action action) {
        if (!isValidIndex(tank) || amount <= 0) {
            return GasStack.EMPTY;
        }
        return this.tanks[tank].extract(amount, action, AutomationType.EXTERNAL);
    }

    @Override
    public GasStack getEmptyStack() {
        return GasStack.EMPTY;
    }
}
