package dev.portalmod.movement;

import dev.portalmod.config.ConfigManager;
import dev.portalmod.config.MovementConfig;
import dev.portalmod.server.ServerMovement;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Client callbacks keep all common mixins independent of client-only classes. */
public final class MovementHooks {
    public interface ClientBridge {
        boolean active(Player player);
        boolean travel(Player player);
        MovementConfig config();
    }
    private static ClientBridge client;
    private MovementHooks() { }
    public static void installClient(ClientBridge bridge) { client = bridge; }

    public static boolean eligible(Player p) {
        return p.isAlive() && !p.isSpectator() && !p.getAbilities().flying && !p.isPassenger()
                && !p.isSleeping() && !p.isFallFlying() && !p.isInWater() && !p.isInLava()
                && !p.onClimbable() && !p.isInPowderSnow && !p.isAutoSpinAttack()
                && !p.hasEffect(MobEffects.LEVITATION) && !p.hasEffect(MobEffects.SLOW_FALLING);
    }

    public static boolean active(Player p) {
        return p.level().isClientSide() ? client != null && client.active(p) : ServerMovement.active(p);
    }

    public static boolean travel(Player p) {
        if (p.level().isClientSide()) return client != null && client.travel(p);
        return active(p); // Server simulation runs after vanilla connection tick restores its position.
    }

    public static MovementConfig config(Player p) {
        return p.level().isClientSide() && client != null ? client.config() : ConfigManager.server();
    }
}
