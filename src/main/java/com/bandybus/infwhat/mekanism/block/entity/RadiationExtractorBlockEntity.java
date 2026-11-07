package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.mekanism.block.ModBlocks;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.bandybus.infwhat.mekanism.gas.ModChemicals;
import com.bandybus.infwhat.mekanism.item.ModItems;
import com.bandybus.infwhat.mekanism.item.SaturatedRadiationSpongeItem;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.SerializationConstants;
import mekanism.api.Upgrade;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.inventory.slot.chemical.ChemicalInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Radiation Extractor: consumes a saturated Radiation Sponge and returns a fresh, reusable
 * Radiation Sponge block item while filling its output tank with unknown radiation substance.
 *
 * <p>The yield is read from the sponge's stored player yield when present; otherwise the
 * absorbed radiation magnitude is converted to an equivalent amount of unknown radiation and
 * passed through the logistic yield curve. The machine itself is a Mekanism configurable
 * machine, so upgrades, side configuration, security, redstone control and auto-ejection all
 * use Mekanism's own systems.
 */
public class RadiationExtractorBlockEntity extends TileEntityConfigurableMachine {

    public static final int BASE_TICKS_REQUIRED = 40;
    public static final long ENERGY_USAGE = 250L;
    public static final long ENERGY_CAPACITY = 50_000L;

    private static long tankCapacity() {
        return MekanismAdditionConfig.extractorTankCapacity();
    }

    private MachineEnergyContainer<RadiationExtractorBlockEntity> energyContainer;
    private IChemicalTank outputTank;
    private InputInventorySlot inputSlot;
    private OutputInventorySlot outputSlot;
    private ChemicalInventorySlot chemicalOutputSlot;
    private EnergyInventorySlot energySlot;

    private int operatingTicks;
    public int ticksRequired = BASE_TICKS_REQUIRED;

