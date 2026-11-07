package com.bandybus.infwhat.mekanism.item;

import com.bandybus.infwhat.creative.ModCreativeTabs;
import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Item registration for the Mekanism Addition. */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, InfinityWhatMekanism.MOD_ID);

    public static final RegistryObject<Item> UPGRADE_CONTAINMENT = ITEMS.register(
            "upgrade_containment",
            () -> new ContainmentUpgradeItem(new Item.Properties().stacksTo(1)
                    .tab(ModCreativeTabs.INFINITY_WHAT)));

    public static final RegistryObject<Item> SATURATED_RADIATION_SPONGE = ITEMS.register(
            "saturated_radiation_sponge",
            () -> new SaturatedRadiationSpongeItem(
                    com.bandybus.infwhat.mekanism.block.ModBlocks.SATURATED_RADIATION_SPONGE.get(),
                    new Item.Properties().stacksTo(64).tab(ModCreativeTabs.INFINITY_WHAT)));

    public static final RegistryObject<Item> RADIATION_CIRCUIT = ITEMS.register(
            "radiation_circuit",
            () -> new RadiationCircuitItem(new Item.Properties().stacksTo(1)
                    .tab(ModCreativeTabs.INFINITY_WHAT)));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
