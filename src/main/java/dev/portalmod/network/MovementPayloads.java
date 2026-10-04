package dev.portalmod.network;

import dev.portalmod.PortalMod;
import dev.portalmod.movement.MotionState;
import dev.portalmod.movement.MovementInput;
import dev.portalmod.movement.Vector;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class MovementPayloads {
    private MovementPayloads() { }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> id(String name) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(PortalMod.MOD_ID, name));
    }

    public record Input(long epoch, long sequence, MovementInput input) implements CustomPacketPayload {
        public static final Type<Input> TYPE = id("movement_input");
        public static final StreamCodec<RegistryFriendlyByteBuf, Input> CODEC = new StreamCodec<>() {
            @Override public Input decode(RegistryFriendlyByteBuf b) {
                return new Input(b.readVarLong(), b.readVarLong(), new MovementInput(
                        b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readBoolean()));
            }
            @Override public void encode(RegistryFriendlyByteBuf b, Input p) {
                b.writeVarLong(p.epoch); b.writeVarLong(p.sequence);
                b.writeFloat(p.input.sideways()); b.writeFloat(p.input.forward());
                b.writeFloat(p.input.yaw()); b.writeFloat(p.input.pitch());
                b.writeBoolean(p.input.jump()); b.writeBoolean(p.input.crouch());
            }
        };
        @Override public Type<Input> type() { return TYPE; }
    }

    public record Settings(String json) implements CustomPacketPayload {
        public static final Type<Settings> TYPE = id("movement_settings");
        public static final StreamCodec<RegistryFriendlyByteBuf, Settings> CODEC = new StreamCodec<>() {
            @Override public Settings decode(RegistryFriendlyByteBuf b) { return new Settings(b.readUtf(16384)); }
            @Override public void encode(RegistryFriendlyByteBuf b, Settings p) { b.writeUtf(p.json, 16384); }
        };
        @Override public Type<Settings> type() { return TYPE; }
    }

    public record State(long epoch, long acknowledged, boolean active, Identifier dimension, MotionState motion)
            implements CustomPacketPayload {
        public static final Type<State> TYPE = id("movement_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = new StreamCodec<>() {
            @Override public State decode(RegistryFriendlyByteBuf b) {
                long epoch = b.readVarLong(); long ack = b.readVarLong(); boolean active = b.readBoolean();
                Identifier dimension = Identifier.parse(b.readUtf(256));
                return new State(epoch, ack, active, dimension, new MotionState(readVector(b), readVector(b),
                        b.readBoolean(), b.readBoolean(), b.readBoolean()));
            }
            @Override public void encode(RegistryFriendlyByteBuf b, State p) {
                b.writeVarLong(p.epoch); b.writeVarLong(p.acknowledged); b.writeBoolean(p.active);
                b.writeUtf(p.dimension.toString(), 256);
                writeVector(b, p.motion.position()); writeVector(b, p.motion.velocity());
                b.writeBoolean(p.motion.grounded()); b.writeBoolean(p.motion.jumpHeld()); b.writeBoolean(p.motion.crouched());
            }
        };
        @Override public Type<State> type() { return TYPE; }
    }

    private static Vector readVector(RegistryFriendlyByteBuf b) { return new Vector(b.readDouble(), b.readDouble(), b.readDouble()); }
    private static void writeVector(RegistryFriendlyByteBuf b, Vector v) { b.writeDouble(v.x()); b.writeDouble(v.y()); b.writeDouble(v.z()); }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Input.TYPE, Input.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Settings.TYPE, Settings.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
    }
}
