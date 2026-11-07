package com.bandybus.infwhat.mekanism.network;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.entity.ChemicalTapTileEntity;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalType;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.DistExecutor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/** Server-authoritative packets shared by every chemical tap machine. */
public final class MekanismAdditionNetwork {

    private static final String PROTOCOL = "1";

    public static final ResourceLocation SET_CHEMICAL =
            new ResourceLocation(InfinityWhatMekanism.MOD_ID, "set_chemical");
    public static final ResourceLocation CHEMICAL_RESULT =
            new ResourceLocation(InfinityWhatMekanism.MOD_ID, "chemical_result");
    public static final ResourceLocation SET_RATE =
            new ResourceLocation(InfinityWhatMekanism.MOD_ID, "set_rate");
    public static final ResourceLocation RATE_RESULT =
            new ResourceLocation(InfinityWhatMekanism.MOD_ID, "rate_result");
    public static final ResourceLocation SET_OUTPUT =
            new ResourceLocation(InfinityWhatMekanism.MOD_ID, "set_output");
    public static final ResourceLocation CLEAR_CHEMICAL =
            new ResourceLocation(InfinityWhatMekanism.MOD_ID, "clear_chemical");

    private MekanismAdditionNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MekanismAdditionNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(InfinityWhatMekanism.MOD_ID)
                .versioned(PROTOCOL);
        registrar.play(SET_CHEMICAL, SetChemicalPacket::decode,
                (packet, context) -> context.workHandler()
                        .execute(() -> handleSetChemical(packet, context)));
        registrar.play(CHEMICAL_RESULT, ChemicalResultPacket::decode,
                (packet, context) -> context.workHandler()
                        .execute(() -> handleChemicalResult(packet, context)));
        registrar.play(SET_RATE, SetRatePacket::decode,
                (packet, context) -> context.workHandler()
                        .execute(() -> handleSetRate(packet, context)));
        registrar.play(RATE_RESULT, RateResultPacket::decode,
                (packet, context) -> context.workHandler()
                        .execute(() -> handleRateResult(packet, context)));
        registrar.play(SET_OUTPUT, SetOutputPacket::decode,
                (packet, context) -> context.workHandler()
                        .execute(() -> handleSetOutput(packet, context)));
        registrar.play(CLEAR_CHEMICAL, ClearChemicalPacket::decode,
                (packet, context) -> context.workHandler()
                        .execute(() -> handleClearChemical(packet, context)));
    }

    public static void sendChemical(int containerId, ChemicalType type, ResourceLocation id) {
        PacketDistributor.SERVER.noArg().send(new SetChemicalPacket(containerId, type, id));
    }

    public static void sendRate(int containerId, long amount) {
        PacketDistributor.SERVER.noArg().send(new SetRatePacket(containerId, amount));
    }

    public static void sendOutputEnabled(int containerId, boolean enabled) {
        PacketDistributor.SERVER.noArg().send(new SetOutputPacket(containerId, enabled));
    }

    public static void sendClearChemical(int containerId) {
        PacketDistributor.SERVER.noArg().send(new ClearChemicalPacket(containerId));
    }

    @Nullable
    private static ServerPlayer player(IPayloadContext context) {
        return context.player().orElse(null) instanceof ServerPlayer serverPlayer
                ? serverPlayer : null;
    }

    @Nullable
    private static ChemicalTapTileEntity<?, ?, ?> validTile(ServerPlayer player, int containerId) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != containerId
                || !(menu instanceof MekanismTileContainer<?> tileMenu)) {
            return null;
        }
        if (!menu.stillValid(player)) {
            return null;
        }
        return tileMenu.getTileEntity() instanceof ChemicalTapTileEntity<?, ?, ?> tap
                ? tap : null;
    }

    private static void handleSetChemical(SetChemicalPacket packet, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        ServerPlayer player = player(context);
        if (player == null) {
            return;
        }
        ChemicalTapTileEntity<?, ?, ?> tap = validTile(player, packet.containerId());
        Chemical<?> chemical = ChemicalTapTileEntity.findChemical(packet.type(),
                packet.chemicalId());
        if (chemical == null) {
            chemical = ChemicalTapTileEntity.findChemicalAnyType(packet.chemicalId());
        }
        boolean success = tap != null && chemical != null
                && tap.setSelectedChemical(chemical);
        if (!success) {
            InfinityWhatMekanism.LOGGER.info(
                    "Chemical selection rejected: container={} type={} id={} tile={} chemical={}",
                    packet.containerId(), packet.type(), packet.chemicalId(), tap != null,
                    chemical != null);
        }
        PacketDistributor.PLAYER.with(player)
                .send(new ChemicalResultPacket(packet.containerId(), success));
    }

    private static void handleChemicalResult(ChemicalResultPacket packet, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.bandybus.infwhat.mekanism.client.network.GasTapClientHandler
                        .handleChemical(packet.containerId(), packet.success()));
    }

    private static void handleSetRate(SetRatePacket packet, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        ServerPlayer player = player(context);
        if (player == null) {
            return;
        }
        ChemicalTapTileEntity<?, ?, ?> tap = validTile(player, packet.containerId());
        boolean success = false;
        long accepted = 0L;
        if (tap != null && packet.amount() >= 1L
                && packet.amount() <= MekanismAdditionConfig.maxRatePerTick()) {
            tap.setOutputAmountPerTick(packet.amount());
            accepted = tap.getOutputAmountPerTick();
            success = true;
        }
        PacketDistributor.PLAYER.with(player)
                .send(new RateResultPacket(packet.containerId(), success, accepted));
    }

    private static void handleRateResult(RateResultPacket packet, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.bandybus.infwhat.mekanism.client.network.GasTapClientHandler
                        .handleRate(packet.containerId(), packet.success(), packet.amount()));
    }

    private static void handleSetOutput(SetOutputPacket packet, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        ServerPlayer player = player(context);
        if (player == null) {
            return;
        }
        ChemicalTapTileEntity<?, ?, ?> tap = validTile(player, packet.containerId());
        if (tap != null && !(packet.enabled() && tap.isOutputLocked())) {
            tap.setOutputEnabled(packet.enabled());
        }
    }

    private static void handleClearChemical(ClearChemicalPacket packet, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        ServerPlayer player = player(context);
        if (player == null) {
            return;
        }
        ChemicalTapTileEntity<?, ?, ?> tap = validTile(player, packet.containerId());
        if (tap != null) {
            tap.clearChemical();
        }
    }

    /**
     * The chemical id component must not be called {@code id}: in NeoForge 1.20.4
     * {@link CustomPacketPayload#id()} is the payload type, and a record component with the same
     * name is shadowed by that override, which made every selection arrive as
     * {@code infwhat_mekanism:set_chemical}.
     */
    private record SetChemicalPacket(int containerId, ChemicalType type,
                                     ResourceLocation chemicalId)
            implements CustomPacketPayload {

        private static SetChemicalPacket decode(FriendlyByteBuf buffer) {
            return new SetChemicalPacket(buffer.readVarInt(), buffer.readEnum(ChemicalType.class),
                    buffer.readResourceLocation());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeEnum(this.type);
            buffer.writeResourceLocation(this.chemicalId);
        }

        @Override
        public ResourceLocation id() {
            return SET_CHEMICAL;
        }
    }

    private record ChemicalResultPacket(int containerId, boolean success)
            implements CustomPacketPayload {

        private static ChemicalResultPacket decode(FriendlyByteBuf buffer) {
            return new ChemicalResultPacket(buffer.readVarInt(), buffer.readBoolean());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeBoolean(this.success);
        }

        @Override
        public ResourceLocation id() {
            return CHEMICAL_RESULT;
        }
    }

    private record SetRatePacket(int containerId, long amount) implements CustomPacketPayload {

        private static SetRatePacket decode(FriendlyByteBuf buffer) {
            return new SetRatePacket(buffer.readVarInt(), buffer.readLong());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeLong(this.amount);
        }

        @Override
        public ResourceLocation id() {
            return SET_RATE;
        }
    }

    private record RateResultPacket(int containerId, boolean success, long amount)
            implements CustomPacketPayload {

        private static RateResultPacket decode(FriendlyByteBuf buffer) {
            return new RateResultPacket(buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readLong());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeBoolean(this.success);
            buffer.writeLong(this.amount);
        }

        @Override
        public ResourceLocation id() {
            return RATE_RESULT;
        }
    }

    private record SetOutputPacket(int containerId, boolean enabled)
            implements CustomPacketPayload {

        private static SetOutputPacket decode(FriendlyByteBuf buffer) {
            return new SetOutputPacket(buffer.readVarInt(), buffer.readBoolean());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeBoolean(this.enabled);
        }

        @Override
        public ResourceLocation id() {
            return SET_OUTPUT;
        }
    }

    private record ClearChemicalPacket(int containerId) implements CustomPacketPayload {

        private static ClearChemicalPacket decode(FriendlyByteBuf buffer) {
            return new ClearChemicalPacket(buffer.readVarInt());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
        }

        @Override
        public ResourceLocation id() {
            return CLEAR_CHEMICAL;
        }
    }
}
