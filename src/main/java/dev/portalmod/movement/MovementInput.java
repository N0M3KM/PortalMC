package dev.portalmod.movement;

public record MovementInput(float sideways, float forward, float yaw, float pitch, boolean jump, boolean crouch) {
    public static final MovementInput IDLE = new MovementInput(0, 0, 0, 0, false, false);
    public boolean valid() {
        return Float.isFinite(sideways) && Float.isFinite(forward) && Float.isFinite(yaw)
                && Float.isFinite(pitch) && Math.abs(sideways) <= 1 && Math.abs(forward) <= 1
                && Math.abs(pitch) <= 90;
    }
}
