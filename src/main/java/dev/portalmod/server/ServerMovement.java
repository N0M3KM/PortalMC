package dev.portalmod.server;

import dev.portalmod.config.ConfigManager;
import dev.portalmod.movement.InputQueue;
import dev.portalmod.movement.MinecraftCollisionWorld;
import dev.portalmod.movement.MotionState;
import dev.portalmod.movement.MovementHooks;
import dev.portalmod.movement.MovementInput;
import dev.portalmod.movement.SourceMovement;
import dev.portalmod.network.MovementPayloads;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Accessed only on the logical server thread. Clients never submit position or velocity. */
public final class ServerMovement {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static long nextEpoch;

    private static final class Session {
        final ServerPlayer player;
        long epoch = ++nextEpoch;
        long portalCrossings;
        InputQueue queue = new InputQueue();
        boolean active;
        boolean jumpHeld;
        boolean timedOut;
        Session(ServerPlayer player) { this.player = player; }
        void reset() { epoch = ++nextEpoch; queue = new InputQueue(); jumpHeld = false; timedOut = false; portalCrossings = 0; }
    }

    private ServerMovement() { }

    public static void initialize() {
        ServerPlayNetworking.registerGlobalReceiver(MovementPayloads.Input.TYPE, (payload, context) -> {
            Session s = SESSIONS.get(context.player().getUUID());
            if (s == null || s.player != context.player() || !s.active || payload.epoch() != s.epoch) return;
            try {
                s.queue.offer(payload.sequence(), payload.input(), ConfigManager.server().maxQueuedInputs);
            } catch (IllegalArgumentException e) {
                context.player().connection.disconnect(Component.literal("PortalMC: invalid movement input stream"));
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.player;
            if (!ServerPlayNetworking.canSend(p, MovementPayloads.State.TYPE)
                    || !ServerPlayNetworking.canSend(p, MovementPayloads.Settings.TYPE)) {
                if (ConfigManager.server().enabled) handler.disconnect(Component.literal("This server requires PortalMC on the client."));
                return;
            }
            SESSIONS.put(p.getUUID(), new Session(p));
            sendSettings(p);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SESSIONS.remove(handler.player.getUUID()));
    }

    public static boolean active(Player p) {
        Session s = SESSIONS.get(p.getUUID());
        return s != null && s.player == p && s.active && ConfigManager.server().enabled && MovementHooks.eligible(p);
    }

    public static void invalidate(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        if (s != null) { s.active = false; s.reset(); }
    }

    public static void sendSettings(ServerPlayer p) {
        if (ServerPlayNetworking.canSend(p, MovementPayloads.Settings.TYPE)) {
            ServerPlayNetworking.send(p, new MovementPayloads.Settings(ConfigManager.JSON.toJson(ConfigManager.server())));
        }
    }

    public static void reloadSessions() {
        for (Session s : SESSIONS.values()) { s.active = false; s.reset(); sendSettings(s.player); }
    }

    /** Called after the connection has finished its vanilla position-restoration tick. */
    public static void tick(ServerPlayer p, boolean awaitingTeleport) {
        if (!ServerPlayNetworking.canSend(p, MovementPayloads.State.TYPE)) return;
        Session s = SESSIONS.get(p.getUUID());
        if (s == null || s.player != p) { // Respawn creates a new ServerPlayer with the same UUID.
            s = new Session(p);
            SESSIONS.put(p.getUUID(), s);
            sendSettings(p);
        }
        boolean shouldRun = ConfigManager.server().enabled && MovementHooks.eligible(p)
                && p.connection.hasClientLoaded() && !awaitingTeleport && !p.isChangingDimension();
        if (s.active != shouldRun) { s.reset(); s.active = shouldRun; }
        if (s.active) {
            var config = ConfigManager.server();
            Vec3 tickStart = p.position();
            s.queue.beginTick(config.maxCatchUpSteps);
            InputQueue.Command command;
            while ((command = s.queue.poll()) != null) {
                s.timedOut = false;
                if (simulate(p, s, command.input())) tickStart = p.position();
                if (!MovementHooks.eligible(p)) break;
            }
            if (s.timedOut || s.queue.idleTicks() >= config.inputGraceTicks) {
                // Withholding input must not allow a client to hover indefinitely.
                if (!s.timedOut) { s.reset(); s.timedOut = true; }
                if (simulate(p, s, new MovementInput(0, 0, p.getYRot(), p.getXRot(), false, p.isShiftKeyDown()))) tickStart = p.position();
            }
            p.applyEffectsFromBlocks(tickStart, p.position());
            p.level().getChunkSource().move(p);
        }
        ServerPlayNetworking.send(p, new MovementPayloads.State(s.epoch, s.queue.acknowledged(), s.active,
                p.level().dimension().identifier(), MinecraftCollisionWorld.capture(p, s.jumpHeld), s.portalCrossings, p.getYRot(), p.getXRot()));
    }

    private static boolean simulate(ServerPlayer p, Session s, MovementInput input) {
        p.setYRot(input.yaw());
        p.setXRot(input.pitch());
        MotionState before = MinecraftCollisionWorld.capture(p, s.jumpHeld);
        MinecraftCollisionWorld collision = new MinecraftCollisionWorld(p);
        MotionState after = SourceMovement.tick(before, input, ConfigManager.server(), collision, p.isUsingItem());
        s.portalCrossings += collision.crossingCount();
        Vec3 from = p.position();
        MinecraftCollisionWorld.apply(p, after);
        Vec3 delta = p.position().subtract(from);
        if (collision.crossed()) { p.resetFallDistance(); delta = MinecraftCollisionWorld.toMinecraft(after.velocity().scale(0.05)); }
        p.setKnownMovement(delta);
        p.doCheckFallDamage(delta.x, delta.y, delta.z, after.grounded());
        p.checkMovementStatistics(delta.x, delta.y, delta.z);
        if (input.forward() != 0 || input.sideways() != 0 || input.jump()) p.resetLastActionTime();
        s.jumpHeld = after.jumpHeld();
        return collision.crossed();
    }
}
