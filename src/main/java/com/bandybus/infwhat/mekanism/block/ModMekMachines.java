package com.bandybus.infwhat.mekanism.block;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.item.CoreTabItemBlockMachine;
import com.bandybus.infwhat.mekanism.block.entity.GasTapBlockEntity;
import com.bandybus.infwhat.mekanism.block.entity.InfiniteInfusionGeneratorTileEntity;
import com.bandybus.infwhat.mekanism.block.entity.InfiniteSlurryGeneratorTileEntity;
import com.bandybus.infwhat.mekanism.block.entity.RadiationExtractorBlockEntity;
import com.bandybus.infwhat.mekanism.block.entity.RadiationFractionatorBlockEntity;
import com.bandybus.infwhat.mekanism.block.entity.UnknownRadiationTankBlockEntity;
import mekanism.api.Upgrade;
import mekanism.api.math.FloatingLong;
import mekanism.api.text.ILangEntry;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.item.block.machine.ItemBlockMachine;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.EnumSet;
import java.util.function.Supplier;

/**
 * Registers the two radiation machines on Mekanism's own machine framework.
 *
 * <p>Using {@link BlockTypeTile} with the same attributes as Mekanism's {@code Machine}
 * block type gives the machines the standard GUI features: upgrades, side configuration,
 * security, redstone control, comparator output, energy slot and ejector configuration.
 */
public final class ModMekMachines {

    private static final BlockDeferredRegister BLOCKS =
            new BlockDeferredRegister(InfinityWhatMekanism.MOD_ID);
    private static final TileEntityTypeDeferredRegister TILE_ENTITY_TYPES =
            new TileEntityTypeDeferredRegister(InfinityWhatMekanism.MOD_ID);
    private static final ContainerTypeDeferredRegister CONTAINERS =
            new ContainerTypeDeferredRegister(InfinityWhatMekanism.MOD_ID);

    private static final ILangEntry GAS_TAP_DESCRIPTION =
            () -> "description.infwhat_mekanism.gas_tap";
    private static final ILangEntry EXTRACTOR_DESCRIPTION =
            () -> "description.infwhat_mekanism.radiation_extractor";
    private static final ILangEntry FRACTIONATOR_DESCRIPTION =
            () -> "description.infwhat_mekanism.radiation_fractionator";
    private static final ILangEntry INFUSION_TAP_DESCRIPTION =
            () -> "description.infwhat_mekanism.infinite_infusion_generator";
    private static final ILangEntry SLURRY_TAP_DESCRIPTION =
            () -> "description.infwhat_mekanism.infinite_slurry_generator";
    private static final ILangEntry UNKNOWN_RADIATION_TANK_DESCRIPTION =
            () -> "description.infwhat_mekanism.unknown_radiation_tank";

    public static final ContainerTypeRegistryObject<MekanismTileContainer<GasTapBlockEntity>>
            GAS_TAP_CONTAINER = CONTAINERS
                    .custom("gas_tap", GasTapBlockEntity.class).build();

    public static final ContainerTypeRegistryObject<MekanismTileContainer<RadiationExtractorBlockEntity>>
            RADIATION_EXTRACTOR_CONTAINER = CONTAINERS
                    .custom("radiation_extractor", RadiationExtractorBlockEntity.class).build();

    public static final ContainerTypeRegistryObject<MekanismTileContainer<RadiationFractionatorBlockEntity>>
            RADIATION_FRACTIONATOR_CONTAINER = CONTAINERS
                    .custom("radiation_fractionator", RadiationFractionatorBlockEntity.class).build();

    public static final ContainerTypeRegistryObject<MekanismTileContainer<InfiniteInfusionGeneratorTileEntity>>
            INFINITE_INFUSION_GENERATOR_CONTAINER = CONTAINERS
                    .custom("infinite_infusion_generator",
                            InfiniteInfusionGeneratorTileEntity.class).build();

