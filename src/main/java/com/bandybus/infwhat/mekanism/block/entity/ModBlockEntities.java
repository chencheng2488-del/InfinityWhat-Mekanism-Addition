package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entity registration for the Mekanism Addition. */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, InfinityWhatMekanism.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadiationSpongeBlockEntity>>
            RADIATION_SPONGE = BLOCK_ENTITIES.register("radiation_sponge", () ->
                    BlockEntityType.Builder.of(
                            RadiationSpongeBlockEntity::new,
                            ModBlocks.RADIATION_SPONGE.get(),
                            ModBlocks.SATURATED_RADIATION_SPONGE.get()
                    ).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
