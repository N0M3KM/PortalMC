package dev.portalmod.client.portal;

import dev.portalmod.movement.MinecraftCollisionWorld;
import dev.portalmod.movement.Vector;
import dev.portalmod.portal.PortalFrame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/** Original opening/rim/wisp motion. Particle simulation is tick driven and globally bounded. */
public final class PortalEffects {
    private record Closing(PortalFrame frame,double at) { }
    public static final class Particle {
        public Vector position, old, velocity;
        public final int color; public final double born;
        Particle(Vector p,Vector v,int color,double now) { position=p; old=p; velocity=v; this.color=color; born=now; }
    }
    private static final Map<Long,Double> births=new HashMap<>();
    private static final List<Closing> closing=new ArrayList<>();
    private static final List<Particle> particles=new ArrayList<>();
    private static final Random random=new Random();
    private PortalEffects() { }
    public static void initialize() { ClientTickEvents.END_CLIENT_TICK.register(client -> tick()); }
    public static double now() { var mc=Minecraft.getInstance(); return mc.level==null?0:(mc.level.getGameTime()+dev.portalmod.client.visual.CharacterAnimation.partialTicks())*.05; }
    public static double radiusY() { var c=PortalRenderer.config(); return Math.min(1,c.referenceHeightUnits/c.referenceWidthUnits*.5); }
    public static double radiusX() { var c=PortalRenderer.config(); return Math.min(.5,c.referenceWidthUnits/c.referenceHeightUnits); }
    public static void changed(List<PortalFrame> before,List<PortalFrame> after) {
        double now=now(); var c=PortalRenderer.config();
        for(PortalFrame p:after) if(before.stream().noneMatch(old -> old.id()==p.id())) {
            births.put(p.id(),now); if(c.openParticles) burst(p,c.particleBurst);
        }
        for(PortalFrame p:before) if(after.stream().noneMatch(next -> next.id()==p.id())) {
            if(c.portalCloseAnimation && closing.size()<256) closing.add(new Closing(p,now));
            if(c.closeParticles) burst(p,c.particleBurst);
            births.remove(p.id());
        }
    }
    public static List<PortalFrame> closing() { return closing.stream().map(Closing::frame).toList(); }
    public static double scale(PortalFrame frame) {
        var c=PortalRenderer.config(); double now=now();
        for(Closing old:closing) if(old.frame.id()==frame.id()) return 1-smooth((now-old.at)/c.closeSeconds);
        return c.portalOpenAnimation?smooth((now-births.getOrDefault(frame.id(),now-c.openSeconds))/c.openSeconds):1;
    }
    private static double smooth(double n) { n=Math.clamp(n,0,1); return n*n*(3-2*n); }
    private static void tick() {
        var mc=Minecraft.getInstance(); if(mc.level==null || mc.player==null || mc.isPaused()) return;
        var c=PortalRenderer.config(); double now=now();
        closing.removeIf(p -> now-p.at>c.closeSeconds);
        particles.removeIf(p -> now-p.born>c.particleLifeSeconds);
        for(Particle p:particles) { p.old=p.position; p.position=p.position.add(p.velocity.scale(.05)); }
        if(c.ambientWisps && c.enabled) for(PortalFrame frame:ClientPortals.frames()) {
            if(!near(frame)) continue;
            double count=c.particleDensity; int n=(int)count+(random.nextDouble()<count%1?1:0);
            for(int i=0;i<n;i++) emit(frame,now);
        }
    }
    private static boolean near(PortalFrame p) {
        var mc=Minecraft.getInstance(); var c=PortalRenderer.config();
        return mc.player!=null && mc.level!=null && p.dimension().equals(mc.level.dimension().identifier().toString()) && p.center().subtract(MinecraftCollisionWorld.fromMinecraft(mc.player.position())).lengthSquared()<c.maxViewDistance*c.maxViewDistance;
    }
    private static void burst(PortalFrame p,int count) {
        if(!near(p)) return;
        var c=PortalRenderer.config(); int n=Math.min(c.particleCap,(int)Math.ceil(count*c.particleDensity));
        for(int i=0;i<n;i++) emit(p,now());
    }
    private static void emit(PortalFrame p,double now) {
        var c=PortalRenderer.config(); if(particles.size()>=c.particleCap) return;
        double a=random.nextDouble()*Math.PI*2; Vector radial=p.right().scale(Math.cos(a)).add(p.up().scale(Math.sin(a)));
        Vector pos=p.center().add(p.right().scale(Math.cos(a)*radiusX())).add(p.up().scale(Math.sin(a)*radiusY())).add(p.normal().scale(c.surfaceOffset*2));
        particles.add(new Particle(pos,radial.scale(c.particleSpeed).add(p.normal().scale(c.particleSpeed)),p.orange()?0xffff8a19:0xff28aaff,now));
    }
    public static void fizzle(boolean orange) {
        var mc=Minecraft.getInstance(); var c=PortalRenderer.config(); if(mc.player==null || !c.fizzleParticles) return;
        Vector at=MinecraftCollisionWorld.fromMinecraft(mc.player.getEyePosition().add(mc.player.getLookAngle().scale(ClientPortals.config().fizzleMissDistance)));
        for(int i=0;i<c.particleBurst*c.particleDensity && particles.size()<c.particleCap;i++) {
            Vector v=new Vector(random.nextGaussian(),random.nextGaussian(),random.nextGaussian()).scale(c.particleSpeed);
            particles.add(new Particle(at,v,orange?0xffff8a19:0xff28aaff,now()));
        }
    }
    public static List<Particle> particles() { return particles; }
    public static void clear() { births.clear(); closing.clear(); particles.clear(); }
}
