package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.bandybus.infwhat.mekanism.gas.ModChemicals;
import com.bandybus.infwhat.mekanism.item.ModItems;
import com.bandybus.infwhat.mekanism.recipe.FractionationRecipe;
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
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.functions.ConstantPredicates;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Radiation Fractionator: converts unknown radiation substance into one of Mekanism's nuclear
 * chemicals. The circuit item in the input slot selects the recipe; each circuit number maps to a
 * fixed chemical and a fixed input ratio.
 */
public class RadiationFractionatorBlockEntity extends TileEntityConfigurableMachine {

    public static final int BASE_TICKS_REQUIRED = 40;
    public static final long ENERGY_USAGE = 500L;
    public static final long ENERGY_CAPACITY = 75_000L;

    private static long tankCapacity() {
        //A tank smaller than one operation's output could never hold the result, so clamp the
        //configured value to the minimum the machine can actually work with.
        return Math.max(FractionationRecipe.OUTPUT_PER_OPERATION,
                MekanismAdditionConfig.fractionatorTankCapacity());
    }

    private MachineEnergyContainer<RadiationFractionatorBlockEntity> energyContainer;
    private IChemicalTank inputTank;
    private IChemicalTank outputTank;
    private InputInventorySlot inputSlot;
    private ChemicalInventorySlot chemicalInputSlot;
    private ChemicalInventorySlot chemicalOutputSlot;
    private EnergyInventorySlot energySlot;

    private int operatingTicks;
    public int ticksRequired = BASE_TICKS_REQUIRED;

    public RadiationFractionatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModMekMachines.RADIATION_FRACTIONATOR, pos, state);
        configComponent.setupItemIOConfig(List.of(inputSlot, chemicalInputSlot),
                List.of(chemicalOutputSlot), energySlot, false);
        configComponent.setupIOConfig(TransmissionType.CHEMICAL, inputTank, outputTank,
                RelativeSide.BACK);
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);
        SideConfigDefaults.setAll(configComponent, TransmissionType.ITEM, DataType.INPUT_OUTPUT);
        SideConfigDefaults.setAll(configComponent, TransmissionType.ENERGY, DataType.INPUT);
        SideConfigDefaults.setAll(configComponent, TransmissionType.CHEMICAL, DataType.INPUT_OUTPUT);

        ejectorComponent = new TileComponentEjector(this);
        //INPUT_OUTPUT is ejectable and ejector extraction uses INTERNAL, which would bypass the
        //input tank's notExternal predicate; restrict auto-ejection to the output tank.
        ejectorComponent.setOutputData(configComponent, TransmissionType.CHEMICAL)
                .setCanTankEject(tank -> tank == outputTank);
    }

    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this);
        //ALWAYS_ALLOW is required: Mekanism's DEFAULT attribute validator rejects every chemical
        //whose attributes need validation, and ChemicalAttributes.Radiation does exactly that while
        //radiation is enabled. Without it even our own tank refuses the unknown radiation substance.
        builder.addTank(inputTank = BasicChemicalTank.createModern(tankCapacity(),
                ConstantPredicates.notExternal(),
                ConstantPredicates.alwaysTrueBi(),
                stack -> stack.is(ModChemicals.UNKNOWN_RADIATION.get()),
                ChemicalAttributeValidator.ALWAYS_ALLOW,
                listener));
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
                stack -> stack.is(ModItems.RADIATION_CIRCUIT.get()), listener, 48, 25));
        builder.addSlot(chemicalInputSlot = ChemicalInventorySlot.fill(inputTank, listener, 152, 25));
        builder.addSlot(chemicalOutputSlot = ChemicalInventorySlot.drain(outputTank, listener, 152, 55));
        //Mark the two container slots as chemical I/O so they render like Mekanism's own slots.
        chemicalInputSlot.setSlotType(ContainerSlotType.INPUT);
        chemicalInputSlot.setSlotOverlay(SlotOverlay.MINUS);
        chemicalOutputSlot.setSlotType(ContainerSlotType.OUTPUT);
        chemicalOutputSlot.setSlotOverlay(SlotOverlay.PLUS);
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer,
                this::getLevel, listener, 152, 5));
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdate = super.onUpdateServer();
        chemicalInputSlot.fillTank();
        chemicalOutputSlot.drainTank();
        energySlot.fillContainerOrConvert();

        if (!MekanismAdditionConfig.radiationMachinesEnabled()
                || !canFunction()) {
            stop();
            return sendUpdate;
        }

        FractionationRecipe recipe = getSelectedRecipe();
        if (recipe == null) {
            stop();
            return sendUpdate;
        }
        Chemical product = recipe.getOutputChemical();
        if (product == null) {
            stop();
            return sendUpdate;
        }

        if ((!outputTank.isEmpty()
                && outputTank.getStack().getChemical() != product)
                || outputTank.getNeeded() < FractionationRecipe.OUTPUT_PER_OPERATION
                || inputTank.getStored() < recipe.getInputPerOperation()) {
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
            finishOperation(recipe, product);
        }
        return sendUpdate;
    }

    private void finishOperation(FractionationRecipe recipe, Chemical product) {
        operatingTicks = 0;
        inputTank.shrinkStack(recipe.getInputPerOperation(), Action.EXECUTE);
        outputTank.insert(new ChemicalStack(product.getAsHolder(),
                        FractionationRecipe.OUTPUT_PER_OPERATION),
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

    @Nullable
    public FractionationRecipe getSelectedRecipe() {
        return FractionationRecipe.forStack(inputSlot.getStack());
    }

    public MachineEnergyContainer<RadiationFractionatorBlockEntity> getEnergyContainer() {
        return energyContainer;
    }

    public IChemicalTank getInputTank() {
        return inputTank;
    }

    public IChemicalTank getOutputTank() {
        return outputTank;
    }

    public InputInventorySlot getInputSlot() {
        return inputSlot;
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
        return Component.translatable("container.infwhat_mekanism.radiation_fractionator");
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
