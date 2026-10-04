package dev.portalmod.assets;

/** Finite, smooth, bounded pulse for independently authored visual effects. */
public final class EffectCurve {
    private EffectCurve() { }
    public static float pulse(double elapsed,double duration) {
        if(!Double.isFinite(elapsed) || !Double.isFinite(duration) || duration<=0 || elapsed<=0 || elapsed>=duration) return 0;
        double t=elapsed/duration; return (float)(Math.sin(t*Math.PI)*Math.exp(-t));
    }
}
