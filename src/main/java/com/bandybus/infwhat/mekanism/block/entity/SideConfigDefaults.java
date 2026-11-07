package com.bandybus.infwhat.mekanism.block.entity;

import mekanism.api.RelativeSide;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;

/**
 * Applies default side modes for 1.21.1 machines.
 *
 * <p>In Mekanism 10.7 the {@code setupInputConfig}/{@code setupOutputConfig} helpers only register
 * the slot information; unlike 10.5 they no longer assign a {@link DataType} to each side, so a
 * freshly placed machine shows every side as "none". This helper restores the expected defaults
 * explicitly.
 */
final class SideConfigDefaults {

    private SideConfigDefaults() {
    }

    static void setAll(TileComponentConfig config, TransmissionType type, DataType dataType) {
        ConfigInfo info = config.getConfig(type);
        if (info != null) {
            for (RelativeSide side : RelativeSide.values()) {
                info.setDataType(dataType, side);
            }
        }
    }
}
