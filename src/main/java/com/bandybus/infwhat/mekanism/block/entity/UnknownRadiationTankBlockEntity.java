package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.bandybus.infwhat.mekanism.gas.ModGases;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.chemical.GasInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Dedicated storage for the unknown radiation substance.
 *
 * <p>Mekanism's normal chemical tanks reject it because it carries a radiation attribute that
 * needs validation, so this tank is built with {@link ChemicalAttributeValidator#ALWAYS_ALLOW}.
 * Breaking it drops a normal block item with the stored amount in its NBT, so the contents travel
 * with the tank instead of leaking into the world.
 */
public class UnknownRadiationTankBlockEntity extends TileEntityConfigurableMachine {

    private static final String TAG_GAS_ID = "StoredGas";
    private static final String TAG_GAS_AMOUNT = "StoredAmount";

    private static long capacity() {
        return MekanismAdditionConfig.unknownRadiationTankCapacity();
    }

    private IGasTank tank;
    private GasInventorySlot fillSlot;
    private GasInventorySlot drainSlot;

    public UnknownRadiationTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModMekMachines.UNKNOWN_RADIATION_TANK, pos, state);
        configComponent = new TileComponentConfig(this, TransmissionType.GAS);
        configComponent.setupIOConfig(TransmissionType.GAS, tank, RelativeSide.BACK);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.GAS);
    }

    @Override
    public IChemicalTankHolder<Gas, GasStack, IGasTank> getInitialGasTanks(
            IContentsListener listener) {
        ChemicalTankHelper<Gas, GasStack, IGasTank> builder =
                ChemicalTankHelper.forSideGasWithConfig(this::getDirection, this::getConfig);
        tank = ChemicalTankBuilder.GAS.create(capacity(),
                ConstantPredicates.alwaysTrueBi(),
                ConstantPredicates.alwaysTrueBi(),
                gas -> gas == ModGases.UNKNOWN_RADIATION.get(),
                ChemicalAttributeValidator.ALWAYS_ALLOW, listener);
        builder.addTank(tank);
        return builder.build();
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSide(this::getDirection);
        builder.addSlot(fillSlot = GasInventorySlot.fill(tank, listener, 152, 25));
        fillSlot.setSlotType(ContainerSlotType.INPUT);
        fillSlot.setSlotOverlay(SlotOverlay.MINUS);
        builder.addSlot(drainSlot = GasInventorySlot.drain(tank, listener, 152, 55));
        drainSlot.setSlotType(ContainerSlotType.OUTPUT);
        drainSlot.setSlotOverlay(SlotOverlay.PLUS);
        return builder.build();
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        fillSlot.fillTank();
        drainSlot.drainTank();
    }

    /**
     * The tank carries its contents in the dropped item instead of spilling them into the world,
     * so Mekanism must not dump the stored radioactive gas as an environmental source on removal.
     */
    @Override
    public boolean shouldDumpRadiation() {
        return false;
    }

    /** Copies the stored gas into the dropped block item. */
    public void writeToItem(ItemStack stack) {
        if (tank == null || tank.isEmpty()) {
            return;
        }
        ResourceLocation id = tank.getStack().getTypeRegistryName();
        if (id != null) {
            stack.getOrCreateTag().putString(TAG_GAS_ID, id.toString());
            stack.getOrCreateTag().putLong(TAG_GAS_AMOUNT, tank.getStored());
        }
    }

    /** Restores the stored gas when the tank is placed back down. */
    public void readFromItem(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || tank == null || !tag.contains(TAG_GAS_ID)) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_GAS_ID));
        if (id == null) {
            return;
        }
        var registry = mekanism.api.MekanismAPI.gasRegistry();
        Gas gas = registry == null ? null : registry.getValue(id);
        if (gas == null || gas.isEmptyType()) {
            return;
        }
        long amount = Math.min(capacity(), Math.max(0L, tag.getLong(TAG_GAS_AMOUNT)));
        if (amount > 0) {
            tank.setStack(new GasStack(gas, amount));
        }
    }

    public IGasTank getGasTank() {
        return tank;
    }

    @NotNull
    @Override
    public Component getName() {
        return Component.translatable("container." + InfinityWhatMekanism.MOD_ID
                + ".unknown_radiation_tank");
    }
}
