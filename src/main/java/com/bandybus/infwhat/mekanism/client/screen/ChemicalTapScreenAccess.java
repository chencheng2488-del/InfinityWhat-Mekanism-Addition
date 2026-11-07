package com.bandybus.infwhat.mekanism.client.screen;

/** Common callbacks so the network handler can talk to any chemical tap screen. */
public interface ChemicalTapScreenAccess {

    int getContainerId();

    void onRateResult(boolean success, long amount);

    void onChemicalResult(boolean success);
}
