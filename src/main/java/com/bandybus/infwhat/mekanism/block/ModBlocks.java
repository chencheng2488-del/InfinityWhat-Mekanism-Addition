package com.bandybus.infwhat.mekanism.block;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.item.ModItems;
import com.bandybus.infwhat.mekanism.item.RadiationSpongeBlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/** Block and block-item registration for the Mekanism Addition. */
public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, InfinityWhatMekanism.MOD_ID);

    public static final RegistryObject<Block> RADIATION_SPONGE = BLOCKS.register("radiation_sponge",
            () -> new RadiationSpongeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.8F)
                    .sound(SoundType.GRASS)));

    public static final RegistryObject<Block> SATURATED_RADIATION_SPONGE = BLOCKS.register(
            "saturated_radiation_sponge",
            () -> new SaturatedRadiationSpongeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.8F)
                    .sound(SoundType.GRASS)));

    public static final RegistryObject<Item> RADIATION_SPONGE_ITEM = ModItems.ITEMS.register(
            "radiation_sponge",
            () -> new RadiationSpongeBlockItem(RADIATION_SPONGE.get(),
                    new Item.Properties().stacksTo(64)));

    private ModBlocks() {
    }
    
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name,
                () -> new BlockItem(toReturn.get(), new Item.Properties().stacksTo(1)));
        return toReturn;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
