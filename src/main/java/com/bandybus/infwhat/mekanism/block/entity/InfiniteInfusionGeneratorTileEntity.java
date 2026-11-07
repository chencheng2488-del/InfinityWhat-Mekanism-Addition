package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.infuse.IInfusionTank;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.chemical.InfusionInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Infinite infusion generator. The player feeds an infusion convertible item (redstone, carbon,
 * fungus, diamond, refined obsidian, bio fuel...) into the slot; the machine locks onto the
 * resulting infusion type and reports an infinite amount of it. The type is released by the
 * GUI's clear button, mirroring the infinite fluid generator.
 */
public class InfiniteInfusionGeneratorTileEntity
        extends ChemicalTapTileEntity<InfuseType, InfusionStack, IInfusionTank> {

    private InfusionInventorySlot fillSlot;

    public InfiniteInfusionGeneratorTileEntity(BlockPos pos, BlockState state) {
        super(ModMekMachines.INFINITE_INFUSION_GENERATOR, pos, state,
                ChemicalType.INFUSION);
        configComponent = new TileComponentConfig(this, TransmissionType.ITEM,
                TransmissionType.INFUSION);
        configComponent.setupInputConfig(TransmissionType.ITEM, fillSlot);
        configComponent.setupIOConfig(TransmissionType.INFUSION, tank, RelativeSide.BACK);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.INFUSION);
    }

    @Override
    public boolean isFillMode() {
        return true;
    }

    @Override
    public IChemicalTankHolder<InfuseType, InfusionStack, IInfusionTank> getInitialInfusionTanks(
            IContentsListener listener) {
        ChemicalTankHelper<InfuseType, InfusionStack, IInfusionTank> builder =
                ChemicalTankHelper.forSideInfusionWithConfig(this::getDirection, this::getConfig);
        builder.addTank(tank = new InfiniteChemicalTank.InfusionTank(this));
        return builder.build();
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        //Turns convertible items (redstone, carbon, refined obsidian...) into infusion and consumes
        //them, the same way Mekanism's metallurgic infuser handles its input slot. Once a type has
        //been locked the tank reports no free space, so further items stay untouched.
        fillSlot.fillTankOrConvert();
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder =
                InventorySlotHelper.forSideWithConfig(this::getDirection, this::getConfig);
        builder.addSlot(fillSlot = InfusionInventorySlot.fillOrConvert(tank,
                this::getLevel, listener, 152, 25));
        fillSlot.setSlotType(ContainerSlotType.INPUT);
        fillSlot.setSlotOverlay(SlotOverlay.MINUS);
        return builder.build();
    }

    @NotNull
    @Override
    public Component getName() {
        return Component.translatable("container." + InfinityWhatMekanism.MOD_ID
                + ".infinite_infusion_generator");
    }
}
