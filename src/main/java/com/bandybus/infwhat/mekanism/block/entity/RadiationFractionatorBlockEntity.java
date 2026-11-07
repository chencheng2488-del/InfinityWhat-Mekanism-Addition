package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.bandybus.infwhat.mekanism.gas.ModGases;
import com.bandybus.infwhat.mekanism.item.ModItems;
import com.bandybus.infwhat.mekanism.recipe.FractionationRecipe;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.NBTConstants;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.chemical.GasInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Radiation Fractionator: converts unknown radiation substance into one of Mekanism's nuclear
 * gases. The circuit item in the input slot selects the recipe; each circuit number maps to a
 * fixed gas and a fixed input ratio.
 */
public class RadiationFractionatorBlockEntity extends TileEntityConfigurableMachine {

    public static final int BASE_TICKS_REQUIRED = 40;
    public static final FloatingLong ENERGY_USAGE = FloatingLong.createConst(500);
    public static final FloatingLong ENERGY_CAPACITY = FloatingLong.createConst(75_000);

    private static long tankCapacity() {
        //A tank smaller than one operation's output could never hold the result, so clamp the
        //configured value to the minimum the machine can actually work with.
        return Math.max(FractionationRecipe.OUTPUT_PER_OPERATION,
                MekanismAdditionConfig.fractionatorTankCapacity());
    }

    private MachineEnergyContainer<RadiationFractionatorBlockEntity> energyContainer;
    private IGasTank inputTank;
    private IGasTank outputTank;
    private InputInventorySlot inputSlot;
    private GasInventorySlot gasInputSlot;
    private GasInventorySlot gasOutputSlot;
    private EnergyInventorySlot energySlot;

    private int operatingTicks;
    public int ticksRequired = BASE_TICKS_REQUIRED;

