package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import mekanism.api.IContentsListener;
import mekanism.api.MekanismAPI;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Shared implementation for every "infinite chemical" machine.
 *
 * <p>Two modes exist:
 * <ul>
 *     <li><b>Selection</b>: the player picks a chemical from the registry list (the chemical
 *     generator uses this for gases and pigments).</li>
 *     <li><b>Import</li></b>: the player feeds the machine through a slot or an adjacent pipe and
 *     the machine locks onto that type (infusion and slurry generators). The type is released by
 *     the GUI's clear button, exactly like the infinite fluid generator.</li>
 * </ul>
 *
 * <p>Concrete machines only provide the tank holder plumbing and the typed tank instance; all
 * selection, rate, budget, locking and synchronisation logic lives here.
 */
public abstract class ChemicalTapTileEntity<C extends Chemical<C>, S extends ChemicalStack<C>,
        T extends IChemicalTank<C, S>> extends TileEntityConfigurableMachine {

    public static final long REPORTED_AMOUNT = Long.MAX_VALUE / 2;
    public static final long DEFAULT_RATE = 1_000_000L;

    protected static final String KEY_CHEMICAL_TYPE = "ChemicalType";
    protected static final String KEY_CHEMICAL = "Chemical";
    protected static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    protected static final String KEY_OUTPUT_RATE = "OutputRate";
    protected static final String KEY_ACTIVE = "Active";
    /** Legacy key used by the gas only build; still read so old worlds keep their selection. */
    protected static final String KEY_LEGACY_GAS = "Gas";

    private final ChemicalType primaryType;
    protected T tank;

    private String selectedId = "";
    @Nullable
    private Chemical<?> selectedChemical;
    private boolean outputEnabled = true;
    private long outputAmountPerTick = MekanismAdditionConfig.defaultRatePerTick();
    private boolean active;

    private long lastBudgetTick = -1L;
    private long extractedThisTick;

    protected ChemicalTapTileEntity(IBlockProvider provider, BlockPos pos, BlockState state,
                                    ChemicalType primaryType) {
        super(provider, pos, state);
        this.primaryType = primaryType;
    }

    // ------------------------------------------------------------------
    // Hooks for the concrete machines
    // ------------------------------------------------------------------

    /** The chemical types this machine can source. */
    protected Set<ChemicalType> getSupportedTypes() {
        return Set.of(this.primaryType);
    }

    public ChemicalType getPrimaryChemicalType() {
        return this.primaryType;
    }

    /** True for import driven machines (infusion, slurry). */
    public boolean isFillMode() {
        return false;
    }

    /** The transmission type used for the output side configuration of the given type. */
    public TransmissionType getTransmissionType(ChemicalType type) {
        return switch (type) {
            case GAS -> TransmissionType.GAS;
            case INFUSION -> TransmissionType.INFUSION;
            case PIGMENT -> TransmissionType.PIGMENT;
            case SLURRY -> TransmissionType.SLURRY;
        };
    }

    /** Extra per chemical validation; the gas machine uses it for containment gating. */
    protected boolean isChemicalAllowed(Chemical<?> chemical) {
        return true;
    }

    @Nullable
    public static Registry<? extends Chemical<?>> registryFor(ChemicalType type) {
        return switch (type) {
            case GAS -> MekanismAPI.GAS_REGISTRY;
            case INFUSION -> MekanismAPI.INFUSE_TYPE_REGISTRY;
            case PIGMENT -> MekanismAPI.PIGMENT_REGISTRY;
            case SLURRY -> MekanismAPI.SLURRY_REGISTRY;
        };
    }

    // ------------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------------

    @Nullable
    public Chemical<?> getSelectedChemical() {
        if (this.selectedChemical == null && !this.selectedId.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(this.selectedId);
            if (id != null) {
                for (ChemicalType type : getSupportedTypes()) {
                    Chemical<?> chemical = findChemical(type, id);
                    if (chemical != null) {
                        this.selectedChemical = chemical;
                        break;
                    }
                }
            }
        }
        return this.selectedChemical;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static Chemical<?> findChemical(ChemicalType type, ResourceLocation id) {
        Registry<? extends Chemical<?>> registry = registryFor(type);
        if (registry == null) {
            return null;
        }
        Chemical<?> chemical = registry.get(id);
        return chemical == null || chemical.isEmptyType() ? null : chemical;
    }

    /**
     * Resolves a chemical id without trusting the type field of the selection packet. Older
     * saves and mods with a shifted enum order could otherwise reject every selection even
     * though the id itself is valid.
     */
    @Nullable
    public static Chemical<?> findChemicalAnyType(ResourceLocation id) {
        for (ChemicalType type : ChemicalType.values()) {
            Chemical<?> chemical = findChemical(type, id);
            if (chemical != null) {
                return chemical;
            }
        }
        return null;
    }

    public String getSelectedChemicalId() {
        return this.selectedId;
    }

    public ChemicalType getSelectedChemicalType() {
        Chemical<?> chemical = getSelectedChemical();
        return chemical == null ? this.primaryType : ChemicalType.getTypeFor(chemical);
    }

    /** Sets the selected chemical. Returns false when the chemical is not allowed. */
    public boolean setSelectedChemical(@Nullable Chemical<?> chemical) {
        if (chemical == null) {
            clearChemical();
            return true;
        }
        if (chemical.isEmptyType() || chemical.isHidden() || chemical.getRegistryName() == null) {
            InfinityWhatMekanism.LOGGER.info(
                    "Selection reject A: empty={} hidden={} id={}",
                    chemical.isEmptyType(), chemical.isHidden(), chemical.getRegistryName());
            return false;
        }
        boolean supported = getSupportedTypes().contains(ChemicalType.getTypeFor(chemical));
        boolean allowed = isChemicalAllowed(chemical);
        if (!supported || !allowed) {
            InfinityWhatMekanism.LOGGER.info(
                    "Selection reject B: id={} type={} supported={} allowed={} containment={}",
                    chemical.getRegistryName(), ChemicalType.getTypeFor(chemical), supported,
                    allowed, hasContainmentUpgrade());
            return false;
        }
        this.selectedChemical = chemical;
        this.selectedId = chemical.getRegistryName().toString();
        resetBudget();
        markUpdated();
        return true;
    }

    /** Used by the import driven tanks when they receive a chemical. */
    public void lockChemical(Chemical<?> chemical) {
        setSelectedChemical(chemical);
    }

    /** Clears the imported chemical so another type may be fed in. */
    public void clearChemical() {
        if (this.selectedId.isEmpty() && this.selectedChemical == null) {
            return;
        }
        this.selectedChemical = null;
        this.selectedId = "";
        resetBudget();
        markUpdated();
    }

    /** Chemicals offered by the selection GUI; overridden by the gas + pigment machine. */
    public List<Chemical<?>> getAvailableChemicals() {
        return collectChemicals(this.primaryType);
    }

    protected List<Chemical<?>> collectChemicals(ChemicalType type) {
        List<Chemical<?>> chemicals = new ArrayList<>();
        Registry<? extends Chemical<?>> registry = registryFor(type);
        if (registry == null) {
            return chemicals;
        }
        for (Chemical<?> chemical : registry.stream().toList()) {
            ResourceLocation id = chemical.getRegistryName();
            if (chemical.isEmptyType() || chemical.isHidden() || id == null
                    || !MekanismAdditionConfig.isGasAllowed(id)
                    || !isChemicalAllowed(chemical)) {
                continue;
            }
            chemicals.add(chemical);
        }
        chemicals.sort(Comparator.comparing(
                chemical -> Component.translatable(chemical.getTranslationKey()).getString(),
                String.CASE_INSENSITIVE_ORDER));
        return chemicals;
    }

    public boolean isChemicalPermitted() {
        Chemical<?> chemical = getSelectedChemical();
        if (chemical == null || !getSupportedTypes().contains(ChemicalType.getTypeFor(chemical))) {
            return false;
        }
        return isChemicalAllowed(chemical);
    }

    public boolean isGasAllowedById(ResourceLocation id) {
        return MekanismAdditionConfig.isGasAllowed(id);
    }

    /** True while the output toggle must stay off; overridden by the gas machine. */
    public boolean isOutputLocked() {
        return false;
    }

    public boolean hasContainmentUpgrade() {
        return false;
    }

    // ------------------------------------------------------------------
    // Output configuration
    // ------------------------------------------------------------------

    public boolean isOutputEnabled() {
        return this.outputEnabled;
    }

    public void setOutputEnabled(boolean enabled) {
        if (this.outputEnabled == enabled) {
            return;
        }
        this.outputEnabled = enabled;
        resetBudget();
        markUpdated();
    }

    public long getOutputAmountPerTick() {
        return Math.max(1L, Math.min(this.outputAmountPerTick,
                MekanismAdditionConfig.maxRatePerTick()));
    }

    public void setOutputAmountPerTick(long amount) {
        long clamped = Math.max(1L, Math.min(amount,
                MekanismAdditionConfig.maxRatePerTick()));
        if (this.outputAmountPerTick == clamped) {
            return;
        }
        this.outputAmountPerTick = clamped;
        resetBudget();
        markUpdated();
    }

    // ------------------------------------------------------------------
    // Per tick budget shared by pipes and auto ejection
    // ------------------------------------------------------------------

    long takeBudget(long requested) {
        Level level = getLevel();
        if (level == null || requested <= 0) {
            return 0L;
        }
        long now = level.getGameTime();
        if (now != this.lastBudgetTick) {
            this.lastBudgetTick = now;
            this.extractedThisTick = 0L;
        }
        long rate = getOutputAmountPerTick();
        return Math.max(0L, Math.min(requested, rate - this.extractedThisTick));
    }

    void consumeBudget(long amount) {
        this.extractedThisTick += amount;
    }

    private void resetBudget() {
        this.lastBudgetTick = -1L;
        this.extractedThisTick = 0L;
    }

    // ------------------------------------------------------------------
    // Providing state
    // ------------------------------------------------------------------

    public boolean isProviding() {
        if (getLevel() != null && getLevel().isClientSide) {
            return this.active;
        }
        return computeActive();
    }

    protected boolean computeActive() {
        Chemical<?> chemical = getSelectedChemical();
        if (chemical == null || !this.outputEnabled || !isChemicalPermitted()
                || !MekanismAdditionConfig.gasTapEnabled()
                || !InfinityWhatConfig.infiniteEnabled()
                || !MekanismUtils.canFunction(this)) {
            return false;
        }
        Level level = getLevel();
        if (level == null) {
            return false;
        }
        if (!MekanismAdditionConfig.isDimensionAllowed(level.dimension().location())) {
            return false;
        }
        if (level.getServer() != null && level.getServer().isDedicatedServer()
                && !MekanismAdditionConfig.allowOnDedicatedServer()) {
            return false;
        }
        return hasOutputSide(ChemicalType.getTypeFor(chemical));
    }

    /** True when the given transmission type has at least one output face configured. */
    protected boolean hasOutputSide(ChemicalType type) {
        if (getConfig() == null) {
            return false;
        }
        ConfigInfo info = getConfig().getConfig(getTransmissionType(type));
        if (info == null) {
            return false;
        }
        for (RelativeSide side : RelativeSide.values()) {
            DataType dataType = info.getDataType(side);
            if (dataType == DataType.OUTPUT || dataType == DataType.INPUT_OUTPUT) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdate = super.onUpdateServer();
        if (isOutputLocked()) {
            setOutputEnabled(false);
        }
        boolean nowActive = computeActive();
        if (this.active != nowActive) {
            this.active = nowActive;
            sendUpdate = true;
        }
        setActive(nowActive);
        return sendUpdate;
    }

    // ------------------------------------------------------------------
    // Persistence and synchronisation
    // ------------------------------------------------------------------

    @Override
    public boolean persists(ContainerType<?, ?, ?> type) {
        //The synthetic tanks are never saved; the locked type lives in our own fields.
        if (type == ContainerType.GAS && getSupportedTypes().contains(ChemicalType.GAS)
                || type == ContainerType.PIGMENT && getSupportedTypes().contains(ChemicalType.PIGMENT)
                || type == ContainerType.INFUSION && getSupportedTypes().contains(ChemicalType.INFUSION)
                || type == ContainerType.SLURRY && getSupportedTypes().contains(ChemicalType.SLURRY)) {
            return false;
        }
        return super.persists(type);
    }

    @Override
    public boolean shouldDumpRadiation() {
        return false;
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        writeSyncData(tag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        readSyncData(tag);
        resetBudget();
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag() {
        CompoundTag tag = super.getReducedUpdateTag();
        writeSyncData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag) {
        super.handleUpdateTag(tag);
        readSyncData(tag);
        resetBudget();
    }

    private void writeSyncData(CompoundTag tag) {
        Chemical<?> chemical = getSelectedChemical();
        tag.putString(KEY_CHEMICAL_TYPE,
                chemical == null ? this.primaryType.name() : ChemicalType.getTypeFor(chemical).name());
        tag.putString(KEY_CHEMICAL, this.selectedId);
        tag.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        tag.putLong(KEY_OUTPUT_RATE, this.outputAmountPerTick);
        tag.putBoolean(KEY_ACTIVE, this.active);
    }

    private void readSyncData(CompoundTag tag) {
        String rawId = tag.getString(KEY_CHEMICAL);
        if (rawId.isEmpty()) {
            //Migrate the gas only build which stored the selection under "Gas".
            rawId = tag.getString(KEY_LEGACY_GAS);
        }
        this.selectedId = rawId;
        this.selectedChemical = null;
        this.outputEnabled = !tag.contains(KEY_OUTPUT_ENABLED, 1)
                || tag.getBoolean(KEY_OUTPUT_ENABLED);
        long rate = tag.contains(KEY_OUTPUT_RATE, 4)
                ? tag.getLong(KEY_OUTPUT_RATE)
                : MekanismAdditionConfig.defaultRatePerTick();
        this.outputAmountPerTick = Math.max(1L, Math.min(rate,
                MekanismAdditionConfig.maxRatePerTick()));
        this.active = tag.getBoolean(KEY_ACTIVE);
    }

    /**
     * Mekanism's own configuration card persists whatever this returns, so adding the tap's
     * output toggle, rate and selected chemical here lets the standard card copy them without
     * touching Mekanism itself.
     */
    @Override
    public CompoundTag getConfigurationData(Player player) {
        CompoundTag data = super.getConfigurationData(player);
        data.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        data.putLong(KEY_OUTPUT_RATE, this.outputAmountPerTick);
        data.putString(KEY_CHEMICAL, this.selectedId);
        return data;
    }

    @Override
    public void setConfigurationData(Player player, CompoundTag data) {
        super.setConfigurationData(player, data);
        if (data.contains(KEY_OUTPUT_ENABLED, Tag.TAG_BYTE)) {
            setOutputEnabled(data.getBoolean(KEY_OUTPUT_ENABLED));
        }
        if (data.contains(KEY_OUTPUT_RATE, Tag.TAG_LONG)) {
            setOutputAmountPerTick(data.getLong(KEY_OUTPUT_RATE));
        }
        if (data.contains(KEY_CHEMICAL, Tag.TAG_STRING)) {
            applyConfiguredChemical(data.getString(KEY_CHEMICAL));
        }
    }

    private void applyConfiguredChemical(String rawId) {
        if (rawId.isEmpty()) {
            clearChemical();
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null) {
            return;
        }
        for (ChemicalType type : getSupportedTypes()) {
            Chemical<?> chemical = findChemical(type, id);
            if (chemical != null) {
                setSelectedChemical(chemical);
                return;
            }
        }
    }

    protected void markUpdated() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    public T getTank() {
        return this.tank;
    }
}
