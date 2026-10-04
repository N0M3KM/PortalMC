package dev.portalmod.assets;

/** Original planted-foot gait, shared by imported and fallback rigs. Coordinates use Source units. */
public final class LegMotion {
    public record Step(float forward, float lift, float stance) { }
    private LegMotion() { }
    public static Step step(float phase, float speed, float crouch, float stride, float lift, float stance) {
        float swing=(float)Math.sin(phase), amount=Math.clamp(speed,0,1)*(1-.5f*Math.clamp(crouch,0,1));
        // Half-cycle on the ground; the other half lifts at the knee rather than swinging a rigid leg.
        return new Step(swing*stride*amount, Math.max(0,(float)Math.cos(phase))*lift*amount, stance*crouch);
    }
}
