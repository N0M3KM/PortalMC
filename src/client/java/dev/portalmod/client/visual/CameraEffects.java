package dev.portalmod.client.visual;

import dev.portalmod.assets.EffectCurve;
import net.minecraft.client.Minecraft;

/** One camera-only effects layer. It never mutates player aim, velocity or server inputs. */
public final class CameraEffects {
    public record Offset(float pitch,float roll) { }
    private static double landedAt=-100, firedAt=-100, exitedAt=-100;
    private static float impact, exitSpeed;
    private CameraEffects() { }
    private static double time() { var mc=Minecraft.getInstance(); return mc.level==null?0:(mc.level.getGameTime()+CharacterAnimation.partialTicks())*.05; }
    public static void landed(float speed) { impact=speed; landedAt=time(); PhysicsHud.landed(speed); }
    public static void fire() { firedAt=time(); }
    public static void exited(float speed) { exitSpeed=speed; exitedAt=time(); }
    public static Offset sample() {
        var c=PortalVisualConfig.current;
        if(!c.cameraEffects || c.reduceMotion) return new Offset(0,0);
        double now=time();
        float land=c.landingViewPunch?EffectCurve.pulse(now-landedAt,c.landingPunchSeconds)*Math.clamp(impact/c.landingReferenceSpeed,0,1)*c.landingPunchDegrees:0;
        float fire=c.fireViewPunch?EffectCurve.pulse(now-firedAt,c.firePunchSeconds)*c.firePunchDegrees:0;
        float exit=c.exitViewPunch?EffectCurve.pulse(now-exitedAt,c.exitPunchSeconds)*Math.clamp(exitSpeed/c.exitReferenceSpeed,0,1):0;
        return new Offset((land-fire+exit*c.exitPunchDegrees)*c.motionIntensity,exit*c.exitRollDegrees*c.motionIntensity);
    }
    public static void clear() { landedAt=-100; firedAt=-100; exitedAt=-100; impact=0; exitSpeed=0; }
}
