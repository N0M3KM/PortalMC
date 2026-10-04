package dev.portalmod.movement;

import dev.portalmod.mixin.EntityCollisionAccessor;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Reuses Minecraft's full-block/slab/stair collision query without triggering move side effects. */
public final class MinecraftCollisionWorld implements CollisionWorld {
    private final Player player;
    private final boolean applyView;
    private int crossings;
    private final java.util.List<dev.portalmod.portal.PortalTraversal.Crossing> substepCrossings = new java.util.ArrayList<>();
    public MinecraftCollisionWorld(Player player) { this(player, true); }
    public MinecraftCollisionWorld(Player player, boolean applyView) { this.player = player; this.applyView = applyView; }
    public boolean crossed() { return crossings > 0; }
    public int crossingCount() { return crossings; }
    @Override public MotionState traverse(Vector previous, MotionState state) {
        substepCrossings.clear();
        while (crossings < dev.portalmod.portal.PortalWorld.config(player.level()).maxCrossingsPerTick) {
            var crossing = dev.portalmod.portal.PortalTraversal.cross(player, previous, state);
            if (crossing == null) break;
            crossings++; substepCrossings.add(crossing);
            Vector wanted = crossing.state().position().subtract(crossing.exitStart());
            Vector actual = resolve(crossing.exitStart(), wanted, false, state.crouched());
            Vector v = crossing.state().velocity();
            v = new Vector(Math.abs(wanted.x()-actual.x())>1e-7 ? 0 : v.x(), Math.abs(wanted.y()-actual.y())>1e-7 ? 0 : v.y(), Math.abs(wanted.z()-actual.z())>1e-7 ? 0 : v.z());
            state = new MotionState(crossing.exitStart().add(actual), v, wanted.y()<0 && Math.abs(wanted.y()-actual.y())>1e-7, state.jumpHeld(), state.crouched());
            previous = crossing.exitStart();
            if (applyView) dev.portalmod.portal.PortalTraversal.rotateView(player, crossing);
        }
        if (applyView && !substepCrossings.isEmpty()) {
            player.xo = state.position().x(); player.yo = state.position().y(); player.zo = state.position().z();
            player.yRotO = player.getYRot(); player.xRotO = player.getXRot();
        }
        return state;
    }
    @Override public Vector rotateWish(Vector wish) {
        for (var crossing : substepCrossings) wish = crossing.entry().rotateTo(crossing.exit(), wish);
        return wish;
    }

    @Override public Vector resolve(Vector position, Vector displacement, boolean grounded, boolean crouched) {
        AABB originalBox = player.getBoundingBox();
        boolean originalGround = player.onGround();
        try {
            player.setBoundingBox(player.getDimensions(crouched ? Pose.CROUCHING : Pose.STANDING).makeBoundingBox(toMinecraft(position)));
            player.setOnGround(grounded);
            if (crossings < dev.portalmod.portal.PortalWorld.config(player.level()).maxCrossingsPerTick) {
                var predicted = dev.portalmod.portal.PortalTraversal.cross(player, position, new MotionState(position.add(displacement), Vector.ZERO, grounded, false, crouched));
                if (predicted != null) {
                    Vector offset = new Vector(0, player.getBoundingBox().getYsize()/2, 0);
                    double t = predicted.entry().crossing(position.add(offset), position.add(displacement).add(offset));
                    Vector prefix = displacement.scale(t);
                    Vector actual = fromMinecraft(((EntityCollisionAccessor)player).portalmod$collide(toMinecraft(prefix)));
                    if (actual.subtract(prefix).lengthSquared() < 1e-12) return displacement;
                }
            }
            return fromMinecraft(((EntityCollisionAccessor) player).portalmod$collide(toMinecraft(displacement)));
        } finally {
            player.setBoundingBox(originalBox);
            player.setOnGround(originalGround);
        }
    }

    @Override public boolean canStand(Vector position) {
        AABB old = player.getBoundingBox();
        try {
            AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(toMinecraft(position)).deflate(1.0E-7);
            player.setBoundingBox(box);
            return player.level().noCollision(player, box);
        } finally { player.setBoundingBox(old); }
    }

    public static Vec3 toMinecraft(Vector v) { return new Vec3(v.x(), v.y(), v.z()); }
    public static Vector fromMinecraft(Vec3 v) { return new Vector(v.x, v.y, v.z); }

    public static MotionState capture(Player p, boolean jumpHeld) {
        return new MotionState(fromMinecraft(p.position()), fromMinecraft(p.getDeltaMovement()).scale(20),
                p.onGround(), jumpHeld, p.getPose() == Pose.CROUCHING);
    }

    public static void apply(Player p, MotionState state) {
        p.setShiftKeyDown(state.crouched());
        p.setPose(state.crouched() ? Pose.CROUCHING : Pose.STANDING);
        p.setPos(toMinecraft(state.position()));
        p.setDeltaMovement(toMinecraft(state.velocity().scale(1.0 / 20)));
        p.setOnGround(state.grounded());
        p.setSprinting(false);
    }
}
