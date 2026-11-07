package com.bandybus.infwhat.mekanism.block;

import com.bandybus.infwhat.mekanism.block.entity.ModBlockEntities;
import com.bandybus.infwhat.mekanism.block.entity.RadiationSpongeBlockEntity;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.bandybus.infwhat.mekanism.item.ModItems;
import com.bandybus.infwhat.mekanism.item.SaturatedRadiationSpongeItem;
import mekanism.api.radiation.capability.IRadiationEntity;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.lib.radiation.RadiationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * One-shot radiation cleaner. Placing it in a radioactive area removes the radiation sources in
 * the configured chunk radius, then turns into the saturated state and never works again.
 */
public class RadiationSpongeBlock extends BaseEntityBlock {

    public static final BooleanProperty SATURATED = BooleanProperty.create("saturated");

    public RadiationSpongeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SATURATED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SATURATED);
    }

    /**
     * Right-clicking a working sponge with a radioactive player drains the player's accumulated
     * dose and saturates the sponge. The dose is stored as absorption data together with a
     * pre-computed logarithmic yield, which the radiation extractor uses directly instead of
     * the environmental yield curve.
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(SATURATED)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!MekanismAdditionConfig.radiationSpongeEnabled()
                || !MekanismAdditionConfig.spongeClearsPlayerRadiation()) {
            return InteractionResult.PASS;
        }
        IRadiationEntity radiation = player.getCapability(Capabilities.RADIATION_ENTITY)
                .resolve().orElse(null);
        if (radiation == null || radiation.getRadiation() <= RadiationManager.BASELINE) {
            return InteractionResult.PASS;
        }
        double dose = radiation.getRadiation();
        radiation.set(0);
        int chunks = (int) Math.max(1L, Math.min(1_000L,
                Math.ceil(dose * MekanismAdditionConfig.spongePlayerDoseToChunks())));
        double playerYield = Math.min(MekanismAdditionConfig.playerYieldCap(),
                Math.log1p(dose));
        level.setBlock(pos, ModBlocks.SATURATED_RADIATION_SPONGE.get().defaultBlockState(),
                Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof RadiationSpongeBlockEntity sponge) {
            sponge.setAbsorbed(chunks,
                    dose * MekanismAdditionConfig.spongePlayerDoseFactor());
            sponge.setPlayerYield(playerYield);
        }
        //Short but maxed nausea, simulating the violent vomiting caused by acute radiation.
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,
                MekanismAdditionConfig.spongeNauseaTicks(),
                MekanismAdditionConfig.spongeNauseaAmplifier(), false, false, true));
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                1.0F, 1.0F);
        return InteractionResult.SUCCESS;
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

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.RADIATION_SPONGE.get(),
                RadiationSpongeBlockEntity::serverTick);
    }

    /**
     * The saturated sponge is a one-shot consumable for the radiation extractor. The absorbed chunk
     * count lives in the block entity, so the default loot table cannot reproduce the correct item.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(SATURATED)) {
            BlockEntity blockEntity =
                    params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            return List.of(createSaturatedDrop(blockEntity));
        }
        return super.getDrops(state, params);
    }

    /**
     * Ensures a player-mined saturated sponge always carries the absorbed chunk/radiation data,
     * independent of the loot-table path.
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide && state.getValue(SATURATED)) {
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
            if (!player.isCreative()) {
                Block.popResource(level, pos, createSaturatedDrop(blockEntity));
            }
            return;
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    public static ItemStack createSaturatedDrop(@Nullable BlockEntity blockEntity) {
        int chunks = blockEntity instanceof RadiationSpongeBlockEntity sponge
                ? sponge.getAbsorbedChunks() : 0;
        double radiation = blockEntity instanceof RadiationSpongeBlockEntity sponge
                ? sponge.getAbsorbedRadiation() : 0.0D;
        double playerYield = blockEntity instanceof RadiationSpongeBlockEntity sponge
                ? sponge.getPlayerYield() : 0.0D;
        ItemStack stack = new ItemStack(ModItems.SATURATED_RADIATION_SPONGE.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(SaturatedRadiationSpongeItem.TAG_CHUNKS, chunks);
        tag.putDouble(SaturatedRadiationSpongeItem.TAG_RADIATION, radiation);
        tag.putDouble(SaturatedRadiationSpongeItem.TAG_PLAYER_YIELD, playerYield);
        return stack;
    }
}
