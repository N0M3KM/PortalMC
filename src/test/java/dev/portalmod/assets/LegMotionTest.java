package dev.portalmod.assets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegMotionTest {
    @Test void opposingFeetHaveDistinctSwingAndPlantPhases() {
        var a=LegMotion.step(0,1,0,7,4,2); var b=LegMotion.step((float)Math.PI,1,0,7,4,2);
        assertEquals(4,a.lift(),1e-5); assertEquals(0,b.lift(),1e-5);
        assertEquals(0,LegMotion.step(0,0,0,7,4,2).lift());
    }
    @Test void crouchingWidensStanceAndShortensTheStep() {
        var a=LegMotion.step((float)Math.PI/2,1,0,7,4,2);
        var b=LegMotion.step((float)Math.PI/2,1,1,7,4,2);
        assertEquals(a.forward()*.5,b.forward(),1e-5); assertEquals(2,b.stance());
    }
}