    public RadiationExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModMekMachines.RADIATION_EXTRACTOR, pos, state);
        configComponent.setupItemIOConfig(List.of(inputSlot),
                List.of(outputSlot, chemicalOutputSlot), energySlot, false);
        configComponent.setupOutputConfig(TransmissionType.CHEMICAL, outputTank,
                RelativeSide.values());
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);
        SideConfigDefaults.setAll(configComponent, TransmissionType.ITEM, DataType.INPUT_OUTPUT);
        SideConfigDefaults.setAll(configComponent, TransmissionType.ENERGY, DataType.INPUT);
        SideConfigDefaults.setAll(configComponent, TransmissionType.CHEMICAL, DataType.OUTPUT);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM,
                TransmissionType.CHEMICAL);
    }

    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this);
        builder.addTank(outputTank = BasicChemicalTank.output(tankCapacity(), listener));
        return builder.build();
    }

    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        builder.addSlot(inputSlot = InputInventorySlot.at(
                stack -> stack.is(ModItems.SATURATED_RADIATION_SPONGE.get()), listener, 29, 25));
        builder.addSlot(outputSlot = OutputInventorySlot.at(listener, 89, 25));
        builder.addSlot(chemicalOutputSlot = ChemicalInventorySlot.drain(outputTank, listener, 152, 55));
        chemicalOutputSlot.setSlotType(ContainerSlotType.OUTPUT);
        chemicalOutputSlot.setSlotOverlay(SlotOverlay.PLUS);
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer,
                this::getLevel, listener, 152, 25));
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdate = super.onUpdateServer();
        chemicalOutputSlot.drainTank();
        energySlot.fillContainerOrConvert();

        if (!MekanismAdditionConfig.radiationMachinesEnabled()
                || !canFunction()) {
            stop();
            return sendUpdate;
        }

        ItemStack input = inputSlot.getStack();
        if (input.isEmpty() || !input.is(ModItems.SATURATED_RADIATION_SPONGE.get())) {
            stop();
            return sendUpdate;
        }

        Chemical requiredChemical = ModChemicals.UNKNOWN_RADIATION.get();
        long chemicalAmount = chemicalAmountFor(input);
        if (!canAcceptOutput()
                || (!outputTank.isEmpty()
                && outputTank.getStack().getChemical() != requiredChemical)
                || outputTank.getNeeded() < chemicalAmount) {
            stop();
            return sendUpdate;
        }

        long usage = energyContainer.getEnergyPerTick();
        if (energyContainer.getEnergy() < usage) {
            stop();
            return sendUpdate;
        }

        energyContainer.extract(usage, Action.EXECUTE, AutomationType.INTERNAL);
        setActive(true);
        operatingTicks++;
        markForSave();

        if (operatingTicks >= ticksRequired) {
            finishOperation(requiredChemical, chemicalAmount);
        }
        return sendUpdate;
    }

    private void finishOperation(Chemical requiredChemical, long chemicalAmount) {
        ItemStack current = inputSlot.getStack();
        if (current.isEmpty() || !current.is(ModItems.SATURATED_RADIATION_SPONGE.get())) {
            //The player removed the sponge while the operation was running.
            operatingTicks = 0;
            markForSave();
            return;
        }
        operatingTicks = 0;
        inputSlot.shrinkStack(1, Action.EXECUTE);
        outputSlot.insertItem(new ItemStack(ModBlocks.RADIATION_SPONGE_ITEM.get()),
                Action.EXECUTE, AutomationType.INTERNAL);
        outputTank.insert(new ChemicalStack(requiredChemical.getAsHolder(), chemicalAmount),
                Action.EXECUTE, AutomationType.INTERNAL);
        markForSave();
    }

    private void stop() {
        if (operatingTicks != 0) {
            operatingTicks = 0;
            markForSave();
        }
        setActive(false);
    }

    /** Chemical amount a saturated sponge produces with the current configuration. */
    public static long chemicalAmountFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        double playerYield = SaturatedRadiationSpongeItem.getPlayerYield(stack);
        if (playerYield > 0.0D) {
            return Math.min(tankCapacity(), Math.max(1L, Math.round(playerYield)));
        }
        double radiation = SaturatedRadiationSpongeItem.getRadiation(stack);
        double strength = Math.max(1.0E-6D, MekanismAdditionConfig.unknownRadiationStrength());
        double equivalent = radiation / strength;
        double equilibrium = Math.max(1.0D, MekanismAdditionConfig.yieldEquilibrium());
        double yield;
        if (equivalent <= equilibrium) {
            //Logistic growth: below K every cycle multiplies, at K it is stable.
            yield = equivalent * (2.0D - equivalent / equilibrium);
        } else {
            //Overload decays smoothly towards K instead of collapsing to zero.
            double overflow = (equivalent - equilibrium) / equilibrium;
            yield = equilibrium / (1.0D + MekanismAdditionConfig.overloadSteepness()
                    * overflow * overflow);
        }
        return Math.min(tankCapacity(), Math.max(1L, Math.round(yield)));
    }

    public MachineEnergyContainer<RadiationExtractorBlockEntity> getEnergyContainer() {
        return energyContainer;
    }

    public IChemicalTank getOutputTank() {
        return outputTank;
    }

    public InputInventorySlot getInputSlot() {
        return inputSlot;
    }

    public OutputInventorySlot getOutputSlot() {
        return outputSlot;
    }

    /**
     * Whether the output slot can take one more sponge. The slot stacks with what is already
     * inside, so an occupied slot only blocks the machine once it is actually full.
     */
    public boolean canAcceptOutput() {
        return outputSlot.insertItem(new ItemStack(ModBlocks.RADIATION_SPONGE_ITEM.get()),
                Action.SIMULATE, AutomationType.INTERNAL).isEmpty();
    }

    public int getOperatingTicks() {
        return operatingTicks;
    }

    public int getTicksRequired() {
        return ticksRequired;
    }

    public double getScaledProgress() {
        return operatingTicks / (double) ticksRequired;
    }

    @NotNull
    @Override
    public Component getName() {
        return Component.translatable("container.infwhat_mekanism.radiation_extractor");
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED) {
            ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
        }
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(this::getOperatingTicks, this::setOperatingTicks));
        container.track(SyncableInt.create(this::getTicksRequired,
                value -> ticksRequired = value));
    }

    private void setOperatingTicks(int ticks) {
        this.operatingTicks = ticks;
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt(SerializationConstants.PROGRESS, operatingTicks);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        operatingTicks = Math.max(0, tag.getInt(SerializationConstants.PROGRESS));
    }
}
