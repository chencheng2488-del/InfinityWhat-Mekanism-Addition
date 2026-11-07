package com.bandybus.infwhat.mekanism.block;

import com.bandybus.infwhat.mekanism.block.entity.UnknownRadiationTankBlockEntity;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.content.blocktype.BlockTypeTile;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide) {
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
            if (!player.isCreative()) {
                Block.popResource(level, pos, createDrop(blockEntity));
            }
        }
    }

    /**
     * Explosions bypass the player break path, so the contents carrying drop is created here as
     * well and the plain loot table drop is suppressed. Forge 1.19.2 removes the block before it
     * calls {@link #wasExploded}, which clears the block entity, so the data is captured first.
     */
    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos,
                                Explosion explosion) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        super.onBlockExploded(state, level, pos, explosion);
        if (!level.isClientSide) {
            Block.popResource(level, pos, createDrop(blockEntity));
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    private static ItemStack createDrop(@Nullable BlockEntity blockEntity) {
        ItemStack dropped = new ItemStack(
                com.bandybus.infwhat.mekanism.block.ModMekMachines
                        .UNKNOWN_RADIATION_TANK.getSecondary());
        if (blockEntity instanceof UnknownRadiationTankBlockEntity tank) {
            tank.writeToItem(dropped);
            UUID owner = tank.getOwnerUUID();
            if (owner != null) {
                //Mekanism's own drop path copies the owner onto the item; our custom chemical
                //carrying drop has to do the same, otherwise a picked up tank looks unowned.
                dropped.getCapability(Capabilities.OWNER_OBJECT)
                        .ifPresent(ownerObject -> ownerObject.setOwnerUUID(owner));
            }
        }
        return dropped;
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