    public static final ContainerTypeRegistryObject<MekanismTileContainer<InfiniteSlurryGeneratorTileEntity>>
            INFINITE_SLURRY_GENERATOR_CONTAINER = CONTAINERS
                    .custom("infinite_slurry_generator",
                            InfiniteSlurryGeneratorTileEntity.class).build();

    public static final ContainerTypeRegistryObject<MekanismTileContainer<UnknownRadiationTankBlockEntity>>
            UNKNOWN_RADIATION_TANK_CONTAINER = CONTAINERS
                    .custom("unknown_radiation_tank",
                            UnknownRadiationTankBlockEntity.class).build();

    public static final BlockTypeTile<GasTapBlockEntity> GAS_TAP_TYPE =
            createGasTapType(() -> GAS_TAP_CONTAINER, GAS_TAP_DESCRIPTION);

    public static final BlockTypeTile<RadiationExtractorBlockEntity> RADIATION_EXTRACTOR_TYPE =
            createMachineType(ModMekMachines::extractorTile,
                    () -> RADIATION_EXTRACTOR_CONTAINER, EXTRACTOR_DESCRIPTION,
                    RadiationExtractorBlockEntity.ENERGY_USAGE,
                    RadiationExtractorBlockEntity.ENERGY_CAPACITY);

    public static final BlockTypeTile<RadiationFractionatorBlockEntity> RADIATION_FRACTIONATOR_TYPE =
            createMachineType(ModMekMachines::fractionatorTile,
                    () -> RADIATION_FRACTIONATOR_CONTAINER, FRACTIONATOR_DESCRIPTION,
                    RadiationFractionatorBlockEntity.ENERGY_USAGE,
                    RadiationFractionatorBlockEntity.ENERGY_CAPACITY);

    public static final BlockTypeTile<InfiniteInfusionGeneratorTileEntity> INFINITE_INFUSION_GENERATOR_TYPE =
            createImportTapType(ModMekMachines::infusionTile,
                    () -> INFINITE_INFUSION_GENERATOR_CONTAINER, INFUSION_TAP_DESCRIPTION, true);

    public static final BlockTypeTile<InfiniteSlurryGeneratorTileEntity> INFINITE_SLURRY_GENERATOR_TYPE =
            createImportTapType(ModMekMachines::slurryTile,
                    () -> INFINITE_SLURRY_GENERATOR_CONTAINER, SLURRY_TAP_DESCRIPTION, false);

    public static final BlockTypeTile<UnknownRadiationTankBlockEntity> UNKNOWN_RADIATION_TANK_TYPE =
            createTankType(ModMekMachines::unknownRadiationTankTile,
                    () -> UNKNOWN_RADIATION_TANK_CONTAINER, UNKNOWN_RADIATION_TANK_DESCRIPTION);

    public static final BlockRegistryObject<GasTapBlockTile, CoreTabItemBlockMachine> GAS_TAP =
            BLOCKS.register("gas_tap",
                    () -> new GasTapBlockTile(GAS_TAP_TYPE, properties ->
                            properties.color(MaterialColor.METAL).sound(SoundType.METAL)),
                    CoreTabItemBlockMachine::new);

    public static final BlockRegistryObject<BlockTile<RadiationExtractorBlockEntity,
            BlockTypeTile<RadiationExtractorBlockEntity>>, CoreTabItemBlockMachine> RADIATION_EXTRACTOR =
            BLOCKS.register("radiation_extractor",
                    () -> new BlockTile<>(RADIATION_EXTRACTOR_TYPE, properties ->
                            properties.color(MaterialColor.METAL).sound(SoundType.METAL)),
                    CoreTabItemBlockMachine::new);

    public static final BlockRegistryObject<BlockTile<RadiationFractionatorBlockEntity,
            BlockTypeTile<RadiationFractionatorBlockEntity>>, CoreTabItemBlockMachine> RADIATION_FRACTIONATOR =
            BLOCKS.register("radiation_fractionator",
                    () -> new BlockTile<>(RADIATION_FRACTIONATOR_TYPE, properties ->
                            properties.color(MaterialColor.METAL).sound(SoundType.METAL)),
                    CoreTabItemBlockMachine::new);

