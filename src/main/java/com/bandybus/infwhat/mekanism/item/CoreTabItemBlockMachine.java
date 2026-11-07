package com.bandybus.infwhat.mekanism.item;

import com.bandybus.infwhat.creative.ModCreativeTabs;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.item.interfaces.IItemSustainedInventory;
import net.minecraft.world.item.Item;

/**
 * Machine block item that reports the core Infinity What tab as its creative category.
 *
 * <p>Mekanism 1.19.2 assigns every block item to its own creative tab through
 * {@code Item.Properties}; passing our own properties is the supported way to keep the add-on
 * machines in the core tab while the unknown radiation tank stays in Mekanism's tab.
 */
public class CoreTabItemBlockMachine extends ItemBlockTooltip<BlockTile<?, ?>>
        implements IItemSustainedInventory {

    public CoreTabItemBlockMachine(BlockTile<?, ?> block) {
        //Same shape as Mekanism's ItemBlockMachine: details tooltip, stack size 1, core tab.
        super(block, true, new Item.Properties().stacksTo(1).tab(ModCreativeTabs.INFINITY_WHAT));
    }
}
