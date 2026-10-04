package dev.portalmod.portal;

import dev.portalmod.movement.MinecraftCollisionWorld;
import dev.portalmod.movement.MotionState;
import dev.portalmod.movement.Vector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

public final class PortalTraversal {
    public record Crossing(MotionState state, PortalFrame entry, PortalFrame exit, Vector exitStart) { }
    private PortalTraversal() { }
    public static Crossing cross(Entity entity, Vector from, MotionState after) {
        if (!PortalWorld.config(entity.level()).enabled || entity.isPassenger() || entity.isVehicle()) return null;
        var box = entity instanceof net.minecraft.world.entity.player.Player player && dev.portalmod.movement.MovementHooks.active(player)
            ? player.getDimensions(after.crouched() ? net.minecraft.world.entity.Pose.CROUCHING : net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(entity.position())
            : entity.getBoundingBox();
        Vector offset = new Vector(0, box.getYsize() / 2, 0);
        Vector start = from.add(offset), end = after.position().add(offset);
        PortalFrame nearest = null, destination = null; double closest = Double.POSITIVE_INFINITY;
        for (PortalFrame p : PortalWorld.frames(entity.level())) {
            if (!p.dimension().equals(entity.level().dimension().identifier().toString())) continue;
            if(!p.intersectsSweep(start,end,box.getXsize()*.5,box.getYsize()*.5,box.getZsize()*.5,PortalWorld.config(entity.level()).collisionMargin)) continue;
            PortalFrame exit = PortalWorld.partner(entity.level(), p); if (exit == null) continue;
            double t = p.crossing(start, end);
            if (t < 0 || t >= closest || !p.contains(start.add(end.subtract(start).scale(t)), PortalWorld.extent(box, p.right()), PortalWorld.extent(box, p.up()))) continue;
            nearest = p; destination = exit; closest = t;
        }
        if (nearest == null) return null;
        Vector transformed = nearest.pointTo(destination, end).add(destination.normal().scale(PortalWorld.config(entity.level()).exitEpsilon)).subtract(offset);
        Vector atPlane = start.add(end.subtract(start).scale(closest));
        Vector exitStart = nearest.pointTo(destination, atPlane).add(destination.normal().scale(PortalWorld.config(entity.level()).exitEpsilon)).subtract(offset);
        return new Crossing(new MotionState(transformed, nearest.rotateTo(destination, after.velocity()), false, after.jumpHeld(), after.crouched()), nearest, destination, exitStart);
    }
    public static void rotateView(Entity entity, Crossing crossing) {
        double yaw = Math.toRadians(entity.getYRot()), pitch = Math.toRadians(entity.getXRot());
        Vector look = crossing.entry.rotateTo(crossing.exit, new Vector(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)));
        entity.setYRot((float) Math.toDegrees(Math.atan2(-look.x(), look.z())));
        entity.setXRot((float) -Math.toDegrees(Math.asin(Math.clamp(look.y(), -1, 1))));
        entity.setYHeadRot(entity.getYRot());
    }
    public static void moveEntity(Entity entity, Vec3 previousCenter) {
        Vector previousFeet = MinecraftCollisionWorld.fromMinecraft(previousCenter.subtract(0, entity.getBoundingBox().getYsize() / 2, 0));
        MotionState state = new MotionState(MinecraftCollisionWorld.fromMinecraft(entity.position()), MinecraftCollisionWorld.fromMinecraft(entity.getDeltaMovement()).scale(20), entity.onGround(), false, false);
        Crossing crossing = cross(entity, previousFeet, state);
        if (crossing == null) return;
        if(entity instanceof ServerPlayer player) PortalServer.recordCrossings(player,1,(float)Math.sqrt(crossing.state().velocity().lengthSquared()),true);
        rotateView(entity, crossing);
        Vec3 position = MinecraftCollisionWorld.toMinecraft(crossing.state.position()), velocity = MinecraftCollisionWorld.toMinecraft(crossing.state.velocity().scale(0.05));
        if (entity instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            Vec3 start = MinecraftCollisionWorld.toMinecraft(crossing.exitStart());
            entity.absSnapTo(start.x, start.y, start.z, entity.getYRot(), entity.getXRot());
            entity.setDeltaMovement(velocity);
            net.minecraft.world.entity.projectile.ProjectileUtil.rotateTowardsMovement(entity, 1);
            var blockHit = entity.level().clipIncludingBorder(new net.minecraft.world.level.ClipContext(start, position, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, entity));
            if (projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow)
                ((dev.portalmod.mixin.PortalArrowAccessor)projectile).portalmod$step(blockHit);
            else {
                Vec3 end = blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? position : blockHit.getLocation();
                var entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(entity.level(), projectile, start, end,
                    entity.getBoundingBox().expandTowards(end.subtract(start)).inflate(1), ((dev.portalmod.mixin.PortalProjectileAccessor)entity)::portalmod$canHit);
                net.minecraft.world.phys.HitResult hit = entityHit == null ? blockHit : entityHit;
                Vec3 at = hit.getLocation(); entity.absSnapTo(at.x, at.y, at.z);
                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) ((dev.portalmod.mixin.PortalProjectileAccessor)entity).portalmod$hit(hit);
            }
            entity.needsSync = true;
        } else {
            if (entity instanceof ServerPlayer player) player.connection.teleport(new PositionMoveRotation(position, velocity, player.getYRot(), player.getXRot()), Set.of());
            else entity.absSnapTo(position.x, position.y, position.z, entity.getYRot(), entity.getXRot());
            entity.setDeltaMovement(velocity);
        }
        entity.setOnGround(false); entity.resetFallDistance();
    }
}
