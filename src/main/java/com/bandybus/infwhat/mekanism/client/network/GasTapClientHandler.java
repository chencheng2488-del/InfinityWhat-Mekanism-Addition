package com.bandybus.infwhat.mekanism.client.network;

import com.bandybus.infwhat.mekanism.client.screen.ChemicalTapScreenAccess;
import net.minecraft.client.Minecraft;

/** Client-side packet handling that forwards to the open chemical tap screen. */
public final class GasTapClientHandler {

    private GasTapClientHandler() {
    }

    public static void handleRate(int containerId, boolean success, long amount) {
        if (Minecraft.getInstance().screen instanceof ChemicalTapScreenAccess screen
                && screen.getContainerId() == containerId) {
            screen.onRateResult(success, amount);
        }
    }

    public static void handleChemical(int containerId, boolean success) {
        if (Minecraft.getInstance().screen instanceof ChemicalTapScreenAccess screen
                && screen.getContainerId() == containerId) {
            screen.onChemicalResult(success);
        }
    }
}
