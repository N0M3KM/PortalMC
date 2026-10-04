package dev.portalmod.movement;

/** Engine-independent vector; velocities in blocks/second, positions in blocks. */
public record Vector(double x, double y, double z) {
    public static final Vector ZERO = new Vector(0, 0, 0);
    public Vector add(Vector other) { return new Vector(x + other.x, y + other.y, z + other.z); }
    public Vector subtract(Vector other) { return new Vector(x - other.x, y - other.y, z - other.z); }
    public Vector scale(double factor) { return new Vector(x * factor, y * factor, z * factor); }
    public double horizontalLength() { return Math.hypot(x, z); }
    public double lengthSquared() { return x * x + y * y + z * z; }
    public double dot(Vector other) { return x * other.x + y * other.y + z * other.z; }
}
