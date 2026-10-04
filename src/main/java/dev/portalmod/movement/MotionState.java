package dev.portalmod.movement;

public record MotionState(Vector position, Vector velocity, boolean grounded, boolean jumpHeld, boolean crouched) { }
