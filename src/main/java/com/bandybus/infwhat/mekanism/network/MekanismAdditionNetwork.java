package com.bandybus.infwhat.mekanism.network;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.entity.ChemicalTapTileEntity;
import com.bandybus.infwhat.mekanism.config.MekanismAdditionConfig;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalType;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** Server-authoritative packets shared by every chemical tap machine. */
public final class MekanismAdditionNetwork {

    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(InfinityWhatMekanism.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private MekanismAdditionNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, SetChemicalPacket.class,
                SetChemicalPacket::encode, SetChemicalPacket::decode,
                SetChemicalPacket::handle);
        CHANNEL.registerMessage(1, ChemicalResultPacket.class,
                ChemicalResultPacket::encode, ChemicalResultPacket::decode,
                ChemicalResultPacket::handle);
        CHANNEL.registerMessage(2, SetRatePacket.class,
                SetRatePacket::encode, SetRatePacket::decode, SetRatePacket::handle);
        CHANNEL.registerMessage(3, RateResultPacket.class,
                RateResultPacket::encode, RateResultPacket::decode, RateResultPacket::handle);
        CHANNEL.registerMessage(4, SetOutputPacket.class,
                SetOutputPacket::encode, SetOutputPacket::decode, SetOutputPacket::handle);
        CHANNEL.registerMessage(5, ClearChemicalPacket.class,
                ClearChemicalPacket::encode, ClearChemicalPacket::decode,
                ClearChemicalPacket::handle);
    }

    public static void sendChemical(int containerId, ChemicalType type, ResourceLocation id) {
        CHANNEL.sendToServer(new SetChemicalPacket(containerId, type, id));
    }

    public static void sendRate(int containerId, long amount) {
        CHANNEL.sendToServer(new SetRatePacket(containerId, amount));
    }

    public static void sendOutputEnabled(int containerId, boolean enabled) {
        CHANNEL.sendToServer(new SetOutputPacket(containerId, enabled));
    }

    public static void sendClearChemical(int containerId) {
        CHANNEL.sendToServer(new ClearChemicalPacket(containerId));
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

    private record SetChemicalPacket(int containerId, ChemicalType type, ResourceLocation id) {

        private static void encode(SetChemicalPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeEnum(packet.type);
            buffer.writeResourceLocation(packet.id);
        }

        private static SetChemicalPacket decode(FriendlyByteBuf buffer) {
            return new SetChemicalPacket(buffer.readVarInt(), buffer.readEnum(ChemicalType.class),
                    buffer.readResourceLocation());
        }

        private static void handle(SetChemicalPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                ChemicalTapTileEntity<?, ?, ?> tap = validTile(player, packet.containerId);
                Chemical<?> chemical = ChemicalTapTileEntity.findChemical(packet.type, packet.id);
                if (chemical == null) {
                    chemical = ChemicalTapTileEntity.findChemicalAnyType(packet.id);
                }
                boolean success = tap != null && chemical != null
                        && tap.setSelectedChemical(chemical);
                if (!success) {
                    InfinityWhatMekanism.LOGGER.info(
                            "Chemical selection rejected: container={} type={} id={} tile={} chemical={}",
                            packet.containerId, packet.type, packet.id, tap != null,
                            chemical != null);
                }
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new ChemicalResultPacket(packet.containerId, success));
            });
            context.setPacketHandled(true);
        }
    }

    private record ChemicalResultPacket(int containerId, boolean success) {

        private static void encode(ChemicalResultPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeBoolean(packet.success);
        }

        private static ChemicalResultPacket decode(FriendlyByteBuf buffer) {
            return new ChemicalResultPacket(buffer.readVarInt(), buffer.readBoolean());
        }

        private static void handle(ChemicalResultPacket packet,
                                   Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bandybus.infwhat.mekanism.client.network.GasTapClientHandler
                            .handleChemical(packet.containerId, packet.success)));
            context.setPacketHandled(true);
        }
    }

    private record SetRatePacket(int containerId, long amount) {

        private static void encode(SetRatePacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeLong(packet.amount);
        }

        private static SetRatePacket decode(FriendlyByteBuf buffer) {
            return new SetRatePacket(buffer.readVarInt(), buffer.readLong());
        }

        private static void handle(SetRatePacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                ChemicalTapTileEntity<?, ?, ?> tap = validTile(player, packet.containerId);
                boolean success = false;
                long accepted = 0L;
                if (tap != null && packet.amount >= 1L
                        && packet.amount <= MekanismAdditionConfig.maxRatePerTick()) {
                    tap.setOutputAmountPerTick(packet.amount);
                    accepted = tap.getOutputAmountPerTick();
                    success = true;
                }
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new RateResultPacket(packet.containerId, success, accepted));
            });
            context.setPacketHandled(true);
        }
    }

    private record RateResultPacket(int containerId, boolean success, long amount) {

        private static void encode(RateResultPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeBoolean(packet.success);
            buffer.writeLong(packet.amount);
        }

        private static RateResultPacket decode(FriendlyByteBuf buffer) {
            return new RateResultPacket(buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readLong());
        }

        private static void handle(RateResultPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bandybus.infwhat.mekanism.client.network.GasTapClientHandler
                            .handleRate(packet.containerId, packet.success, packet.amount)));
            context.setPacketHandled(true);
        }
    }

    private record SetOutputPacket(int containerId, boolean enabled) {

        private static void encode(SetOutputPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeBoolean(packet.enabled);
        }

        private static SetOutputPacket decode(FriendlyByteBuf buffer) {
            return new SetOutputPacket(buffer.readVarInt(), buffer.readBoolean());
        }

        private static void handle(SetOutputPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                ChemicalTapTileEntity<?, ?, ?> tap = validTile(player, packet.containerId);
                if (tap != null && !(packet.enabled && tap.isOutputLocked())) {
                    tap.setOutputEnabled(packet.enabled);
                }
            });
            context.setPacketHandled(true);
        }
    }

    private record ClearChemicalPacket(int containerId) {

        private static void encode(ClearChemicalPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
        }

        private static ClearChemicalPacket decode(FriendlyByteBuf buffer) {
            return new ClearChemicalPacket(buffer.readVarInt());
        }

        private static void handle(ClearChemicalPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                ChemicalTapTileEntity<?, ?, ?> tap = validTile(player, packet.containerId);
                if (tap != null) {
                    tap.clearChemical();
                }
            });
            context.setPacketHandled(true);
        }
    }
}
