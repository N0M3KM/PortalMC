package dev.portalmod.config;

/** Values use Source units and seconds until the solver converts them to blocks. */
public final class MovementConfig {
    public boolean enabled = true;
    public double sourceUnitsPerBlock = 40.0;
    public double gravity = 600.0;
    public double groundAcceleration = 10.0;
    public double airAcceleration = 10.0;
    public double friction = 4.0;
    public double stopSpeed = 100.0;
    public double maxSpeed = 320.0;
    public double normalSpeed = 175.0;
    public double airWishSpeedCap = 30.0;
    public double maxVelocity = 3500.0;
    public double jumpHeight = 45.0;
    public double stepHeight = 18.0;
    public double crouchSpeedMultiplier = 1.0 / 3.0;
    public double surfaceFriction = 1.0;
    public double useItemSpeedMultiplier = 0.2;
    public int substeps = 3;
    public boolean autoBunnyHop = true;
    public boolean preventFallDamageWithGun = true;
    public boolean preventFallDamageWithBoots = true;
    public int maxQueuedInputs = 128;
    public int maxCatchUpSteps = 2;
    public int inputGraceTicks = 6;
    public double reconciliationTolerance = 0.002;
    public double velocityTolerance = 0.01;
    public double groundProbeDistance = 0.001;

    public void validate() {
        positive("sourceUnitsPerBlock", sourceUnitsPerBlock, 1, 1000);
        positive("gravity", gravity, 1, 10000);
        positive("groundAcceleration", groundAcceleration, 0, 1000);
        positive("airAcceleration", airAcceleration, 0, 1000);
        positive("friction", friction, 0, 100);
        positive("stopSpeed", stopSpeed, 0, 10000);
        positive("maxSpeed", maxSpeed, 1, 10000);
        positive("normalSpeed", normalSpeed, 1, 10000);
        positive("airWishSpeedCap", airWishSpeedCap, 0, 10000);
        positive("maxVelocity", maxVelocity, 1, 10000);
        positive("jumpHeight", jumpHeight, 0, 1000);
        positive("stepHeight", stepHeight, 0, sourceUnitsPerBlock);
        positive("crouchSpeedMultiplier", crouchSpeedMultiplier, 0, 1);
        positive("surfaceFriction", surfaceFriction, 0, 10);
        positive("useItemSpeedMultiplier", useItemSpeedMultiplier, 0, 1);
        positive("reconciliationTolerance", reconciliationTolerance, 0, 0.1);
        positive("velocityTolerance", velocityTolerance, 0, 0.1);
        positive("groundProbeDistance", groundProbeDistance, 0.00001, 0.01);
        positive("substeps", substeps, 1, 12);
        positive("maxQueuedInputs", maxQueuedInputs, 16, 1024);
        positive("maxCatchUpSteps", maxCatchUpSteps, 1, 4);
        positive("inputGraceTicks", inputGraceTicks, 2, 100);
    }

    private static void positive(String name, double value, double min, double max) {
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(name + " must be finite and between " + min + " and " + max);
        }
    }
}