    public static final BlockRegistryObject<BlockTile<InfiniteInfusionGeneratorTileEntity,
            BlockTypeTile<InfiniteInfusionGeneratorTileEntity>>, CoreTabItemBlockMachine> INFINITE_INFUSION_GENERATOR =
            BLOCKS.register("infinite_infusion_generator",
                    () -> new BlockTile<>(INFINITE_INFUSION_GENERATOR_TYPE, properties ->
                            properties.color(MaterialColor.METAL).sound(SoundType.METAL)),
                    CoreTabItemBlockMachine::new);

    public static final BlockRegistryObject<BlockTile<InfiniteSlurryGeneratorTileEntity,
            BlockTypeTile<InfiniteSlurryGeneratorTileEntity>>, CoreTabItemBlockMachine> INFINITE_SLURRY_GENERATOR =
            BLOCKS.register("infinite_slurry_generator",
                    () -> new BlockTile<>(INFINITE_SLURRY_GENERATOR_TYPE, properties ->
                            properties.color(MaterialColor.METAL).sound(SoundType.METAL)),
                    CoreTabItemBlockMachine::new);

    public static final BlockRegistryObject<UnknownRadiationTankBlock, CoreTabItemBlockMachine>
            UNKNOWN_RADIATION_TANK = BLOCKS.register("unknown_radiation_tank",
            () -> new UnknownRadiationTankBlock(UNKNOWN_RADIATION_TANK_TYPE, properties ->
                    properties.color(MaterialColor.METAL).sound(SoundType.METAL)
                            .strength(2.0F, 9.0F)),
            CoreTabItemBlockMachine::new);

    public static final TileEntityTypeRegistryObject<GasTapBlockEntity> GAS_TAP_TILE =
            TILE_ENTITY_TYPES.register(GAS_TAP, GasTapBlockEntity::new);

    public static final TileEntityTypeRegistryObject<RadiationExtractorBlockEntity>
            RADIATION_EXTRACTOR_TILE = TILE_ENTITY_TYPES.register(RADIATION_EXTRACTOR,
            RadiationExtractorBlockEntity::new);

    public static final TileEntityTypeRegistryObject<RadiationFractionatorBlockEntity>
            RADIATION_FRACTIONATOR_TILE = TILE_ENTITY_TYPES.register(RADIATION_FRACTIONATOR,
            RadiationFractionatorBlockEntity::new);

    public static final TileEntityTypeRegistryObject<InfiniteInfusionGeneratorTileEntity>
            INFINITE_INFUSION_GENERATOR_TILE = TILE_ENTITY_TYPES.register(
            INFINITE_INFUSION_GENERATOR, InfiniteInfusionGeneratorTileEntity::new);

    public static final TileEntityTypeRegistryObject<InfiniteSlurryGeneratorTileEntity>
            INFINITE_SLURRY_GENERATOR_TILE = TILE_ENTITY_TYPES.register(
            INFINITE_SLURRY_GENERATOR, InfiniteSlurryGeneratorTileEntity::new);

    public static final TileEntityTypeRegistryObject<UnknownRadiationTankBlockEntity>
            UNKNOWN_RADIATION_TANK_TILE = TILE_ENTITY_TYPES.register(
            UNKNOWN_RADIATION_TANK, UnknownRadiationTankBlockEntity::new);

    private ModMekMachines() {
    }

    private static TileEntityTypeRegistryObject<UnknownRadiationTankBlockEntity>
            unknownRadiationTankTile() {
        return UNKNOWN_RADIATION_TANK_TILE;
    }

    private static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createTankType(
            Supplier<TileEntityTypeRegistryObject<TILE>> tileType,
            Supplier<ContainerTypeRegistryObject<? extends MekanismContainer>> container,
            ILangEntry description) {
        BlockTypeTile<TILE> type = BlockTypeTile.BlockTileBuilder
                .createBlock(tileType, description)
                .withGui(container)
                .build();
        type.add(Attributes.INVENTORY, Attributes.SECURITY, Attributes.COMPARATOR);
        return type;
    }

