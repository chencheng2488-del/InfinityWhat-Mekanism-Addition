package com.bandybus.infwhat.mekanism.block;

import com.bandybus.infwhat.mekanism.block.entity.UnknownRadiationTankBlockEntity;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Storage tank for the unknown radiation substance.
 *
 * <p>Unlike Mekanism's tanks this one accepts radioactive chemicals, and breaking it drops the
 * tank together with its contents instead of spilling them into the world.
 */
public class UnknownRadiationTankBlock extends BlockTile<UnknownRadiationTankBlockEntity,
        BlockTypeTile<UnknownRadiationTankBlockEntity>> {

    public UnknownRadiationTankBlock(BlockTypeTile<UnknownRadiationTankBlockEntity> type,
                                     UnaryOperator<Properties> properties) {
        super(type, properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack dropped = new ItemStack(
                com.bandybus.infwhat.mekanism.block.ModMekMachines
                        .UNKNOWN_RADIATION_TANK.getSecondary());
        if (blockEntity instanceof UnknownRadiationTankBlockEntity tank) {
            tank.writeToItem(dropped);
            UUID owner = tank.getOwnerUUID();
            if (owner != null) {
                //Mekanism's own drop path copies the owner onto the item; our custom gas carrying
                //drop has to do the same, otherwise a picked up tank always looks unowned.
                dropped.getCapability(Capabilities.OWNER_OBJECT)
                        .ifPresent(ownerObject -> ownerObject.setOwnerUUID(owner));
            }
        }
        return List.of(dropped);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof UnknownRadiationTankBlockEntity tank) {
            tank.readFromItem(stack);
        }
    }
}
