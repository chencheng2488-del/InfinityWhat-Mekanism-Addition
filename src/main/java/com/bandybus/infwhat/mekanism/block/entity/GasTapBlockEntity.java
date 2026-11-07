package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.bandybus.infwhat.mekanism.item.ModItems;
import mekanism.api.IContentsListener;
import mekanism.api.MekanismAPI;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.chemical.pigment.IPigmentTank;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Infinite chemical generator: sources gases and pigments from the registry.
 *
 * <p>Both types share one selection, one rate and one output toggle; the tank of the type that is
 * not selected simply reports empty, so pipes and gauges always see exactly one chemical.
 */
public class GasTapBlockEntity extends ChemicalTapTileEntity<Gas, GasStack, IGasTank> {

    /** Chemicals that require the containment upgrade before they can be selected. */
    private static final Set<ResourceLocation> DANGEROUS_GASES = Set.of(
            new ResourceLocation("mekanism", "fissile_fuel"),
            new ResourceLocation("mekanism", "nuclear_waste"),
            new ResourceLocation("mekanism", "spent_nuclear_waste"),
            new ResourceLocation("mekanism", "antimatter"),
            new ResourceLocation("mekanism", "plutonium"),
            new ResourceLocation("mekanism", "polonium"));

    /** Chemicals that must never be selectable; sourcing them would bypass the radiation line. */
    private static final Set<ResourceLocation> FORBIDDEN_CHEMICALS = Set.of(
            new ResourceLocation(InfinityWhatMekanism.MOD_ID, "unknown_radiation"));

    private InfiniteChemicalTank.PigmentTank pigmentTank;
    private InputInventorySlot containmentSlot;

    public GasTapBlockEntity(BlockPos pos, BlockState state) {
        super(ModMekMachines.GAS_TAP, pos, state, ChemicalType.GAS);
        configComponent.setupInputConfig(TransmissionType.ITEM, containmentSlot);
        configComponent.setupOutputConfig(TransmissionType.GAS, tank, RelativeSide.values());
        configComponent.setupOutputConfig(TransmissionType.PIGMENT, pigmentTank,
                RelativeSide.values());

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.GAS,
                TransmissionType.PIGMENT);
    }

    @Override
    protected Set<ChemicalType> getSupportedTypes() {
        return Set.of(ChemicalType.GAS, ChemicalType.PIGMENT);
    }

    @Override
    public IChemicalTankHolder<Gas, GasStack, IGasTank> getInitialGasTanks(
            IContentsListener listener) {
        ChemicalTankHelper<Gas, GasStack, IGasTank> builder =
                ChemicalTankHelper.forSideGasWithConfig(this::getDirection, this::getConfig);
        builder.addTank(tank = new InfiniteChemicalTank.GasTank(this));
        return builder.build();
    }

    @Override
    public IChemicalTankHolder<Pigment, PigmentStack, IPigmentTank> getInitialPigmentTanks(
            IContentsListener listener) {
        ChemicalTankHelper<Pigment, PigmentStack, IPigmentTank> builder =
                ChemicalTankHelper.forSidePigmentWithConfig(this::getDirection, this::getConfig);
        builder.addTank(pigmentTank = new InfiniteChemicalTank.PigmentTank(this));
        return builder.build();
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder =
                InventorySlotHelper.forSideWithConfig(this::getDirection, this::getConfig);
        builder.addSlot(containmentSlot = InputInventorySlot.at(
                stack -> stack.is(ModItems.UPGRADE_CONTAINMENT.get()), listener, 152, 25));
        containmentSlot.setSlotOverlay(SlotOverlay.UPGRADE);
        return builder.build();
    }

    @Override
    public boolean hasContainmentUpgrade() {
        return this.containmentSlot != null
                && this.containmentSlot.getStack().is(ModItems.UPGRADE_CONTAINMENT.get());
    }

    public static boolean isDangerousGas(@Nullable Gas gas) {
        ResourceLocation id = gas == null ? null : gas.getRegistryName();
        return id != null && DANGEROUS_GASES.contains(id);
    }

    public static boolean isDangerousChemical(@Nullable Chemical<?> chemical) {
        ResourceLocation id = chemical == null ? null : chemical.getRegistryName();
        return id != null && DANGEROUS_GASES.contains(id);
    }

    @Override
    protected boolean isChemicalAllowed(Chemical<?> chemical) {
        ResourceLocation id = chemical.getRegistryName();
        if (id == null || FORBIDDEN_CHEMICALS.contains(id)
                || !MekanismAdditionConfig.isGasAllowed(id)) {
            return false;
        }
        return !isDangerousChemical(chemical) || hasContainmentUpgrade();
    }

    /** True when the selected dangerous chemical has no containment upgrade installed. */
    public boolean selectedChemicalRequiresUpgrade() {
        return isDangerousChemical(getSelectedChemical()) && !hasContainmentUpgrade();
    }

    @Override
    public boolean isOutputLocked() {
        return selectedChemicalRequiresUpgrade();
    }

    /** Gas and pigment share one selection screen. */
    @Override
    public List<Chemical<?>> getAvailableChemicals() {
        List<Chemical<?>> chemicals = new ArrayList<>(collectChemicals(ChemicalType.GAS));
        chemicals.addAll(collectChemicals(ChemicalType.PIGMENT));
        return chemicals;
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        // Migrate the pre-refactor containment upgrade into the new dedicated slot.
        if (tag.contains("RedstoneMode", 3)) {
            // The pre-refactor Gas Tap stored its own three-state redstone mode.
            setControlType(RedstoneControl.byIndexStatic(tag.getInt("RedstoneMode")));
        }
        if (tag.contains("ContainmentUpgrade", 10)
                && this.containmentSlot != null
                && this.containmentSlot.getStack().isEmpty()) {
            var upgrade = net.minecraft.world.item.ItemStack.of(
                    tag.getCompound("ContainmentUpgrade"));
            if (upgrade.is(ModItems.UPGRADE_CONTAINMENT.get())) {
                upgrade.setCount(1);
                this.containmentSlot.setStack(upgrade);
            }
        }
    }

    @NotNull
    @Override
    public Component getName() {
        return Component.translatable("container." + InfinityWhatMekanism.MOD_ID + ".gas_tap");
    }

    public IGasTank getGasTank() {
        return this.tank;
    }

    /** The selected chemical when it is a gas; the configuration card only stores gases. */
    @Nullable
    public Gas getSelectedGas() {
        Chemical<?> chemical = getSelectedChemical();
        return chemical instanceof Gas gas ? gas : null;
    }

    public IPigmentTank getPigmentTank() {
        return this.pigmentTank;
    }

    /**
     * Only the tank matching the selected type is exposed. Both tanks exist internally (one for
     * gases, one for pigments), but exposing the idle one made Jade list two empty gauges.
     */
    @Override
    public List<IGasTank> getGasTanks(@Nullable Direction side) {
        Chemical<?> chemical = getSelectedChemical();
        if (chemical != null && ChemicalType.getTypeFor(chemical) != ChemicalType.GAS) {
            return List.of();
        }
        return super.getGasTanks(side);
    }

    @Override
    public List<IPigmentTank> getPigmentTanks(@Nullable Direction side) {
        Chemical<?> chemical = getSelectedChemical();
        if (chemical == null || ChemicalType.getTypeFor(chemical) != ChemicalType.PIGMENT) {
            return List.of();
        }
        return super.getPigmentTanks(side);
    }

    public InputInventorySlot getContainmentSlot() {
        return this.containmentSlot;
    }
}
