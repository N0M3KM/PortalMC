package dev.portalmod.portal;

import dev.portalmod.PortalItems;
import dev.portalmod.movement.MinecraftCollisionWorld;
import dev.portalmod.movement.Vector;
import dev.portalmod.network.PortalPayloads;
import dev.portalmod.config.ConfigManager;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PortalServer {
    public static final TagKey<Block> PORTALABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("portalmod", "portalable"));
    private static final List<PortalFrame> PORTALS = new ArrayList<>();
    private static final List<PortalFrame> READ_ONLY_PORTALS = Collections.unmodifiableList(PORTALS);
    private static final Map<UUID, Long> SHOTS = new HashMap<>();
    private static final Map<UUID,Long> CROSSINGS = new HashMap<>();
    private static final Map<Entity, Vec3> BEFORE = new IdentityHashMap<>();
    private record Ticket(ServerLevel level, ChunkPos pos) { }
    private static final Set<Ticket> TICKETS = new HashSet<>();
    private static TicketType ticketType;
    private static long nextId;
    private PortalServer() { }
    public static List<PortalFrame> frames() { return READ_ONLY_PORTALS; }
    public static void captureProjectile(Entity entity) { if(!entity.level().isClientSide()) BEFORE.putIfAbsent(entity,entity.getBoundingBox().getCenter()); }
    public static void transported(Entity entity) { BEFORE.remove(entity); }
    public static void recordCrossings(ServerPlayer player,int count,float speed,boolean nativeMove) {
        if(count<=0) return;
        long total=CROSSINGS.merge(player.getUUID(),(long)count,Long::sum);
        if(ServerPlayNetworking.canSend(player,PortalPayloads.TraversalFeedback.TYPE)) ServerPlayNetworking.send(player,new PortalPayloads.TraversalFeedback(total,speed,nativeMove));
    }
    public static void initialize() {
        PortalConfig.load();
        ticketType = Registry.register(BuiltInRegistries.TICKET_TYPE, Identifier.fromNamespaceAndPath("portalmod", "portal"), new TicketType(0, 14));
        PortalPayloads.register();
        ServerPlayNetworking.registerGlobalReceiver(PortalPayloads.Shot.TYPE, (shot, context) -> {
            ServerPlayer shooter = context.player();
            long before = SHOTS.getOrDefault(shooter.getUUID(), Long.MIN_VALUE);
            boolean accepted = fire(shooter, shot);
            // Broadcast only a consumed shot, never a cooldown/item/invalid-input rejection.
            if (SHOTS.getOrDefault(shooter.getUUID(), Long.MIN_VALUE) != before) {
                var feedback = new PortalPayloads.ShotFeedback(shooter.getUUID(), shooter.level().getGameTime(), shot.orange(), accepted);
                for (ServerPlayer viewer : shooter.level().getServer().getPlayerList().getPlayers())
                    if (ServerPlayNetworking.canSend(viewer, PortalPayloads.ShotFeedback.TYPE)) ServerPlayNetworking.send(viewer, feedback);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (PORTALS.removeIf(p -> p.owner().equals(handler.player.getUUID()))) changed(server);
            SHOTS.remove(handler.player.getUUID());
            CROSSINGS.remove(handler.player.getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { PORTALS.clear(); TICKETS.clear(); SHOTS.clear(); CROSSINGS.clear(); BEFORE.clear(); });
        ServerTickEvents.START_LEVEL_TICK.register(level -> {
            for (PortalFrame p : PORTALS) if (p.dimension().equals(level.dimension().identifier().toString()) && PortalWorld.partner(level,p)!=null) {
                Vec3 center = MinecraftCollisionWorld.toMinecraft(p.center());
                // Section-indexed nearby query, never a world-wide entity scan. Native moves handle mobs/items.
                for (Entity e : level.getEntities((Entity) null, new AABB(center, center).inflate(PortalConfig.get().entityCaptureRadius)))
                    if(e instanceof net.minecraft.world.entity.projectile.Projectile || e instanceof ServerPlayer player && !dev.portalmod.server.ServerMovement.active(player)) BEFORE.putIfAbsent(e, e.getBoundingBox().getCenter());
            }
        });
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            boolean removed = PORTALS.removeIf(p -> p.dimension().equals(level.dimension().identifier().toString()) && !validSupport(level, p));
            if (removed) changed(level.getServer());
            for (var entry : new ArrayList<>(BEFORE.entrySet())) if (entry.getKey().level() == level) {
                Entity e = entry.getKey();
                if (!e.isRemoved() && (!(e instanceof ServerPlayer player) || !dev.portalmod.server.ServerMovement.active(player)))
                    PortalTraversal.moveEntity(e, entry.getValue());
                BEFORE.remove(e);
            }
            if (level.getGameTime() % PortalConfig.get().chunkRefreshTicks == 0)
                for (ServerPlayer player : level.players()) sendChunks(player);
        });
    }
    public static boolean fire(ServerPlayer player, PortalPayloads.Shot shot) {
        PortalConfig config = PortalConfig.get();
        if (!config.enabled || !player.isAlive() || player.isSpectator()
                || !(player.getMainHandItem().is(PortalItems.PORTAL_GUN) || player.getOffhandItem().is(PortalItems.PORTAL_GUN))
                || !Float.isFinite(shot.yaw()) || !Float.isFinite(shot.pitch()) || Math.abs(shot.pitch()) > 90) return false;
        long now = player.level().getGameTime();
        if (now - SHOTS.getOrDefault(player.getUUID(), now - config.shotCooldownTicks) < config.shotCooldownTicks) return false;
        SHOTS.put(player.getUUID(), now);
        Vec3 from = player.getEyePosition();
        double yaw = Math.toRadians(shot.yaw()), pitch = Math.toRadians(shot.pitch());
        Vec3 ray = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        BlockHitResult hit = player.level().clip(new ClipContext(from, from.add(ray.scale(config.shotRange)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) { fizzle(player, from.add(ray.scale(config.fizzleMissDistance))); return false; }
        Direction normal = hit.getDirection();
        Direction up = normal.getAxis().isHorizontal() ? Direction.UP : Direction.fromYRot(shot.yaw());
        BlockPos first = normal.getAxis().isHorizontal() ? hit.getBlockPos().below() : hit.getBlockPos();
        BlockPos second = first.relative(up);
        Vector n = vector(normal), u = vector(up), r = u.cross(n);
        Vec3 center = Vec3.atCenterOf(first).add(MinecraftCollisionWorld.toMinecraft(n.scale(0.5).add(u.scale(0.5))));
        PortalFrame frame = new PortalFrame(++nextId, player.getUUID(), shot.orange(), player.level().dimension().identifier().toString(),
                MinecraftCollisionWorld.fromMinecraft(center), r, u, n,
                List.of(new PortalFrame.Cell(first.getX(), first.getY(), first.getZ()), new PortalFrame.Cell(second.getX(), second.getY(), second.getZ())));
        boolean sameColor = PORTALS.stream().anyMatch(p -> p.owner().equals(player.getUUID()) && p.orange() == shot.orange());
        if (!validSupport(player.level(), frame) || !clearFront(player.level(), first, second, normal)
                || (!sameColor && PORTALS.stream().map(PortalFrame::owner).distinct().count() >= config.maxPairs
                    && PORTALS.stream().noneMatch(p -> p.owner().equals(player.getUUID())))
                || PORTALS.stream().filter(p -> p.dimension().equals(frame.dimension()) && !(p.owner().equals(frame.owner()) && p.orange() == frame.orange()))
                    .anyMatch(p -> p.support().stream().anyMatch(frame.support()::contains))) { fizzle(player, hit.getLocation()); return false; }
        PORTALS.removeIf(p -> p.owner().equals(frame.owner()) && p.orange() == frame.orange());
        PORTALS.add(frame); changed(player.level().getServer()); return true;
    }
    private static Vector vector(Direction d) { return new Vector(d.getStepX(), d.getStepY(), d.getStepZ()); }
    private static boolean clearFront(ServerLevel level, BlockPos a, BlockPos b, Direction normal) {
        for (BlockPos support : List.of(a, b)) { BlockPos front = support.relative(normal); if (!level.getBlockState(front).getCollisionShape(level, front).isEmpty()) return false; }
        return true;
    }
    public static boolean validSupport(ServerLevel level, PortalFrame p) {
        for (PortalFrame.Cell c : p.support()) {
            BlockPos pos = new BlockPos(c.x(), c.y(), c.z()); var state = level.getBlockState(pos);
            if (!state.is(PORTALABLE) || !state.isCollisionShapeFullBlock(level, pos)) return false;
        }
        return true;
    }
    private static void fizzle(ServerPlayer player, Vec3 pos) {
        PortalConfig c = PortalConfig.get();
        player.level().sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, c.fizzleParticles, c.fizzleSpread, c.fizzleSpread, c.fizzleSpread, c.fizzleSpeed);
    }
    private static void changed(MinecraftServer server) {
        Set<Ticket> desired = new HashSet<>();
        for (PortalFrame p : PORTALS) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(p.dimension())));
            if (level != null) desired.add(new Ticket(level, portalChunk(p)));
        }
        for (Ticket old : TICKETS) if (!desired.contains(old)) old.level.getChunkSource().removeTicketWithRadius(ticketType, old.pos, PortalConfig.get().chunkRadius + 1);
        for (Ticket added : desired) if (!TICKETS.contains(added)) added.level.getChunkSource().addTicketWithRadius(ticketType, added.pos, PortalConfig.get().chunkRadius + 1);
        TICKETS.clear(); TICKETS.addAll(desired);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) { sync(p); sendChunks(p); }
    }
    private static void sync(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, PortalPayloads.Snapshot.TYPE))
            ServerPlayNetworking.send(player, new PortalPayloads.Snapshot(ConfigManager.JSON.toJson(new PortalPayloads.Data(PortalConfig.get(), frames()))));
    }
    private static void sendChunks(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, PortalPayloads.Snapshot.TYPE)) return;
        Set<ChunkPos> sent = new HashSet<>(); int radius = PortalConfig.get().chunkRadius;
        for (PortalFrame p : PORTALS) {
            if (!p.dimension().equals(player.level().dimension().identifier().toString())) continue;
            PortalFrame partner = PortalWorld.partner(player.level(), p);
            if (partner == null || p.center().subtract(MinecraftCollisionWorld.fromMinecraft(player.position())).lengthSquared() > PortalConfig.get().remoteViewDistance * PortalConfig.get().remoteViewDistance) continue;
            ChunkPos center = portalChunk(partner);
            for (int x = center.x() - radius; x <= center.x() + radius; x++) for (int z = center.z() - radius; z <= center.z() + radius; z++) {
                ChunkPos pos = new ChunkPos(x, z);
                if (sent.size() >= PortalConfig.get().maxRemoteChunks || !sent.add(pos)) continue;
                var chunk = player.level().getChunkSource().getChunkNow(x, z);
                if (chunk != null) player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, player.level().getLightEngine(), null, null));
            }
        }
    }
    private static ChunkPos portalChunk(PortalFrame p) {
        return new ChunkPos(Math.floorDiv((int)Math.floor(p.center().x()), 16), Math.floorDiv((int)Math.floor(p.center().z()), 16));
    }
    public static boolean visibleThroughPortal(ServerPlayer player, Entity entity) {
        if (!PortalConfig.get().enabled || player.level() != entity.level() || !entity.broadcastToPlayer(player)) return false;
        for (PortalFrame entry : PORTALS) {
            if (!entry.dimension().equals(player.level().dimension().identifier().toString())
                    || entry.center().subtract(MinecraftCollisionWorld.fromMinecraft(player.position())).lengthSquared() > PortalConfig.get().remoteViewDistance * PortalConfig.get().remoteViewDistance) continue;
            PortalFrame exit = PortalWorld.partner(player.level(), entry);
            if (exit != null && entity.position().distanceToSqr(MinecraftCollisionWorld.toMinecraft(exit.center())) <= Math.pow(PortalConfig.get().chunkRadius * 16, 2)) return true;
        }
        return false;
    }
}
