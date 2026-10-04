package dev.portalmod.movement;

/** Queries must not move entities or produce gameplay effects during prediction replay. */
public interface CollisionWorld {
    Vector resolve(Vector position, Vector displacement, boolean grounded, boolean crouched);
    boolean canStand(Vector position);
    default MotionState traverse(Vector previousPosition, MotionState state) { return state; }
    default Vector rotateWish(Vector wish) { return wish; }
}
