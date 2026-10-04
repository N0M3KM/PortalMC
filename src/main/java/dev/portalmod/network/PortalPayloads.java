package dev.portalmod.network;

import dev.portalmod.config.ConfigManager;
import dev.portalmod.portal.PortalConfig;
import dev.portalmod.portal.PortalFrame;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class PortalPayloads {
    public record Shot(boolean orange, float yaw, float pitch) implements CustomPacketPayload {
        public static final Type<Shot> TYPE = new Type<>(Identifier.fromNamespaceAndPath("portalmod", "shot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Shot> CODEC = new StreamCodec<>() {
            public Shot decode(RegistryFriendlyByteBuf b) { return new Shot(b.readBoolean(), b.readFloat(), b.readFloat()); }
            public void encode(RegistryFriendlyByteBuf b, Shot s) { b.writeBoolean(s.orange); b.writeFloat(s.yaw); b.writeFloat(s.pitch); }
        };
        public Type<Shot> type() { return TYPE; }
    }
    public record Snapshot(String json) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(Identifier.fromNamespaceAndPath("portalmod", "portals"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = new StreamCodec<>() {
            public Snapshot decode(RegistryFriendlyByteBuf b) { return new Snapshot(b.readUtf(524288)); }
            public void encode(RegistryFriendlyByteBuf b, Snapshot s) { b.writeUtf(s.json, 524288); }
        };
        public Type<Snapshot> type() { return TYPE; }
    }
    public record Data(PortalConfig config, List<PortalFrame> portals) { }
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Shot.TYPE, Shot.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE, Snapshot.CODEC);
    }
    private PortalPayloads() { }
}
