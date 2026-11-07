package com.bandybus.infwhat.mekanism.block;

import com.bandybus.infwhat.mekanism.block.entity.GasTapBlockEntity;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.UnaryOperator;

/** Gas Tap block; all interaction is handled by Mekanism's standard machine behaviour. */
public class GasTapBlockTile extends BlockTile<GasTapBlockEntity,
        BlockTypeTile<GasTapBlockEntity>> {

    public GasTapBlockTile(BlockTypeTile<GasTapBlockEntity> type,
                           UnaryOperator<BlockBehaviour.Properties> propertiesModifier) {
        super(type, propertiesModifier);
    }
}
