package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Block entity registration for the Mekanism Addition. */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, InfinityWhatMekanism.MOD_ID);

    public static final RegistryObject<BlockEntityType<RadiationSpongeBlockEntity>> RADIATION_SPONGE =
            BLOCK_ENTITIES.register("radiation_sponge", () ->
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
