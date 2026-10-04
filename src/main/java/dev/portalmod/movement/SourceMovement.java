package dev.portalmod.movement;

import dev.portalmod.config.MovementConfig;

/** Independently implemented Source-style equations, not copied Source engine code. */
public final class SourceMovement {
    public static final double TICK_SECONDS = 1.0 / 20.0;

    private SourceMovement() { }

    public static MotionState tick(MotionState state, MovementInput input, MovementConfig config,
                                   CollisionWorld world, boolean usingItem) {
        if (!input.valid()) throw new IllegalArgumentException("Invalid movement input");
        boolean crouched = input.crouch() || !world.canStand(state.position());
        double units = config.sourceUnitsPerBlock;
        double wishSpeed = Math.min(config.normalSpeed, config.maxSpeed) / units;
        if (crouched) wishSpeed *= config.crouchSpeedMultiplier;
        if (usingItem) wishSpeed *= config.useItemSpeedMultiplier;
        double magnitude = Math.min(1, Math.hypot(input.forward(), input.sideways()));
        double yaw = Math.toRadians(input.yaw());
        Vector wish = new Vector(input.sideways() * Math.cos(yaw) - input.forward() * Math.sin(yaw),
                0, input.forward() * Math.cos(yaw) + input.sideways() * Math.sin(yaw));
        if (wish.horizontalLength() > 0) wish = wish.scale(1 / wish.horizontalLength());
        wishSpeed *= magnitude;
        Vector velocity = state.velocity();
        Vector position = state.position();
        boolean grounded = state.grounded();
        boolean canJump = input.jump() && (config.autoBunnyHop || !state.jumpHeld());
        double dt = TICK_SECONDS / config.substeps;
        double gravity = config.gravity / units;
        for (int step = 0; step < config.substeps; step++) {
            if (grounded && canJump) {
                velocity = new Vector(velocity.x(), Math.sqrt(2 * gravity * config.jumpHeight / units), velocity.z());
                grounded = false;
                if (!config.autoBunnyHop) canJump = false;
            }
            if (grounded) {
                velocity = friction(velocity, config.friction * config.surfaceFriction,
                        config.stopSpeed / units, dt);
                velocity = accelerate(velocity, wish, wishSpeed, wishSpeed,
                        config.groundAcceleration * config.surfaceFriction, dt);
            } else {
                velocity = accelerate(velocity, wish, wishSpeed, Math.min(wishSpeed, config.airWishSpeedCap / units),
                        config.airAcceleration * config.surfaceFriction, dt);
            }
            velocity = new Vector(velocity.x(), grounded ? 0 : velocity.y() - gravity * dt / 2, velocity.z());
            velocity = clamp(velocity, config.maxVelocity / units);
            Vector wanted = velocity.scale(dt);
            // A small downward query preserves grounded state even while standing still.
            if (grounded) wanted = new Vector(wanted.x(), -config.groundProbeDistance, wanted.z());
            Vector actual = world.resolve(position, wanted, grounded, crouched);
            Vector before = position;
            position = position.add(actual);
            boolean landed = wanted.y() < 0 && different(wanted.y(), actual.y());
            velocity = new Vector(different(wanted.x(), actual.x()) ? 0 : velocity.x(),
                    different(wanted.y(), actual.y()) ? 0 : velocity.y(),
                    different(wanted.z(), actual.z()) ? 0 : velocity.z());
            grounded = landed;
            if (!grounded) velocity = velocity.add(new Vector(0, -gravity * dt / 2, 0));
            MotionState traversed = world.traverse(before, new MotionState(position, velocity, grounded, input.jump(), crouched));
            position = traversed.position(); velocity = traversed.velocity(); grounded = traversed.grounded();
            wish = world.rotateWish(wish);
        }
        return new MotionState(position, clamp(velocity, config.maxVelocity / units), grounded, input.jump(), crouched);
    }

    public static Vector friction(Vector velocity, double friction, double stopSpeed, double dt) {
        double speed = velocity.horizontalLength();
        if (speed == 0) return velocity;
        double remaining = Math.max(0, speed - Math.max(speed, stopSpeed) * friction * dt);
        return new Vector(velocity.x() * remaining / speed, velocity.y(), velocity.z() * remaining / speed);
    }

    public static Vector accelerate(Vector velocity, Vector direction, double wishSpeed,
                                    double projectionCap, double acceleration, double dt) {
        double available = projectionCap - velocity.dot(direction);
        if (available <= 0) return velocity;
        return velocity.add(direction.scale(Math.min(available, acceleration * wishSpeed * dt)));
    }

    private static Vector clamp(Vector v, double max) {
        return new Vector(Math.clamp(v.x(), -max, max), Math.clamp(v.y(), -max, max), Math.clamp(v.z(), -max, max));
    }

    private static boolean different(double a, double b) { return Math.abs(a - b) > 1.0E-7; }
}