    // Method indirection keeps the tile fields below referable from the block types above.
    private static TileEntityTypeRegistryObject<RadiationExtractorBlockEntity> extractorTile() {
        return RADIATION_EXTRACTOR_TILE;
    }

    private static TileEntityTypeRegistryObject<RadiationFractionatorBlockEntity> fractionatorTile() {
        return RADIATION_FRACTIONATOR_TILE;
    }

    private static TileEntityTypeRegistryObject<InfiniteInfusionGeneratorTileEntity> infusionTile() {
        return INFINITE_INFUSION_GENERATOR_TILE;
    }

    private static TileEntityTypeRegistryObject<InfiniteSlurryGeneratorTileEntity> slurryTile() {
        return INFINITE_SLURRY_GENERATOR_TILE;
    }

    /**
     * Import driven taps: the player feeds the machine through its slot or an adjacent pipe. They
     * have no facing because the side configuration itself selects the import and output faces.
     */
    private static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createImportTapType(
            Supplier<TileEntityTypeRegistryObject<TILE>> tileType,
            Supplier<ContainerTypeRegistryObject<? extends MekanismContainer>> container,
            ILangEntry description, boolean hasInventory) {
        BlockTypeTile<TILE> type = BlockTypeTile.BlockTileBuilder
                .createBlock(tileType, description)
                .withGui(container)
                .build();
        if (hasInventory) {
            type.add(Attributes.ACTIVE_LIGHT, Attributes.INVENTORY, Attributes.SECURITY,
                    Attributes.REDSTONE, Attributes.COMPARATOR);
        } else {
            type.add(Attributes.ACTIVE_LIGHT, Attributes.SECURITY, Attributes.REDSTONE,
                    Attributes.COMPARATOR);
        }
        return type;
    }

    private static BlockTypeTile<GasTapBlockEntity> createGasTapType(
            Supplier<ContainerTypeRegistryObject<? extends MekanismContainer>> container,
            ILangEntry description) {
        BlockTypeTile<GasTapBlockEntity> type = BlockTypeTile.BlockTileBuilder
                .createBlock(ModMekMachines::gasTapTile, description)
                .withGui(container)
                .build();
        type.add(new AttributeStateFacing(), Attributes.ACTIVE_LIGHT, Attributes.INVENTORY,
                Attributes.SECURITY, Attributes.REDSTONE, Attributes.COMPARATOR);
        return type;
    }

    private static TileEntityTypeRegistryObject<GasTapBlockEntity> gasTapTile() {
        return GAS_TAP_TILE;
    }

    /**
     * Builds a block type with the same base attributes as Mekanism's own {@code Machine}.
     * The custom description keeps the item tooltip specific to this add-on.
     */
    private static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createMachineType(
            Supplier<TileEntityTypeRegistryObject<TILE>> tileType,
            Supplier<ContainerTypeRegistryObject<? extends MekanismContainer>> container,
            ILangEntry description, FloatingLong energyUsage, FloatingLong energyStorage) {
        BlockTypeTile<TILE> type = BlockTypeTile.BlockTileBuilder
                .createBlock(tileType, description)
                .withGui(container)
                .withEnergyConfig(() -> energyUsage, () -> energyStorage)
                .withSupportedUpgrades(EnumSet.of(Upgrade.SPEED, Upgrade.ENERGY,
                        Upgrade.MUFFLING))
                .build();
        type.add(new AttributeStateFacing(), Attributes.ACTIVE_LIGHT, Attributes.INVENTORY,
                Attributes.SECURITY, Attributes.REDSTONE, Attributes.COMPARATOR);
        return type;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        TILE_ENTITY_TYPES.register(eventBus);
        CONTAINERS.register(eventBus);
    }
}
