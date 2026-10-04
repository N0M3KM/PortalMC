package dev.portalmod.movement;

import dev.portalmod.mixin.EntityCollisionAccessor;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Reuses Minecraft's full-block/slab/stair collision query without triggering move side effects. */
public final class MinecraftCollisionWorld implements CollisionWorld {
    private final Player player;
    public MinecraftCollisionWorld(Player player) { this.player = player; }

    @Override public Vector resolve(Vector position, Vector displacement, boolean grounded, boolean crouched) {
        AABB originalBox = player.getBoundingBox();
        boolean originalGround = player.onGround();
        try {
            player.setBoundingBox(player.getDimensions(crouched ? Pose.CROUCHING : Pose.STANDING).makeBoundingBox(toMinecraft(position)));
            player.setOnGround(grounded);
            return fromMinecraft(((EntityCollisionAccessor) player).portalmod$collide(toMinecraft(displacement)));
        } finally {
            player.setBoundingBox(originalBox);
            player.setOnGround(originalGround);
        }
    }

    @Override public boolean canStand(Vector position) {
        return player.level().noCollision(player,
                player.getDimensions(Pose.STANDING).makeBoundingBox(toMinecraft(position)).deflate(1.0E-7));
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
