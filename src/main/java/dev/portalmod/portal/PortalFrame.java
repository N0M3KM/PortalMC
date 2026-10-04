package dev.portalmod.portal;

import dev.portalmod.movement.Vector;
import java.util.List;
import java.util.UUID;

/** An immutable 1x2 aperture; right cross up equals outward normal. */
public record PortalFrame(long id, UUID owner, boolean orange, String dimension, Vector center,
                          Vector right, Vector up, Vector normal, List<Cell> support) {
    public record Cell(int x, int y, int z) { }
    public double distance(Vector point) { return point.subtract(center).dot(normal); }
    public Vector rotateTo(PortalFrame exit, Vector v) {
        return exit.right.scale(-v.dot(right)).add(exit.up.scale(v.dot(up))).add(exit.normal.scale(-v.dot(normal)));
    }
    public Vector pointTo(PortalFrame exit, Vector p) { return exit.center.add(rotateTo(exit, p.subtract(center))); }
    public boolean contains(Vector p, double halfWidth, double halfHeight) {
        Vector offset = p.subtract(center);
        return Math.abs(offset.dot(right)) + halfWidth <= 0.50001 && Math.abs(offset.dot(up)) + halfHeight <= 1.00001;
    }
    public double crossing(Vector from, Vector to) {
        double a = distance(from), b = distance(to);
        if (a < 0 || b >= 0 || a == b) return -1;
        return a / (a - b);
    }
    /** Cheap swept AABB rejection before plane/hull math. Includes endpoint contact and fast sweeps. */
    public boolean intersectsSweep(Vector from,Vector to,double halfX,double halfY,double halfZ,double margin) {
        double x=Math.abs(right.x())*.5+Math.abs(up.x())+Math.abs(normal.x())*margin+halfX;
        double y=Math.abs(right.y())*.5+Math.abs(up.y())+Math.abs(normal.y())*margin+halfY;
        double z=Math.abs(right.z())*.5+Math.abs(up.z())+Math.abs(normal.z())*margin+halfZ;
        return Math.max(from.x(),to.x())>=center.x()-x && Math.min(from.x(),to.x())<=center.x()+x
            && Math.max(from.y(),to.y())>=center.y()-y && Math.min(from.y(),to.y())<=center.y()+y
            && Math.max(from.z(),to.z())>=center.z()-z && Math.min(from.z(),to.z())<=center.z()+z;
    }
}
