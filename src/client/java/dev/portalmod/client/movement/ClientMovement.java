package dev.portalmod.client.movement;

import dev.portalmod.config.ConfigManager;
import dev.portalmod.config.MovementConfig;
import dev.portalmod.movement.MinecraftCollisionWorld;
import dev.portalmod.movement.MotionState;
import dev.portalmod.movement.MovementHooks;
import dev.portalmod.movement.MovementInput;
import dev.portalmod.movement.SourceMovement;
import dev.portalmod.network.MovementPayloads;
import java.util.ArrayDeque;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public final class ClientMovement implements MovementHooks.ClientBridge {
    private record Predicted(long sequence, MovementInput input, boolean usingItem, MotionState after) { }
    private final ArrayDeque<Predicted> pending = new ArrayDeque<>();
    private MovementConfig config = new MovementConfig();
    private long epoch;
    private long sequence;
    private long acknowledged;
    private boolean serverActive;
    private boolean jumpHeld;
    private int playerId = -1;
    private int lastTick = -1;
    private MotionState lastAuthority;

    public void initialize() {
        MovementHooks.installClient(this);
        ClientPlayNetworking.registerGlobalReceiver(MovementPayloads.Settings.TYPE, (payload, context) -> {
            MovementConfig received = ConfigManager.JSON.fromJson(payload.json(), MovementConfig.class);
            if (received == null) throw new IllegalArgumentException("Missing movement settings");
            received.validate();
            config = received;
        });
        ClientPlayNetworking.registerGlobalReceiver(MovementPayloads.State.TYPE, (payload, context) -> reconcile(context.player(), payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
    }

    private void reset() {
        pending.clear(); epoch = 0; sequence = 0; acknowledged = 0;
        serverActive = false; jumpHeld = false; playerId = -1; lastTick = -1;
        lastAuthority = null;
    }

    @Override public boolean active(Player p) {
        return p.isLocalPlayer() && epoch != 0 && serverActive && config.enabled && MovementHooks.eligible(p);
    }
    @Override public MovementConfig config() { return config; }

    @Override public boolean travel(Player player) {
        if (!(player instanceof LocalPlayer p) || !active(p)) return false;
        if (p.tickCount == lastTick) return true;
        lastTick = p.tickCount;
        var keys = p.input.keyPresses;
        MovementInput input = new MovementInput((keys.left() ? 1 : 0) - (keys.right() ? 1 : 0),
                (keys.forward() ? 1 : 0) - (keys.backward() ? 1 : 0),
                p.getYRot(), p.getXRot(), keys.jump(), keys.shift());
        if (pending.size() >= config.maxQueuedInputs) {
            // Await acknowledgement; never discard a command while pretending it was sent.
            return true;
        }
        MotionState next = SourceMovement.tick(MinecraftCollisionWorld.capture(p, jumpHeld), input,
                config, new MinecraftCollisionWorld(p), p.isUsingItem());
        MinecraftCollisionWorld.apply(p, next);
        jumpHeld = next.jumpHeld();
        pending.addLast(new Predicted(++sequence, input, p.isUsingItem(), next));
        ClientPlayNetworking.send(new MovementPayloads.Input(epoch, sequence, input));
        return true;
    }

    private void reconcile(LocalPlayer p, MovementPayloads.State payload) {
        if (!payload.dimension().equals(p.level().dimension().identifier())) return;
        boolean fresh = payload.epoch() != epoch || p.getId() != playerId;
        if (!fresh && payload.acknowledged() < acknowledged) return;
        serverActive = payload.active();
        if (fresh) {
            pending.clear(); epoch = payload.epoch(); sequence = payload.acknowledged();
            playerId = p.getId(); lastTick = -1;
        }
        acknowledged = payload.acknowledged();
        boolean authorityChanged = lastAuthority == null || !matches(lastAuthority, payload.motion());
        lastAuthority = payload.motion();
        if (!serverActive) { pending.clear(); jumpHeld = false; return; }
        Predicted matching = null;
        while (!pending.isEmpty() && pending.peekFirst().sequence() <= acknowledged) matching = pending.removeFirst();
        if (!fresh && matching != null && matching.sequence() == acknowledged && matches(matching.after(), payload.motion())) return;
        // A repeated acknowledgement with no changed authority should not rewind live prediction.
        if (!fresh && matching == null && !authorityChanged) return;
        MotionState replayed = payload.motion();
        ArrayDeque<Predicted> rebuilt = new ArrayDeque<>();
        for (Predicted command : pending) {
            replayed = SourceMovement.tick(replayed, command.input(), config, new MinecraftCollisionWorld(p), command.usingItem());
            rebuilt.addLast(new Predicted(command.sequence(), command.input(), command.usingItem(), replayed));
        }
        pending.clear(); pending.addAll(rebuilt);
        double dx = replayed.position().x() - p.getX();
        double dy = replayed.position().y() - p.getY();
        double dz = replayed.position().z() - p.getZ();
        MinecraftCollisionWorld.apply(p, replayed);
        p.xo += dx; p.yo += dy; p.zo += dz;
        jumpHeld = replayed.jumpHeld();
    }

    private boolean matches(MotionState a, MotionState b) {
        return a.position().subtract(b.position()).lengthSquared() <= config.reconciliationTolerance * config.reconciliationTolerance
                && a.velocity().subtract(b.velocity()).lengthSquared() <= config.velocityTolerance * config.velocityTolerance
                && a.grounded() == b.grounded() && a.crouched() == b.crouched() && a.jumpHeld() == b.jumpHeld();
    }
}
