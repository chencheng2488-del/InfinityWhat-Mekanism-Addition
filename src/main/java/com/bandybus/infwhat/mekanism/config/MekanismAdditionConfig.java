package com.bandybus.infwhat.mekanism.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Server/common configuration for the Mekanism Addition.
 *
 * <p>The Gas Tap is an infinite source, so it is intentionally gated in several layers: a global
 * switch, a dedicated-server switch, per-tick rate cap, gas blacklist/whitelist and dimension
 * blacklist. The core mod's own {@code balance.enableInfiniteSource} switch is also respected by
 * the block entity.
 */
public final class MekanismAdditionConfig {

    public enum FilterMode {
        BLACKLIST,
        WHITELIST
    }

    public static final ForgeConfigSpec SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static final class Common {
        public final ForgeConfigSpec.BooleanValue enableGasTap;
        public final ForgeConfigSpec.BooleanValue allowOnDedicatedServer;
        public final ForgeConfigSpec.LongValue maxRatePerTick;
        public final ForgeConfigSpec.LongValue defaultRatePerTick;
        public final ForgeConfigSpec.EnumValue<FilterMode> gasFilterMode;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> gasFilterList;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionBlacklist;
        public final ForgeConfigSpec.BooleanValue enableRadiationSponge;
        public final ForgeConfigSpec.BooleanValue allowRadiationSpongeOnDedicatedServer;
        public final ForgeConfigSpec.IntValue radiationSpongeRadiusChunks;
        public final ForgeConfigSpec.DoubleValue radiationSpongeMinRadiation;
        public final ForgeConfigSpec.BooleanValue spongeClearsPlayerRadiation;
        public final ForgeConfigSpec.DoubleValue spongePlayerDoseToChunks;
        public final ForgeConfigSpec.DoubleValue spongePlayerDoseFactor;
        public final ForgeConfigSpec.LongValue playerYieldCap;
        public final ForgeConfigSpec.IntValue spongeNauseaTicks;
        public final ForgeConfigSpec.IntValue spongeNauseaAmplifier;
        public final ForgeConfigSpec.BooleanValue enableRadiationMachines;
        public final ForgeConfigSpec.IntValue gasPerChunk;
        public final ForgeConfigSpec.DoubleValue radiationGasFactor;
        public final ForgeConfigSpec.LongValue yieldEquilibrium;
        public final ForgeConfigSpec.DoubleValue overloadSteepness;
        public final ForgeConfigSpec.LongValue extractorTankCapacity;
        public final ForgeConfigSpec.LongValue fractionatorTankCapacity;
        public final ForgeConfigSpec.DoubleValue unknownRadiationStrength;
        public final ForgeConfigSpec.LongValue unknownRadiationTankCapacity;

        private Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Infinity What: Mekanism Addition settings.").push("gas_tap");
            this.enableGasTap = builder
                    .comment("Master switch for the Gas Tap's infinite output.",
                            "When false the block still exists, but provides no gas and performs no push.")
                    .define("enableGasTap", true);
            this.allowOnDedicatedServer = builder
                    .comment("Allow the Gas Tap to output on a dedicated server.",
                            "Defaults to false so a server administrator must opt in explicitly.")
                    .define("allowOnDedicatedServer", false);
            this.maxRatePerTick = builder
                    .comment("Maximum gas amount per tick across all enabled output faces.",
                            "The Gas Tap's synthetic tank shares this budget between pipes and auto-ejection.")
                    .defineInRange("maxRatePerTick", 1_000_000L, 1L, Long.MAX_VALUE);
            this.defaultRatePerTick = builder
                    .comment("Default output rate of a freshly placed generator.",
                            "Existing machines keep the rate stored in their own NBT.")
                    .defineInRange("defaultRatePerTick", 1_000_000L, 1L, Long.MAX_VALUE);
            builder.pop();

            builder.comment("Gas filter for the Gas Tap.").push("gas_filter");
            this.gasFilterMode = builder
                    .comment("BLACKLIST: listed gas ids can never be selected.",
                            "WHITELIST: only listed gas ids can be selected.")
                    .defineEnum("mode", FilterMode.BLACKLIST);
            this.gasFilterList = builder
                    .comment("Gas ids, for example \"mekanism:oxygen\".")
                    .defineList("list", List.of(), obj -> obj instanceof String s && s.contains(":"));
            builder.pop();

            builder.comment("Dimensions in which the Gas Tap must not output.").push("dimensions");
            this.dimensionBlacklist = builder
                    .comment("Dimension ids, for example \"minecraft:the_nether\".")
                    .defineList("blacklist", List.of(), obj -> obj instanceof String s && s.contains(":"));
            builder.pop();

            builder.comment("Super Radiation Sponge settings.").push("radiation_sponge");
            this.enableRadiationSponge = builder
                    .comment("Master switch for the Super Radiation Sponge.",
                            "When false the block stays inert.")
                    .define("enableRadiationSponge", true);
            this.allowRadiationSpongeOnDedicatedServer = builder
                    .comment("Allow the Super Radiation Sponge to work on a dedicated server.",
                            "Defaults to false so a server administrator must opt in explicitly.")
                    .define("allowRadiationSpongeOnDedicatedServer", false);
            this.radiationSpongeRadiusChunks = builder
                    .comment("Chunk radius cleared by one sponge. 0 means only its own chunk.",
                            "Larger values clear a wider radiation cloud but are more powerful.")
                    .defineInRange("radiusChunks", 5, 0, 8);
            this.radiationSpongeMinRadiation = builder
                    .comment("Minimum radiation level at the sponge position before it activates.")
                    .defineInRange("minRadiation", 1.0E-4D, 0.0D, 1.0D);
            this.spongeClearsPlayerRadiation = builder
                    .comment("Allow right-clicking a working sponge to drain the player's accumulated dose.",
                            "The sponge saturates and the player receives a short nausea effect.")
                    .define("clearPlayerRadiation", true);
            this.spongePlayerDoseToChunks = builder
                    .comment("Equivalent chunks credited per sievert of player dose when the sponge is used.",
                            "Used as: chunks = ceil(dose * playerDoseToChunks).")
                    .defineInRange("playerDoseToChunks", 1.0D, 0.0D, 1000.0D);
            this.spongePlayerDoseFactor = builder
                    .comment("Multiplier applied to the player's dose before it is stored as absorbed radiation.",
                            "Only used for display and legacy sponges; the player yield is logarithmic.")
                    .defineInRange("playerDoseFactor", 1.0D, 0.0D, 1000.0D);
            this.playerYieldCap = builder
                    .comment("Maximum unknown radiation substance produced from one player dose, in mB.")
                    .defineInRange("playerYieldCap", 9_000L, 1L, Long.MAX_VALUE);
            this.spongeNauseaTicks = builder
                    .comment("Duration in ticks of the nausea effect applied after draining a player.")
                    .defineInRange("nauseaTicks", 40, 1, 20 * 60);
            this.spongeNauseaAmplifier = builder
                    .comment("Amplifier of the nausea effect. 0 is level I, 4 is level V.")
                    .defineInRange("nauseaAmplifier", 4, 0, 255);
            builder.pop();

            builder.comment("Radiation Extractor and Radiation Fractionator settings.")
                    .push("radiation_machines");
            this.enableRadiationMachines = builder
                    .comment("Master switch for the radiation production line.",
                            "When false both machines stay idle and keep their contents.")
                    .define("enableRadiationMachines", true);
            this.gasPerChunk = builder
                    .comment("Legacy value, no longer used by the yield curve.",
                            "Kept so existing configuration files keep loading without losing the key.")
                    .defineInRange("gasPerChunk", 64, 1, 256);
            this.radiationGasFactor = builder
                    .comment("Legacy value, no longer used by the yield curve.",
                            "Kept so existing configuration files keep loading without losing the key.")
                    .defineInRange("radiationGasFactor", 1.0D, 0.0D, 100.0D);
            this.yieldEquilibrium = builder
                    .comment("Equilibrium K of the sponge yield curve, in mB.",
                            "Below K the self-copying loop grows, at K it is stable,",
                            "and above K the overflow decays back towards K.")
                    .defineInRange("yieldEquilibrium", 8_192L, 1L, Long.MAX_VALUE);
            this.overloadSteepness = builder
                    .comment("Steepness of the overflow decay above the equilibrium.",
                            "Larger values make an overloaded sponge yield less.")
                    .defineInRange("overloadSteepness", 1.5D, 0.1D, 10.0D);
            this.extractorTankCapacity = builder
                    .comment("Output tank capacity of the Radiation Extractor, in mB.")
                    .defineInRange("extractorTankCapacity", 3_200_000L, 1L, Long.MAX_VALUE);
            this.fractionatorTankCapacity = builder
                    .comment("Input and output tank capacity of the Radiation Fractionator, in mB.")
                    .defineInRange("fractionatorTankCapacity", 2_000L, 1L, Long.MAX_VALUE);
            builder.pop();

            builder.comment("Unknown radiation substance settings.").push("radiation_gas");
            this.unknownRadiationStrength = builder
                    .comment("Radioactivity per mB of the unknown radiation substance, in Sv/h.",
                            "Mekanism's hottest own gas, polonium, uses 0.05.")
                    .defineInRange("unknownRadiationStrength", 5.0D, 0.0D, 1000.0D);
            builder.pop();

            builder.comment("Unknown radiation substance storage.").push("unknown_radiation_tank");
            this.unknownRadiationTankCapacity = builder
                    .comment("Capacity of the dedicated unknown radiation substance tank, in mB.")
                    .defineInRange("capacity", 4_000_000L, 1L, Long.MAX_VALUE);
            builder.pop();
        }
    }

    public static boolean gasTapEnabled() {
        return COMMON.enableGasTap.get();
    }

    public static boolean allowOnDedicatedServer() {
        return COMMON.allowOnDedicatedServer.get();
    }

    public static long maxRatePerTick() {
        return COMMON.maxRatePerTick.get();
    }

    public static long defaultRatePerTick() {
        return COMMON.defaultRatePerTick.get();
    }

    /** Returns true when the gas id is allowed by the configured filter. */
    public static boolean isGasAllowed(@Nullable ResourceLocation gasId) {
        if (gasId == null) {
            return false;
        }
        boolean listed = COMMON.gasFilterList.get().contains(gasId.toString());
        return COMMON.gasFilterMode.get() == FilterMode.BLACKLIST ? !listed : listed;
    }

    /** Returns false when the dimension id is listed in the blacklist. */
    public static boolean isDimensionAllowed(@Nullable ResourceLocation dimensionId) {
        if (dimensionId == null) {
            return true;
        }
        return !COMMON.dimensionBlacklist.get().contains(dimensionId.toString());
    }

    public static boolean radiationSpongeEnabled() {
        return COMMON.enableRadiationSponge.get();
    }

    public static boolean allowRadiationSpongeOnDedicatedServer() {
        return COMMON.allowRadiationSpongeOnDedicatedServer.get();
    }

    public static int radiationSpongeRadiusChunks() {
        return COMMON.radiationSpongeRadiusChunks.get();
    }

    public static double radiationSpongeMinRadiation() {
        return COMMON.radiationSpongeMinRadiation.get();
    }

    public static boolean spongeClearsPlayerRadiation() {
        return COMMON.spongeClearsPlayerRadiation.get();
    }

    public static double spongePlayerDoseToChunks() {
        return COMMON.spongePlayerDoseToChunks.get();
    }

    public static double spongePlayerDoseFactor() {
        return COMMON.spongePlayerDoseFactor.get();
    }

    public static long playerYieldCap() {
        return COMMON.playerYieldCap.get();
    }

    public static int spongeNauseaTicks() {
        return COMMON.spongeNauseaTicks.get();
    }

    public static int spongeNauseaAmplifier() {
        return COMMON.spongeNauseaAmplifier.get();
    }

    public static boolean radiationMachinesEnabled() {
        return COMMON.enableRadiationMachines.get();
    }

    public static int gasPerChunk() {
        return COMMON.gasPerChunk.get();
    }

    public static double radiationGasFactor() {
        return COMMON.radiationGasFactor.get();
    }

    public static long yieldEquilibrium() {
        return COMMON.yieldEquilibrium.get();
    }

    public static double overloadSteepness() {
        return COMMON.overloadSteepness.get();
    }

    public static long extractorTankCapacity() {
        return COMMON.extractorTankCapacity.get();
    }

    public static long fractionatorTankCapacity() {
        return COMMON.fractionatorTankCapacity.get();
    }

    public static double unknownRadiationStrength() {
        return COMMON.unknownRadiationStrength.get();
    }

    public static long unknownRadiationTankCapacity() {
        return COMMON.unknownRadiationTankCapacity.get();
    }

    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SPEC, "infwhat_mekanism.toml");
    }

    private MekanismAdditionConfig() {
    }
}
