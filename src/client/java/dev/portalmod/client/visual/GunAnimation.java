package dev.portalmod.client.visual;

import dev.portalmod.client.assets.LocalPortalAssets;
import net.minecraft.client.Minecraft;

/** Original motion curves; all timings are estimated, not Valve animation data. */
public final class GunAnimation {
    public record Pose(float kick,float opening,float bobX,float bobY,float sway,float fizzle,int color) { }
    private static long firedAt, resultAt;
    private record Remote(long at,boolean orange,boolean rejected) { }
    private static final java.util.Map<java.util.UUID,Remote> remote=new java.util.HashMap<>();
    public static void remoteResult(java.util.UUID owner,boolean color,boolean accepted) { remote.put(owner,new Remote(System.nanoTime(),color,!accepted)); }
    public static Pose forEntity(int id) {
        var mc=Minecraft.getInstance(); var entity=mc.level==null?null:mc.level.getEntity(id);
        if(entity==null || entity==mc.player) return sample();
        Remote event=remote.get(entity.getUUID());
        return event==null?sampleAt(0,0,false,false,false):sampleAt(event.at,event.at,event.orange,event.rejected,false);
    }
    private static boolean orange, rejected;
    private GunAnimation() { }
    public static void fired(boolean color) { orange=color; rejected=false; firedAt=System.nanoTime(); CameraEffects.fire(); }
    public static void result(boolean color,boolean accepted) { orange=color; rejected=!accepted; resultAt=System.nanoTime(); }
    public static Pose sample() { return sampleAt(firedAt,resultAt,orange,rejected,true); }
    private static Pose sampleAt(long firedAt,long resultAt,boolean orange,boolean rejected,boolean local) {
        var c=PortalVisualConfig.current; var player=local?Minecraft.getInstance().player:null;
        double now=System.nanoTime()*1e-9, elapsed=now-firedAt*1e-9, result=now-resultAt*1e-9;
        float pulse=elapsed<2?(float)((1-Math.exp(-elapsed/c.fireAttack))*Math.exp(-elapsed/c.fireRecovery)):0;
        float speed=player==null?0:Math.clamp((float)(player.getDeltaMovement().horizontalDistance()*20/c.runSpeed),0,1);
        float phase=player==null?0:CharacterAnimation.pose(player.getId(),CharacterAnimation.partialTicks()).stride()*c.bobFrequency/c.strideRadiansPerBlock;
        float bob=c.gunMovementBob && !c.reduceMotion?c.bobAmplitude*speed:0;
        float shake=c.gunFizzleAnimation && !c.reduceMotion && rejected && result<c.fizzleDuration?(float)(Math.sin(result*c.fizzleFrequency)*Math.sin(Math.PI*result/c.fizzleDuration)):0;
        int color=orange?0xffff8a19:0xff28aaff;
        float brightness=1-c.glowPulse*(float)(.5+.5*Math.sin(now*c.swayFrequency));
        color=0xff000000 | (int)(((color>>16)&255)*brightness)<<16 | (int)(((color>>8)&255)*brightness)<<8 | (int)((color&255)*brightness);
        return new Pose(c.gunRecoil && !c.reduceMotion?pulse:0,c.gunProngs?pulse:0,(float)Math.sin(phase)*bob,(float)-Math.abs(Math.cos(phase))*bob,
            c.gunIdleSway && !c.reduceMotion?(float)Math.sin(now*c.swayFrequency)*LocalPortalAssets.config.idleSway:0,shake,color);
    }
    public static void clear() { firedAt=0; resultAt=0; rejected=false; orange=false; remote.clear(); }
}
