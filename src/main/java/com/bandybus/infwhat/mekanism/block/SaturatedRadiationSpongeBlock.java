package com.bandybus.infwhat.mekanism.block;

import com.bandybus.infwhat.mekanism.block.entity.ModBlockEntities;
import com.bandybus.infwhat.mekanism.block.entity.RadiationSpongeBlockEntity;
import com.bandybus.infwhat.mekanism.item.SaturatedRadiationSpongeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The used up state of the Radiation Sponge.
 *
 * <p>It is a separate block (like vanilla's wet sponge) so that looking at it shows the correct
 * name instead of the still working sponge. The absorbed chunk/radiation data lives in the shared
 * {@link RadiationSpongeBlockEntity} so the drop and the radiation extractor keep working.
 */
public class SaturatedRadiationSpongeBlock extends BaseEntityBlock {

    public SaturatedRadiationSpongeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadiationSpongeBlockEntity(pos, state);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide) {
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
            if (!player.isCreative()) {
                Block.popResource(level, pos, RadiationSpongeBlock.createSaturatedDrop(blockEntity));
            }
        }
    }

    /**
     * Explosions bypass the player break path; drop the data carrying item instead of nothing.
     * Forge 1.19.2 removes the block before it calls {@link #wasExploded}, which clears the block
     * entity, so the absorbed data is captured before the block is removed.
     */
    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos,
                                Explosion explosion) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        super.onBlockExploded(state, level, pos, explosion);
        if (!level.isClientSide) {
            Block.popResource(level, pos,
                    RadiationSpongeBlock.createSaturatedDrop(blockEntity));
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof RadiationSpongeBlockEntity sponge) {
            sponge.setAbsorbed(SaturatedRadiationSpongeItem.getChunks(stack),
                    SaturatedRadiationSpongeItem.getRadiation(stack));
            sponge.setPlayerYield(SaturatedRadiationSpongeItem.getPlayerYield(stack));
        }
    }
}
