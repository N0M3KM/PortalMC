package dev.portalmod.assets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EffectCurveTest {
    @Test void pulseNeverOutlivesItsEffectOrExceedsItsIntensity() {
        assertEquals(0,EffectCurve.pulse(-1,.3)); assertEquals(0,EffectCurve.pulse(0,.3)); assertEquals(0,EffectCurve.pulse(.3,.3));
        assertEquals(0,EffectCurve.pulse(Double.NaN,.3)); assertEquals(0,EffectCurve.pulse(1,0));
        for(int i=0;i<=1000;i++) { float value=EffectCurve.pulse(i*.0003,.3); assertTrue(value>=0 && value<=1); }
        assertTrue(EffectCurve.pulse(.1,.3)>.5);
    }
}
