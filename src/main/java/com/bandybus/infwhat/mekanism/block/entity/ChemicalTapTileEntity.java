package com.bandybus.infwhat.mekanism.block.entity;

import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import mekanism.api.MekanismAPI;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.IChemicalTank;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shared implementation for every "infinite chemical" machine.
 *
 * <p>Two modes exist:
 * <ul>
 *     <li><b>Selection</b>: the player picks a chemical from the registry list (the chemical
 *     generator uses this).</li>
 *     <li><b>Import</b>: the player feeds the machine through a slot or an adjacent pipe and
 *     the machine locks onto that type (infusion and slurry generators). The type is released by
 *     the GUI's clear button, exactly like the infinite fluid generator.</li>
 * </ul>
 *
 * <p>Concrete machines only provide the tank holder plumbing; all selection, rate, budget,
 * locking and synchronisation logic lives here.
 */
public abstract class ChemicalTapTileEntity extends TileEntityConfigurableMachine {

    public static final long REPORTED_AMOUNT = Long.MAX_VALUE / 2;
    public static final long DEFAULT_RATE = 1_000_000L;

    protected static final String KEY_CHEMICAL = "Chemical";
    protected static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    protected static final String KEY_OUTPUT_RATE = "OutputRate";
    protected static final String KEY_ACTIVE = "Active";
    /** Legacy key used by the gas only build; still read so old worlds keep their selection. */
    protected static final String KEY_LEGACY_GAS = "Gas";

    protected IChemicalTank tank;

    private String selectedId = "";
    @Nullable
    private Chemical selectedChemical;
    private boolean outputEnabled = true;
    private long outputAmountPerTick = MekanismAdditionConfig.defaultRatePerTick();
    private boolean active;

    private long lastBudgetTick = -1L;
    private long extractedThisTick;

    protected ChemicalTapTileEntity(Holder<Block> provider, BlockPos pos, BlockState state) {
        super(provider, pos, state);
    }

    // ------------------------------------------------------------------
    // Hooks for the concrete machines
    // ------------------------------------------------------------------

    /** True for import driven machines (infusion, slurry). */
    public boolean isFillMode() {
        return false;
    }

    /** Extra per chemical validation; the chemical generator uses it for containment gating. */
    protected boolean isChemicalAllowed(Chemical chemical) {
        return true;
    }

    public static Registry<Chemical> registry() {
        return MekanismAPI.CHEMICAL_REGISTRY;
    }

    @Nullable
    public static ResourceLocation idOf(@Nullable Chemical chemical) {
        return chemical == null ? null : registry().getKey(chemical);
    }

    // ------------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------------

    @Nullable
    public Chemical getSelectedChemical() {
        if (this.selectedChemical == null && !this.selectedId.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(this.selectedId);
            if (id != null) {
                Chemical chemical = findChemical(id);
                if (chemical != null) {
                    this.selectedChemical = chemical;
                }
            }
        }
        return this.selectedChemical;
    }

    @Nullable
    public static Chemical findChemical(ResourceLocation id) {
        Chemical chemical = registry().get(id);
        return chemical == null || chemical == MekanismAPI.EMPTY_CHEMICAL ? null : chemical;
    }

    public String getSelectedChemicalId() {
        return this.selectedId;
    }

    /** Sets the selected chemical. Returns false when the chemical is not allowed. */
    public boolean setSelectedChemical(@Nullable Chemical chemical) {
        if (chemical == null) {
            clearChemical();
            return true;
        }
        if (chemical.isEmptyType() || !isChemicalAllowed(chemical)) {
            InfinityWhatMekanism.LOGGER.info(
                    "Selection reject A: empty={} id={} allowed={} containment={}",
                    chemical.isEmptyType(), idOf(chemical), isChemicalAllowed(chemical),
                    hasContainmentUpgrade());
            return false;
        }
        ResourceLocation id = idOf(chemical);
        if (id == null) {
            InfinityWhatMekanism.LOGGER.info("Selection reject B: chemical has no registry id");
            return false;
        }
        this.selectedChemical = chemical;
        this.selectedId = id.toString();
        resetBudget();
        markUpdated();
        return true;
    }

    /** Used by the import driven tanks when they receive a chemical. */
    public void lockChemical(Chemical chemical) {
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

    /** Chemicals offered by the selection GUI. */
    public List<Chemical> getAvailableChemicals() {
        return collectChemicals();
    }

    protected List<Chemical> collectChemicals() {
        List<Chemical> chemicals = new ArrayList<>();
        for (Chemical chemical : registry().stream().toList()) {
            ResourceLocation id = idOf(chemical);
            if (chemical == MekanismAPI.EMPTY_CHEMICAL || id == null
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
        Chemical chemical = getSelectedChemical();
        return chemical != null && isChemicalAllowed(chemical);
    }

    public boolean isGasAllowedById(ResourceLocation id) {
        return MekanismAdditionConfig.isGasAllowed(id);
    }

    /** True while the output toggle must stay off; overridden by the chemical generator. */
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
        Chemical chemical = getSelectedChemical();
        if (chemical == null || !this.outputEnabled || !isChemicalPermitted()
                || !MekanismAdditionConfig.gasTapEnabled()
                || !InfinityWhatConfig.infiniteEnabled()
                || !canFunction()) {
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
        return hasOutputSide();
    }

    /** True when the chemical transmission type has at least one output face configured. */
    protected boolean hasOutputSide() {
        ConfigInfo info = getConfig().getConfig(TransmissionType.CHEMICAL);
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
        //The synthetic tank is never saved; the locked type lives in our own fields.
        if (type == ContainerType.CHEMICAL) {
            return false;
        }
        return super.persists(type);
    }

    @Override
    public boolean shouldDumpRadiation() {
        return false;
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        writeSyncData(tag);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        readSyncData(tag);
        resetBudget();
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag tag = super.getReducedUpdateTag(provider);
        writeSyncData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        readSyncData(tag);
        resetBudget();
    }

    private void writeSyncData(CompoundTag tag) {
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
    public CompoundTag getConfigurationData(@NotNull HolderLookup.Provider provider,
                                            @NotNull Player player) {
        CompoundTag data = super.getConfigurationData(provider, player);
        data.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        data.putLong(KEY_OUTPUT_RATE, this.outputAmountPerTick);
        data.putString(KEY_CHEMICAL, this.selectedId);
        return data;
    }

    @Override
    public void setConfigurationData(@NotNull HolderLookup.Provider provider,
                                     @NotNull Player player, CompoundTag data) {
        super.setConfigurationData(provider, player, data);
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
        Chemical chemical = findChemical(id);
        if (chemical != null) {
            setSelectedChemical(chemical);
        }
    }

    private void markUpdated() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    public IChemicalTank getTank() {
        return this.tank;
    }
}
