package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.mekanism.block.RadiationSpongeBlock;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.google.common.collect.Table;
import mekanism.api.Chunk3D;
import mekanism.api.radiation.IRadiationManager;
import mekanism.api.radiation.IRadiationSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server ticker for the one-shot radiation cleaner.
 *
 * <p>Radiation in Mekanism is source-based: the level is computed from the registered
 * {@link IRadiationSource} entries. Removing the sources in the configured chunk radius therefore
 * clears the current cloud immediately. Existing source blocks may register themselves again on
 * later ticks; this sponge intentionally becomes saturated after the first successful removal and
 * does not suppress the area afterwards.
 */
public class RadiationSpongeBlockEntity extends BlockEntity {

    private static final String KEY_ABSORBED_CHUNKS = "AbsorbedChunks";
    private static final String KEY_ABSORBED_RADIATION = "AbsorbedRadiation";
    private static final String KEY_PLAYER_YIELD = "PlayerYield";

    private int tickCounter;

    /**
     * Number of chunks that actually contained radiation sources removed by this sponge. Kept for
     * the item tooltip and for backwards compatibility; the yield curve no longer uses it.
     */
    private int absorbedChunks;

    /** Total magnitude of the radiation sources removed by this sponge. */
    private double absorbedRadiation;

    /**
     * Pre-computed yield of a sponge that absorbed a player's dose. Zero for sponges that absorbed
     * environmental radiation.
     */
    private double playerYield;

    public RadiationSpongeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIATION_SPONGE.get(), pos, state);
    }

    public int getAbsorbedChunks() {
        return this.absorbedChunks;
    }

    public double getAbsorbedRadiation() {
        return this.absorbedRadiation;
    }

    public double getPlayerYield() {
        return this.playerYield;
    }

    /** Restores the absorbed data, used when the drop is placed back as the saturated block. */
    public void setAbsorbed(int chunks, double radiation) {
        this.absorbedChunks = Math.max(0, chunks);
        this.absorbedRadiation = Math.max(0.0D, radiation);
        this.playerYield = 0.0D;
        setChanged();
    }

    /** Stores the pre-computed yield of a player-dose absorption. */
    public void setPlayerYield(double yield) {
        this.playerYield = Math.max(0.0D, yield);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  RadiationSpongeBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        if (state.getValue(RadiationSpongeBlock.SATURATED)) {
            //Old worlds stored the saturated sponge as a block state; move it to the dedicated
            //block so it shows the correct name from now on.
            level.setBlock(pos, com.bandybus.infwhat.mekanism.block.ModBlocks
                    .SATURATED_RADIATION_SPONGE.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof RadiationSpongeBlockEntity sponge) {
                sponge.setAbsorbed(blockEntity.getAbsorbedChunks(),
                        blockEntity.getAbsorbedRadiation());
            }
            return;
        }
        if (!MekanismAdditionConfig.radiationSpongeEnabled()) {
            return;
        }
        if (level.getServer() != null && level.getServer().isDedicatedServer()
                && !MekanismAdditionConfig.allowRadiationSpongeOnDedicatedServer()) {
            return;
        }
        if ((blockEntity.tickCounter++ % 20) != 0) {
            return;
        }

        IRadiationManager manager = IRadiationManager.INSTANCE;
        if (!manager.isRadiationEnabled()) {
            return;
        }
        if (manager.getRadiationLevel(GlobalPos.of(level.dimension(), pos))
                < MekanismAdditionConfig.radiationSpongeMinRadiation()) {
            return;
        }

        int radius = Math.max(0, Math.min(8, MekanismAdditionConfig.radiationSpongeRadiusChunks()));
        Chunk3D center = new Chunk3D(level.dimension(), new ChunkPos(pos));
        Set<Chunk3D> chunks = center.expand(radius);
        Table<Chunk3D, GlobalPos, IRadiationSource> sources = manager.getRadiationSources();

        int removed = 0;
        int absorbedChunks = 0;
        double absorbedRadiation = 0;
        double minRadiation = MekanismAdditionConfig.radiationSpongeMinRadiation();
        List<Chunk3D> chunksToClear = new ArrayList<>();
        //First pass: evaluate every chunk while all sources are still registered. Removing a
        //source early would drop the exposure of the neighbouring chunks we have not sampled yet.
        for (Chunk3D chunk : chunks) {
            Map<GlobalPos, IRadiationSource> chunkSources = sources.row(chunk);
            int count = chunkSources.size();
            if (count > 0) {
                for (IRadiationSource source : chunkSources.values()) {
                    absorbedRadiation += source.getMagnitude();
                }
                chunksToClear.add(chunk);
                removed += count;
                absorbedChunks++;
                continue;
            }
            //A chunk with no source still counts when the cloud spreads into it: Mekanism only
            //registers the source in its own chunk even though its exposure covers several.
            GlobalPos sample = GlobalPos.of(chunk.dimension,
                    new BlockPos(chunk.getMinBlockX() + 8, pos.getY(), chunk.getMinBlockZ() + 8));
            if (manager.getRadiationLevel(sample) > minRadiation) {
                absorbedChunks++;
            }
        }
        //Second pass: clear the chunks that actually held sources.
        for (Chunk3D chunk : chunksToClear) {
            manager.removeRadiationSources(chunk);
        }

        if (removed > 0) {
            blockEntity.absorbedChunks = absorbedChunks;
            blockEntity.absorbedRadiation = absorbedRadiation;
            blockEntity.setChanged();
            //Swap to the dedicated saturated block so it reads as "Saturated Radiation Sponge"
            //instead of still being the working sponge, and hand the absorbed data to the new
            //block entity created by that block.
            level.setBlock(pos, com.bandybus.infwhat.mekanism.block.ModBlocks
                    .SATURATED_RADIATION_SPONGE.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof RadiationSpongeBlockEntity sponge) {
                sponge.setAbsorbed(absorbedChunks, absorbedRadiation);
            }
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                    1.0F, 1.0F);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(KEY_ABSORBED_CHUNKS, this.absorbedChunks);
        tag.putDouble(KEY_ABSORBED_RADIATION, this.absorbedRadiation);
        tag.putDouble(KEY_PLAYER_YIELD, this.playerYield);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.absorbedChunks = Math.max(0, tag.getInt(KEY_ABSORBED_CHUNKS));
        this.absorbedRadiation = Math.max(0.0D, tag.getDouble(KEY_ABSORBED_RADIATION));
        this.playerYield = Math.max(0.0D, tag.getDouble(KEY_PLAYER_YIELD));
    }
}
