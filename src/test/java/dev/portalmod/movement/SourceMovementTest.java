package dev.portalmod.movement;

import dev.portalmod.config.MovementConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceMovementTest {
    private final MovementConfig config = new MovementConfig();
    private static final CollisionWorld FLOOR = new CollisionWorld() {
        public Vector resolve(Vector p, Vector d, boolean grounded, boolean crouched) {
            return new Vector(d.x(), Math.max(d.y(), -p.y()), d.z());
        }
        public boolean canStand(Vector p) { return true; }
    };
    private static final CollisionWorld EMPTY = new CollisionWorld() {
        public Vector resolve(Vector p, Vector d, boolean grounded, boolean crouched) { return d; }
        public boolean canStand(Vector p) { return true; }
    };
    private MotionState standing() { return new MotionState(Vector.ZERO, Vector.ZERO, true, false, false); }
    private MovementInput input(float side, float forward, boolean jump, boolean crouch) {
        return new MovementInput(side, forward, 0, 0, jump, crouch);
    }

    @Test void normalSpeedComesFrom175NotGeneric320() {
        MotionState state = standing();
        for (int i = 0; i < 40; i++) state = SourceMovement.tick(state, input(0, 1, false, false), config, FLOOR, false);
        assertEquals(4.375, state.velocity().horizontalLength(), 1.0E-9);
        assertTrue(state.grounded());
    }

    @Test void diagonalInputCannotExceedWalkingSpeed() {
        MotionState state = standing();
        for (int i = 0; i < 40; i++) state = SourceMovement.tick(state, input(1, 1, false, false), config, FLOOR, false);
        assertEquals(4.375, state.velocity().horizontalLength(), 1.0E-9);
    }

    @Test void frictionStopsGroundMotionButNotAirMomentum() {
        Vector v = new Vector(10, 2, 0);
        assertEquals(8, SourceMovement.friction(v, 4, 2.5, 0.05).x(), 1.0E-9);
        MotionState air = new MotionState(new Vector(0, 100, 0), v, false, false, false);
        MotionState result = SourceMovement.tick(air, input(0, 0, false, false), config, EMPTY, false);
        assertEquals(10, result.velocity().x(), 1.0E-9);
        assertEquals(1.25, result.velocity().y(), 1.0E-9);
    }

    @Test void airAccelerationCapsOnlyWishDirectionProjection() {
        Vector result = SourceMovement.accelerate(new Vector(10, 0, 0), new Vector(0, 0, 1), 4.375, 0.75, 10, 0.05);
        assertEquals(10, result.x());
        assertEquals(0.75, result.z());
        assertTrue(result.horizontalLength() > 10);
        assertEquals(result, SourceMovement.accelerate(result, new Vector(0, 0, 1), 4.375, 0.75, 10, 0.05));
    }

    @Test void jumpArcMatchesConvertedHeightAndGravity() {
        MotionState state = SourceMovement.tick(standing(), input(0, 0, true, false), config, FLOOR, false);
        double highest = state.position().y();
        for (int i = 0; i < 30; i++) {
            state = SourceMovement.tick(state, input(0, 0, false, false), config, FLOOR, false);
            highest = Math.max(highest, state.position().y());
        }
        assertEquals(1.125, highest, 0.005);
        assertTrue(state.grounded());
        assertEquals(0, state.position().y(), 1.0E-9);
    }

    @Test void autoBhopPreservesMomentumOnTakeoff() {
        MotionState state = new MotionState(Vector.ZERO, new Vector(10, 0, 0), true, true, false);
        MotionState result = SourceMovement.tick(state, input(0, 0, true, false), config, FLOOR, false);
        assertEquals(10, result.velocity().x(), 1.0E-9);
        assertTrue(result.position().y() > 0);
    }

    @Test void manualBhopRequiresJumpRelease() {
        config.autoBunnyHop = false;
        MotionState state = new MotionState(Vector.ZERO, Vector.ZERO, true, true, false);
        assertTrue(SourceMovement.tick(state, input(0, 0, true, false), config, FLOOR, false).grounded());
        state = SourceMovement.tick(state, input(0, 0, false, false), config, FLOOR, false);
        assertFalse(SourceMovement.tick(state, input(0, 0, true, false), config, FLOOR, false).grounded());
    }

    @Test void crouchingLimitsSpeedAndCannotStandInsideCeiling() {
        CollisionWorld ceiling = new CollisionWorld() {
            public Vector resolve(Vector p, Vector d, boolean g, boolean c) { return FLOOR.resolve(p, d, g, c); }
            public boolean canStand(Vector p) { return false; }
        };
        MotionState state = standing();
        for (int i = 0; i < 40; i++) state = SourceMovement.tick(state, input(0, 1, false, false), config, ceiling, false);
        assertTrue(state.crouched());
        assertEquals(4.375 / 3, state.velocity().horizontalLength(), 1.0E-9);
    }

    @Test void collisionsStopNormalComponentAndKeepWallSliding() {
        CollisionWorld wall = new CollisionWorld() {
            public Vector resolve(Vector p, Vector d, boolean g, boolean c) { return new Vector(0, Math.max(d.y(), -p.y()), d.z()); }
            public boolean canStand(Vector p) { return true; }
        };
        MotionState state = SourceMovement.tick(standing(), input(1, 1, false, false), config, wall, false);
        assertEquals(0, state.velocity().x());
        assertEquals(0, state.position().x());
        assertTrue(state.position().z() > 0);
    }

    @Test void invalidConfigAndInputsAreRejected() {
        config.gravity = Double.NaN;
        assertThrows(IllegalArgumentException.class, config::validate);
        assertFalse(new MovementInput(Float.NaN, 0, 0, 0, false, false).valid());
        assertFalse(new MovementInput(0, 2, 0, 0, false, false).valid());
        assertFalse(new MovementInput(0, 0, 0, 91, false, false).valid());
    }

    @Test void deterministicReplayMatchesPredictionAfterDelayedAcknowledgement() {
        MotionState predicted = standing();
        MotionState authority = standing();
        MovementInput[] commands = new MovementInput[120];
        for (int i = 0; i < commands.length; i++) commands[i] = new MovementInput(i % 3 == 0 ? 1 : 0, 1, i, 0, i > 5, false);
        for (int i = 0; i < commands.length; i++) {
            predicted = SourceMovement.tick(predicted, commands[i], config, FLOOR, false);
            if (i >= 4) authority = SourceMovement.tick(authority, commands[i - 4], config, FLOOR, false);
            MotionState replay = authority;
            for (int j = Math.max(0, i - 3); j <= i; j++) replay = SourceMovement.tick(replay, commands[j], config, FLOOR, false);
            assertEquals(predicted, replay);
        }
    }
}
