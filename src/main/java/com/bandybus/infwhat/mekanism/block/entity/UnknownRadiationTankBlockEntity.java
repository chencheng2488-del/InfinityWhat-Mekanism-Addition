package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModMekMachines;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import com.bandybus.infwhat.mekanism.gas.ModChemicals;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.chemical.ChemicalInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Dedicated storage for the unknown radiation substance.
 *
 * <p>Mekanism's normal chemical tanks reject it because it carries a radiation attribute that
 * needs validation, so this tank is built with {@link ChemicalAttributeValidator#ALWAYS_ALLOW}.
 * Breaking it drops a normal block item with the stored amount in its data components, so the
 * contents travel with the tank instead of leaking into the world.
 */
public class UnknownRadiationTankBlockEntity extends TileEntityConfigurableMachine {

    private static final String TAG_CHEMICAL_ID = "StoredChemical";
    private static final String TAG_AMOUNT = "StoredAmount";

    private static long capacity() {
        return MekanismAdditionConfig.unknownRadiationTankCapacity();
    }

    private IChemicalTank tank;
    private ChemicalInventorySlot fillSlot;
    private ChemicalInventorySlot drainSlot;

    public UnknownRadiationTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModMekMachines.UNKNOWN_RADIATION_TANK, pos, state);
        configComponent.setupIOConfig(TransmissionType.CHEMICAL, tank, RelativeSide.BACK);
        SideConfigDefaults.setAll(configComponent, TransmissionType.CHEMICAL,
                DataType.INPUT_OUTPUT);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.CHEMICAL);
    }

    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this);
        tank = BasicChemicalTank.createModern(capacity(),
                ConstantPredicates.alwaysTrueBi(),
                ConstantPredicates.alwaysTrueBi(),
                stack -> stack.is(ModChemicals.UNKNOWN_RADIATION.get()),
                ChemicalAttributeValidator.ALWAYS_ALLOW, listener);
        builder.addTank(tank);
        return builder.build();
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSide(this::getDirection);
        builder.addSlot(fillSlot = ChemicalInventorySlot.fill(tank, listener, 152, 25));
        fillSlot.setSlotType(ContainerSlotType.INPUT);
        fillSlot.setSlotOverlay(SlotOverlay.MINUS);
        builder.addSlot(drainSlot = ChemicalInventorySlot.drain(tank, listener, 152, 55));
        drainSlot.setSlotType(ContainerSlotType.OUTPUT);
        drainSlot.setSlotOverlay(SlotOverlay.PLUS);
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdate = super.onUpdateServer();
        fillSlot.fillTank();
        drainSlot.drainTank();
        return sendUpdate;
    }

    /**
     * The tank carries its contents in the dropped item instead of spilling them into the world,
     * so Mekanism must not dump the stored radioactive chemical as an environmental source on
     * removal.
     */
    @Override
    public boolean shouldDumpRadiation() {
        return false;
    }

    /** Copies the stored chemical into the dropped block item. */
    public void writeToItem(ItemStack stack) {
        if (tank == null || tank.isEmpty()) {
            return;
        }
        ResourceLocation id = ChemicalTapTileEntity.idOf(tank.getStack().getChemical());
        if (id == null) {
            return;
        }
        long amount = tank.getStored();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_CHEMICAL_ID, id.toString());
            tag.putLong(TAG_AMOUNT, amount);
        });
    }

    /** Restores the stored chemical when the tank is placed back down. */
    public void readFromItem(ItemStack stack) {
        CompoundTag tag = customDataCopy(stack);
        if (tag == null || tank == null || !tag.contains(TAG_CHEMICAL_ID)) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_CHEMICAL_ID));
        if (id == null) {
            return;
        }
        Chemical chemical = ChemicalTapTileEntity.findChemical(id);
        if (chemical == null || chemical != ModChemicals.UNKNOWN_RADIATION.get()) {
            return;
        }
        long amount = Math.min(capacity(), Math.max(0L, tag.getLong(TAG_AMOUNT)));
        if (amount > 0) {
            tank.setStack(new ChemicalStack(chemical.getAsHolder(), amount));
        }
    }

    @javax.annotation.Nullable
    private static CompoundTag customDataCopy(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public IChemicalTank getChemicalTank() {
        return tank;
    }

    @NotNull
    @Override
    public Component getName() {
        return Component.translatable("container." + InfinityWhatMekanism.MOD_ID
                + ".unknown_radiation_tank");
    }
}
