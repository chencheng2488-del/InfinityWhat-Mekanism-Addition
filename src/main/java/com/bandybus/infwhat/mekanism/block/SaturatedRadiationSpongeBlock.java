package com.bandybus.infwhat.mekanism.block;

import com.bandybus.infwhat.mekanism.block.entity.ModBlockEntities;
import com.bandybus.infwhat.mekanism.block.entity.RadiationSpongeBlockEntity;
import com.bandybus.infwhat.mekanism.item.SaturatedRadiationSpongeItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The used up state of the Radiation Sponge.
 *
 * <p>It is a separate block (like vanilla's wet sponge) so that looking at it shows the correct
 * name instead of the still working sponge. The absorbed chunk/radiation data lives in the shared
 * {@link RadiationSpongeBlockEntity} so the drop and the radiation extractor keep working.
 */
public class SaturatedRadiationSpongeBlock extends BaseEntityBlock {

    public static final MapCodec<SaturatedRadiationSpongeBlock> CODEC =
            simpleCodec(SaturatedRadiationSpongeBlock::new);

    public SaturatedRadiationSpongeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return List.of(RadiationSpongeBlock.createSaturatedDrop(blockEntity));
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
