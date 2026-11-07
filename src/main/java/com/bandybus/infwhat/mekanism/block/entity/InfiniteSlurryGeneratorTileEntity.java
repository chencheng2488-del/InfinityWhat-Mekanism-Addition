package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.slurry.ISlurryTank;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Infinite slurry generator. Mekanism has no slurry container item, so slurry is imported
 * through the pressurized tube / adjacent capability (the back face is the default output,
 * every other face accepts input). The GUI's clear button releases the locked type, mirroring
 * the infinite fluid generator.
 */
public class InfiniteSlurryGeneratorTileEntity
        extends ChemicalTapTileEntity<Slurry, SlurryStack, ISlurryTank> {

    private int importCooldown;

    public InfiniteSlurryGeneratorTileEntity(BlockPos pos, BlockState state) {
        super(ModMekMachines.INFINITE_SLURRY_GENERATOR, pos, state, ChemicalType.SLURRY);
        configComponent.setupIOConfig(TransmissionType.SLURRY, tank, RelativeSide.BACK);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.SLURRY);
    }

    @Override
    public boolean isFillMode() {
        return true;
    }

    @Override
    public IChemicalTankHolder<Slurry, SlurryStack, ISlurryTank> getInitialSlurryTanks(
            IContentsListener listener) {
        ChemicalTankHelper<Slurry, SlurryStack, ISlurryTank> builder =
                ChemicalTankHelper.forSideSlurryWithConfig(this::getDirection, this::getConfig);
        builder.addTank(tank = new InfiniteChemicalTank.SlurryTank(this));
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdate = super.onUpdateServer();
        //Mekanism has no slurry container item, so importing a type relies on an adjacent tank or
        //pipe. Pipes only push when the source machine ejects, which left this machine unable to
        //be filled from a storage tank placed next to it. Pull a single bucket from any neighbour
        //until a type is locked; the type stays locked afterwards.
        if (getSelectedChemical() != null) {
            return sendUpdate;
        }
        if (++importCooldown < 10) {
            return sendUpdate;
        }
        importCooldown = 0;
        Level level = getLevel();
        if (level == null) {
            return sendUpdate;
        }
        for (Direction side : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(side));
            if (neighbor == null) {
                continue;
            }
            var handler = level.getCapability(Capabilities.SLURRY.block(),
                    worldPosition.relative(side), side.getOpposite());
            if (handler == null) {
                continue;
            }
            SlurryStack offered = handler.extractChemical(1L, Action.SIMULATE);
            if (offered.isEmpty()) {
                continue;
            }
            if (tank.insert(offered, Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
                handler.extractChemical(1L, Action.EXECUTE);
                tank.insert(offered, Action.EXECUTE, AutomationType.INTERNAL);
                return sendUpdate;
            }
        }
        return sendUpdate;
    }

    @NotNull
    @Override
    public Component getName() {
        return Component.translatable("container." + InfinityWhatMekanism.MOD_ID
                + ".infinite_slurry_generator");
    }
}
