package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.bandybus.infwhat.mekanism.item.ModItems;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.IChemicalTank;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Infinite chemical generator: sources any registered chemical from the registry.
 *
 * <p>One selection, one rate and one output toggle drive a single synthetic tank, so pipes and
 * gauges always see exactly the selected chemical.
 */
public class GasTapBlockEntity extends ChemicalTapTileEntity {

    /** Chemicals that require the containment upgrade before they can be selected. */
    private static final Set<ResourceLocation> DANGEROUS_CHEMICALS = Set.of(
            ResourceLocation.fromNamespaceAndPath("mekanism", "fissile_fuel"),
            ResourceLocation.fromNamespaceAndPath("mekanism", "nuclear_waste"),
            ResourceLocation.fromNamespaceAndPath("mekanism", "spent_nuclear_waste"),
            ResourceLocation.fromNamespaceAndPath("mekanism", "antimatter"),
            ResourceLocation.fromNamespaceAndPath("mekanism", "plutonium"),
            ResourceLocation.fromNamespaceAndPath("mekanism", "polonium"));

    /** Chemicals that must never be selectable; sourcing them would bypass the radiation line. */
    private static final Set<ResourceLocation> FORBIDDEN_CHEMICALS = Set.of(
            ResourceLocation.fromNamespaceAndPath(InfinityWhatMekanism.MOD_ID,
                    "unknown_radiation"));

    private InputInventorySlot containmentSlot;

    public GasTapBlockEntity(BlockPos pos, BlockState state) {
        super(ModMekMachines.GAS_TAP, pos, state);
        configComponent.setupInputConfig(TransmissionType.ITEM, containmentSlot);
        configComponent.setupOutputConfig(TransmissionType.CHEMICAL, tank,
                RelativeSide.values());
        SideConfigDefaults.setAll(configComponent, TransmissionType.ITEM, DataType.INPUT);
        SideConfigDefaults.setAll(configComponent, TransmissionType.CHEMICAL, DataType.OUTPUT);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.CHEMICAL);
    }

    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this);
        builder.addTank(tank = new InfiniteChemicalTank(this));
        return builder.build();
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
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

    public static boolean isDangerousChemical(@Nullable Chemical chemical) {
        ResourceLocation id = idOf(chemical);
        return id != null && DANGEROUS_CHEMICALS.contains(id);
    }

    @Override
    protected boolean isChemicalAllowed(Chemical chemical) {
        ResourceLocation id = idOf(chemical);
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

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        // Migrate the pre-refactor Gas Tap which stored its own three-state redstone mode.
        if (tag.contains("RedstoneMode", 3)) {
            setControlType(RedstoneControl.BY_ID.apply(tag.getInt("RedstoneMode")));
        }
    }

    @NotNull
    @Override
    public Component getName() {
        return Component.translatable("container." + InfinityWhatMekanism.MOD_ID + ".gas_tap");
    }

    public IChemicalTank getChemicalTank() {
        return this.tank;
    }

    public InputInventorySlot getContainmentSlot() {
        return this.containmentSlot;
    }
}
