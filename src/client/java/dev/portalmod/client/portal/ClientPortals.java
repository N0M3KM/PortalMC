package dev.portalmod.client.portal;

import dev.portalmod.PortalItems;
import dev.portalmod.client.assets.LocalPortalAssets;
import dev.portalmod.config.ConfigManager;
import dev.portalmod.network.PortalPayloads;
import dev.portalmod.portal.PortalConfig;
import dev.portalmod.portal.PortalFrame;
import dev.portalmod.portal.PortalWorld;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

public final class ClientPortals {
    private static List<PortalFrame> portals = List.of();
    private static PortalConfig config = new PortalConfig();
    private static final dev.portalmod.portal.ShotCooldown cooldown = new dev.portalmod.portal.ShotCooldown();
    private ClientPortals() { }
    public static List<PortalFrame> frames() { return portals; }
    public static PortalConfig config() { return config; }
    public static void initialize() {
        PortalRenderer.initialize(); PortalEffects.initialize();
        ClientPlayNetworking.registerGlobalReceiver(PortalPayloads.TraversalFeedback.TYPE,(feedback,context) -> {
            dev.portalmod.client.visual.PhysicsHud.traversal(feedback.count());
            if(feedback.nativeMove()) dev.portalmod.client.visual.CameraEffects.exited(feedback.speed());
        });
        ClientPlayNetworking.registerGlobalReceiver(PortalPayloads.ShotFeedback.TYPE, (feedback, context) -> {
            dev.portalmod.client.visual.CharacterAnimation.fired(feedback.owner(),feedback.tick());
            dev.portalmod.client.visual.GunAnimation.remoteResult(feedback.owner(),feedback.orange(),feedback.accepted());
            if(context.player().getUUID().equals(feedback.owner())) {
                dev.portalmod.client.visual.GunAnimation.result(feedback.orange(),feedback.accepted());
                if(!feedback.accepted()) PortalEffects.fizzle(feedback.orange());
            }
        });
        PortalWorld.installClient(() -> portals, () -> config);
        ClientPlayNetworking.registerGlobalReceiver(PortalPayloads.Snapshot.TYPE, (snapshot, context) -> {
            PortalPayloads.Data data = ConfigManager.JSON.fromJson(snapshot.json(), PortalPayloads.Data.class);
            data.config().validate();
            if (data.portals().size() > 256) throw new IllegalArgumentException("Too many portals");
            PortalEffects.changed(portals,data.portals());
            config = data.config(); portals = List.copyOf(data.portals());
            if (context.client().level != null) ((PortalChunkCacheBridge)context.client().level.getChunkSource()).portalmod$retain();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { portals = List.of(); cooldown.reset(); PortalRenderer.close(); dev.portalmod.client.visual.GunAnimation.clear(); PortalEffects.clear(); dev.portalmod.client.visual.CameraEffects.clear(); LocalPortalAssets.clearPoseCache(); });
        ClientPreAttackCallback.EVENT.register((client, player, clicks) -> {
            if (player.getMainHandItem().is(PortalItems.PORTAL_GUN) || player.getOffhandItem().is(PortalItems.PORTAL_GUN)) {
                if (clicks > 0) fire(false);
                return true;
            }
            return false;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() && player.getItemInHand(hand).is(PortalItems.PORTAL_GUN)) { fire(true); return InteractionResult.SUCCESS; }
            return InteractionResult.PASS;
        });
    }
    public static void fire(boolean orange) {
        var player = Minecraft.getInstance().player;
        if (player == null || !ClientPlayNetworking.canSend(PortalPayloads.Shot.TYPE)
            || !cooldown.acquire(player,player.level(),player.level().getGameTime(),config.shotCooldownTicks)) return;
        dev.portalmod.client.visual.GunAnimation.fired(orange);
        ClientPlayNetworking.send(new PortalPayloads.Shot(orange, player.getYRot(), player.getXRot()));
    }
    public static boolean remoteAllowed(int chunkX, int chunkZ) {
        var level = Minecraft.getInstance().level;
        if (level == null) return false;
        for (PortalFrame p : portals) if (p.dimension().equals(level.dimension().identifier().toString()) && PortalWorld.partner(level, p) != null) {
            int px = Math.floorDiv((int) Math.floor(p.center().x()), 16), pz = Math.floorDiv((int) Math.floor(p.center().z()), 16);
            if (Math.abs(px - chunkX) <= config.chunkRadius && Math.abs(pz - chunkZ) <= config.chunkRadius) return true;
        }
        return false;
    }
}
