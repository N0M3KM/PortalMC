package dev.portalmod.portal;

import dev.portalmod.movement.Vector;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalFrameTest {
    private static final Vector[] AXES = {new Vector(1,0,0), new Vector(-1,0,0), new Vector(0,1,0), new Vector(0,-1,0), new Vector(0,0,1), new Vector(0,0,-1)};
    private static PortalFrame frame(Vector n, Vector center) {
        Vector u = Math.abs(n.y()) == 1 ? new Vector(0,0,1) : new Vector(0,1,0);
        return new PortalFrame(1, new UUID(0,1), false, "minecraft:overworld", center, u.cross(n), u, n, List.of());
    }
    @Test void allThirtySixOrientationPairsPreserveMomentumAndRoundTrip() {
        Vector velocity = new Vector(4.25, -17.5, 11), point = new Vector(0.2, 0.3, -0.7);
        for (Vector a : AXES) for (Vector b : AXES) {
            PortalFrame entry = frame(a, Vector.ZERO), exit = frame(b, new Vector(104,79,-64));
            Vector transformed = entry.rotateTo(exit, velocity);
            assertEquals(velocity.lengthSquared(), transformed.lengthSquared(), 1e-10);
            assertEquals(0, exit.rotateTo(entry, transformed).subtract(velocity).lengthSquared(), 1e-10);
            assertEquals(0, exit.pointTo(entry, entry.pointTo(exit, point)).subtract(point).lengthSquared(), 1e-10);
            assertEquals(0, entry.normal().scale(-1).subtract(exit.rotateTo(entry, exit.normal())).lengthSquared(), 1e-10);
        }
    }
    @Test void crossingRequiresFrontToBackAndFitsEntireHull() {
        PortalFrame p = frame(new Vector(0,0,-1), Vector.ZERO);
        assertEquals(.5, p.crossing(new Vector(0,0,-2), new Vector(0,0,2)), 1e-10);
        assertEquals(-1, p.crossing(new Vector(0,0,2), new Vector(0,0,-2)));
        assertTrue(p.contains(new Vector(0,-.1,0), .3, .9));
        assertFalse(p.contains(new Vector(.25,0,0), .3, .9));
        assertFalse(p.contains(new Vector(0,.2,0), .3, .9));
    }
    @Test void broadPhaseKeepsFastCrossingsAndRejectsDistantSweeps() {
        for(Vector axis:AXES) {
            PortalFrame p=frame(axis,Vector.ZERO);
            assertTrue(p.intersectsSweep(axis.scale(100),axis.scale(-100),.3,.9,.3,.1));
            Vector distant=p.right().scale(100);
            assertFalse(p.intersectsSweep(distant.add(axis),distant.subtract(axis),.3,.9,.3,.1));
        }
    }
}
