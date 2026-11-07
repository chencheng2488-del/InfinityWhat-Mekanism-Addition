package com.bandybus.infwhat.mekanism.item;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item registration for the Mekanism Addition. */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, InfinityWhatMekanism.MOD_ID);

    public static final DeferredHolder<Item, Item> UPGRADE_CONTAINMENT = ITEMS.register(
            "upgrade_containment",
            () -> new ContainmentUpgradeItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> SATURATED_RADIATION_SPONGE = ITEMS.register(
            "saturated_radiation_sponge",
            () -> new SaturatedRadiationSpongeItem(
                    com.bandybus.infwhat.mekanism.block.ModBlocks.SATURATED_RADIATION_SPONGE.get(),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<Item, Item> RADIATION_CIRCUIT = ITEMS.register(
            "radiation_circuit",
            () -> new RadiationCircuitItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