    public RadiationFractionatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModMekMachines.RADIATION_FRACTIONATOR, pos, state);
        configComponent = new TileComponentConfig(this, TransmissionType.ITEM,
                TransmissionType.GAS, TransmissionType.ENERGY);
        configComponent.setupItemIOConfig(List.of(inputSlot, gasInputSlot),
                List.of(gasOutputSlot), energySlot, false);
        configComponent.setupIOConfig(TransmissionType.GAS, inputTank, outputTank,
                RelativeSide.BACK);
        ConfigInfo gasConfig = configComponent.getConfig(TransmissionType.GAS);
        if (gasConfig != null) {
            //The two-tank setupIOConfig overload only lets the single outputSide expose the output
            //tank, so a storage tank placed on any other side cannot pull the produced gas.
            //Expose input and output on every side instead; the input tank's canExtract predicate
            //is notExternal, so it still cannot be drained from outside.
            gasConfig.setDataType(DataType.INPUT_OUTPUT, RelativeSide.values());
        }
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);

        ejectorComponent = new TileComponentEjector(this);
        //INPUT_OUTPUT is ejectable and ejector extraction uses INTERNAL, which would bypass the
        //input tank's notExternal predicate; restrict auto-ejection to the output tank.
        ejectorComponent.setOutputData(configComponent, TransmissionType.GAS)
                .setCanTankEject(tank -> tank == outputTank);
    }

    @Override
    public IChemicalTankHolder<Gas, GasStack, IGasTank> getInitialGasTanks(
            IContentsListener listener) {
        ChemicalTankHelper<Gas, GasStack, IGasTank> builder =
                ChemicalTankHelper.forSideGasWithConfig(this::getDirection, this::getConfig);
        //ALWAYS_ALLOW is required: Mekanism's DEFAULT attribute validator rejects every chemical
        //whose attributes need validation, and GasAttributes.Radiation does exactly that while
        //radiation is enabled. Without it even our own tank refuses the unknown radiation substance.
        builder.addTank(inputTank = ChemicalTankBuilder.GAS.create(tankCapacity(),
                mekanism.api.functions.ConstantPredicates.notExternal(),
                mekanism.api.functions.ConstantPredicates.alwaysTrueBi(),
                gas -> gas == ModGases.UNKNOWN_RADIATION.get(),
                mekanism.api.chemical.attribute.ChemicalAttributeValidator.ALWAYS_ALLOW,
                listener));
        builder.addTank(outputTank = ChemicalTankBuilder.GAS.output(tankCapacity(), listener));
        return builder.build();
    }

    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder =
                EnergyContainerHelper.forSideWithConfig(this::getDirection, this::getConfig);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder =
                InventorySlotHelper.forSideWithConfig(this::getDirection, this::getConfig);
        builder.addSlot(inputSlot = InputInventorySlot.at(
                stack -> stack.is(ModItems.RADIATION_CIRCUIT.get()), listener, 48, 25));
        builder.addSlot(gasInputSlot = GasInventorySlot.fill(inputTank, listener, 152, 25));
        builder.addSlot(gasOutputSlot = GasInventorySlot.drain(outputTank, listener, 152, 55));
        //Mark the two container slots as chemical I/O so they render like Mekanism's own gas slots.
        gasInputSlot.setSlotType(ContainerSlotType.INPUT);
        gasInputSlot.setSlotOverlay(SlotOverlay.MINUS);
        gasOutputSlot.setSlotType(ContainerSlotType.OUTPUT);
        gasOutputSlot.setSlotOverlay(SlotOverlay.PLUS);
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer,
                this::getLevel, listener, 152, 5));
        return builder.build();
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        gasInputSlot.fillTank();
        gasOutputSlot.drainTank();
        energySlot.fillContainerOrConvert();

        if (!MekanismAdditionConfig.radiationMachinesEnabled()
                || !MekanismUtils.canFunction(this)) {
            stop();
            return;
        }

        FractionationRecipe recipe = getSelectedRecipe();
        if (recipe == null) {
            stop();
            return;
        }
        Gas product = recipe.getOutputGas();
        if (product == null) {
            stop();
            return;
        }

        if ((!outputTank.isEmpty() && outputTank.getType() != product)
                || outputTank.getNeeded() < FractionationRecipe.OUTPUT_PER_OPERATION
                || inputTank.getStored() < recipe.getInputPerOperation()) {
            stop();
            return;
        }

        FloatingLong usage = energyContainer.getEnergyPerTick();
        if (energyContainer.getEnergy().smallerThan(usage)) {
            stop();
            return;
        }

        energyContainer.extract(usage, Action.EXECUTE, AutomationType.INTERNAL);
        setActive(true);
        operatingTicks++;
        markForSave();

        if (operatingTicks >= ticksRequired) {
            finishOperation(recipe, product);
        }
    }

    private void finishOperation(FractionationRecipe recipe, Gas product) {
        operatingTicks = 0;
        inputTank.shrinkStack(recipe.getInputPerOperation(), Action.EXECUTE);
        outputTank.insert(new GasStack(product, FractionationRecipe.OUTPUT_PER_OPERATION),
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

    public IGasTank getInputTank() {
        return inputTank;
    }

    public IGasTank getOutputTank() {
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
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(NBTConstants.PROGRESS, operatingTicks);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        operatingTicks = Math.max(0, tag.getInt(NBTConstants.PROGRESS));
        //Worlds created before the side fix saved the old layout (everything input, output only on
        //the back face), which the component config restores on load. Migrate exactly that layout
        //so an existing machine starts outputting without having to be broken and replaced.
        ConfigInfo gasConfig = configComponent.getConfig(TransmissionType.GAS);
        if (gasConfig != null) {
            boolean legacy = true;
            for (RelativeSide side : RelativeSide.values()) {
                DataType expected = side == RelativeSide.BACK ? DataType.OUTPUT : DataType.INPUT;
                if (gasConfig.getDataType(side) != expected) {
                    legacy = false;
                    break;
                }
            }
            if (legacy) {
                gasConfig.setDataType(DataType.INPUT_OUTPUT, RelativeSide.values());
            }
        }
    }
}
