package com.bandybus.infwhat.mekanism.network;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.entity.ChemicalTapTileEntity;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import mekanism.api.chemical.Chemical;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/** Server-authoritative packets shared by every chemical tap machine. */
public final class MekanismAdditionNetwork {

    private static final String PROTOCOL = "1";

    public static final CustomPacketPayload.Type<SetChemicalPacket> SET_CHEMICAL =
            type("set_chemical");
    public static final CustomPacketPayload.Type<ChemicalResultPacket> CHEMICAL_RESULT =
            type("chemical_result");
    public static final CustomPacketPayload.Type<SetRatePacket> SET_RATE =
            type("set_rate");
    public static final CustomPacketPayload.Type<RateResultPacket> RATE_RESULT =
            type("rate_result");
    public static final CustomPacketPayload.Type<SetOutputPacket> SET_OUTPUT =
            type("set_output");
    public static final CustomPacketPayload.Type<ClearChemicalPacket> CLEAR_CHEMICAL =
            type("clear_chemical");

    /**
     * Builds a payload type with the add-on namespace. Passing a full {@code namespace:path}
     * string to {@link CustomPacketPayload#createType(String)} makes vanilla prepend
     * {@code minecraft:}, which produces an invalid location and crashes the mod during
     * construction.
     */
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(InfinityWhatMekanism.MOD_ID, path));
    }

    private MekanismAdditionNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MekanismAdditionNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(InfinityWhatMekanism.MOD_ID)
                .versioned(PROTOCOL);
        registrar.playToServer(SET_CHEMICAL, SetChemicalPacket.STREAM_CODEC,
                MekanismAdditionNetwork::handleSetChemical);
        registrar.playToClient(CHEMICAL_RESULT, ChemicalResultPacket.STREAM_CODEC,
                MekanismAdditionNetwork::handleChemicalResult);
        registrar.playToServer(SET_RATE, SetRatePacket.STREAM_CODEC,
                MekanismAdditionNetwork::handleSetRate);
        registrar.playToClient(RATE_RESULT, RateResultPacket.STREAM_CODEC,
                MekanismAdditionNetwork::handleRateResult);
        registrar.playToServer(SET_OUTPUT, SetOutputPacket.STREAM_CODEC,
                MekanismAdditionNetwork::handleSetOutput);
        registrar.playToServer(CLEAR_CHEMICAL, ClearChemicalPacket.STREAM_CODEC,
                MekanismAdditionNetwork::handleClearChemical);
    }

    public static void sendChemical(int containerId, ResourceLocation id) {
        PacketDistributor.sendToServer(new SetChemicalPacket(containerId, id));
    }

    public static void sendRate(int containerId, long amount) {
        PacketDistributor.sendToServer(new SetRatePacket(containerId, amount));
    }

    public static void sendOutputEnabled(int containerId, boolean enabled) {
        PacketDistributor.sendToServer(new SetOutputPacket(containerId, enabled));
    }

    public static void sendClearChemical(int containerId) {
        PacketDistributor.sendToServer(new ClearChemicalPacket(containerId));
    }

    @Nullable
    private static ServerPlayer player(IPayloadContext context) {
        return context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    @Nullable
    private static ChemicalTapTileEntity validTile(ServerPlayer player, int containerId) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != containerId
                || !(menu instanceof MekanismTileContainer<?> tileMenu)) {
            return null;
        }
        if (!menu.stillValid(player)) {
            return null;
        }
        return tileMenu.getTileEntity() instanceof ChemicalTapTileEntity tap ? tap : null;
    }

    private static void handleSetChemical(SetChemicalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null) {
                return;
            }
            ChemicalTapTileEntity tap = validTile(player, packet.containerId());
            Chemical chemical = ChemicalTapTileEntity.findChemical(packet.id());
            boolean success = tap != null && chemical != null
                    && tap.setSelectedChemical(chemical);
            if (!success) {
                InfinityWhatMekanism.LOGGER.info(
                        "Chemical selection rejected: container={} id={} tile={} chemical={}",
                        packet.containerId(), packet.id(), tap != null, chemical != null);
            }
            PacketDistributor.sendToPlayer(player,
                    new ChemicalResultPacket(packet.containerId(), success));
        });
    }

    private static void handleChemicalResult(ChemicalResultPacket packet,
                                             IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.bandybus.infwhat.mekanism.client.network.GasTapClientHandler
                        .handleChemical(packet.containerId(), packet.success());
            }
        });
    }

    private static void handleSetRate(SetRatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null) {
                return;
            }
            ChemicalTapTileEntity tap = validTile(player, packet.containerId());
            boolean success = false;
            long accepted = 0L;
            if (tap != null && packet.amount() >= 1L
                    && packet.amount() <= MekanismAdditionConfig.maxRatePerTick()) {
                tap.setOutputAmountPerTick(packet.amount());
                accepted = tap.getOutputAmountPerTick();
                success = true;
            }
            PacketDistributor.sendToPlayer(player,
                    new RateResultPacket(packet.containerId(), success, accepted));
        });
    }

    private static void handleRateResult(RateResultPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.bandybus.infwhat.mekanism.client.network.GasTapClientHandler
                        .handleRate(packet.containerId(), packet.success(), packet.amount());
            }
        });
    }

    private static void handleSetOutput(SetOutputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null) {
                return;
            }
            ChemicalTapTileEntity tap = validTile(player, packet.containerId());
            if (tap != null && !(packet.enabled() && tap.isOutputLocked())) {
                tap.setOutputEnabled(packet.enabled());
            }
        });
    }

    private static void handleClearChemical(ClearChemicalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null) {
                return;
            }
            ChemicalTapTileEntity tap = validTile(player, packet.containerId());
            if (tap != null) {
                tap.clearChemical();
            }
        });
    }

    public record SetChemicalPacket(int containerId, ResourceLocation id)
            implements CustomPacketPayload {

        public static final StreamCodec<RegistryFriendlyByteBuf, SetChemicalPacket> STREAM_CODEC =
                CustomPacketPayload.codec(SetChemicalPacket::write, SetChemicalPacket::decode);

        private static SetChemicalPacket decode(RegistryFriendlyByteBuf buffer) {
            return new SetChemicalPacket(buffer.readVarInt(), buffer.readResourceLocation());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeResourceLocation(this.id);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return SET_CHEMICAL;
        }
    }

    public record ChemicalResultPacket(int containerId, boolean success)
            implements CustomPacketPayload {

        public static final StreamCodec<RegistryFriendlyByteBuf, ChemicalResultPacket> STREAM_CODEC =
                CustomPacketPayload.codec(ChemicalResultPacket::write,
                        ChemicalResultPacket::decode);

        private static ChemicalResultPacket decode(RegistryFriendlyByteBuf buffer) {
            return new ChemicalResultPacket(buffer.readVarInt(), buffer.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeBoolean(this.success);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return CHEMICAL_RESULT;
        }
    }

    public record SetRatePacket(int containerId, long amount) implements CustomPacketPayload {

        public static final StreamCodec<RegistryFriendlyByteBuf, SetRatePacket> STREAM_CODEC =
                CustomPacketPayload.codec(SetRatePacket::write, SetRatePacket::decode);

        private static SetRatePacket decode(RegistryFriendlyByteBuf buffer) {
            return new SetRatePacket(buffer.readVarInt(), buffer.readLong());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeLong(this.amount);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return SET_RATE;
        }
    }

    public record RateResultPacket(int containerId, boolean success, long amount)
            implements CustomPacketPayload {

        public static final StreamCodec<RegistryFriendlyByteBuf, RateResultPacket> STREAM_CODEC =
                CustomPacketPayload.codec(RateResultPacket::write, RateResultPacket::decode);

        private static RateResultPacket decode(RegistryFriendlyByteBuf buffer) {
            return new RateResultPacket(buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readLong());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeBoolean(this.success);
            buffer.writeLong(this.amount);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return RATE_RESULT;
        }
    }

    public record SetOutputPacket(int containerId, boolean enabled)
            implements CustomPacketPayload {

        public static final StreamCodec<RegistryFriendlyByteBuf, SetOutputPacket> STREAM_CODEC =
                CustomPacketPayload.codec(SetOutputPacket::write, SetOutputPacket::decode);

        private static SetOutputPacket decode(RegistryFriendlyByteBuf buffer) {
            return new SetOutputPacket(buffer.readVarInt(), buffer.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeBoolean(this.enabled);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return SET_OUTPUT;
        }
    }

    public record ClearChemicalPacket(int containerId) implements CustomPacketPayload {

        public static final StreamCodec<RegistryFriendlyByteBuf, ClearChemicalPacket> STREAM_CODEC =
                CustomPacketPayload.codec(ClearChemicalPacket::write, ClearChemicalPacket::decode);

        private static ClearChemicalPacket decode(RegistryFriendlyByteBuf buffer) {
            return new ClearChemicalPacket(buffer.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return CLEAR_CHEMICAL;
        }
    }
}
