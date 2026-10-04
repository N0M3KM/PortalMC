package dev.portalmod.client.visual;

import dev.portalmod.assets.SourceAnimations;
import dev.portalmod.assets.SourceModel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

/** Playback of locally installed animations, driven by tracked Minecraft motion. Never runs Source. */
public final class AuthoredCharacterAnimation {
    private AuthoredCharacterAnimation() { }
    public static Matrix4f[] pose(SourceAnimations library,SourceModel model,int entityId,boolean gun) {
        var mc=Minecraft.getInstance();var e=mc.level==null?null:mc.level.getEntity(entityId);
        if(!(e instanceof Player player)) return dev.portalmod.assets.ArmaturePose.bind(model);
        var c=PortalVisualConfig.current;var a=CharacterAnimation.pose(entityId,CharacterAnimation.partialTicks());
        var motion=CharacterAnimation.motion(entityId);
        double seconds=(player.tickCount+CharacterAnimation.partialTicks())/20.0;
        String prefix=gun?"portalgun_":"nogun_";
        double yaw=Math.toRadians(player.yBodyRot),speed=motion.velocity().horizontalDistance();
        float right=speed<1e-5?0:(float)(-(motion.velocity().x*Math.cos(yaw)+motion.velocity().z*Math.sin(yaw))/speed);
        float forward=speed<1e-5?0:(float)((-motion.velocity().x*Math.sin(yaw)+motion.velocity().z*Math.cos(yaw))/speed);
        String idle=prefix+"standing_idle",run=prefix+"run",crouch=prefix+"crouch_idle",walk=prefix+"crouchWalk";
        float moving=c.strideAnimation?Math.clamp(a.speed()*c.runSpeed/c.authoredMovingBlendSpeed,0,1):0;
        var standing=SourceAnimations.blend(library.sample(idle,c.idleWeightShift?seconds/library.duration(idle):0,0,0),
            library.sample(run,cycle(library,run,motion.distanceBlocks(),c.authoredUnitsPerBlock),right,forward),moving);
        var low=SourceAnimations.blend(library.sample(crouch,c.idleWeightShift?seconds/library.duration(crouch):0,0,0),
            library.sample(walk,cycle(library,walk,motion.distanceBlocks(),c.authoredUnitsPerBlock),right,forward),moving);
        var base=SourceAnimations.blend(standing,low,a.crouch());
        if(c.airbornePose && a.air()>0) {
            String jump=prefix+"standing_jump",air=gun?"portalgun_jump_float":"nogun_airwalk";
            double jumpDuration=library.duration(jump);
            var airborne=motion.airSeconds()<jumpDuration && c.jumpPose?library.sample(jump,motion.airSeconds()/jumpDuration,0,0)
                : library.sample(air,seconds/library.duration(air),0,0);
            base=SourceAnimations.blend(base,airborne,a.air());
        }
        if(c.headFollow || gun && c.armPose) {
            // Original yaw/pitch grids: x ranges +45..-45, y ranges -45..+90.
            if(gun && c.armPose) {
                var high=library.layer(base,prefix+"standing_aimmatrix",0,-a.yaw(),-a.pitch(),1);
                var lowAim=library.layer(base,prefix+"crouch_aimmatrix",0,-a.yaw(),-a.pitch(),1);
                base=SourceAnimations.blend(high,lowAim,a.crouch());
            }
        }
        if(c.headFollow) base=library.layer(base,"nogun_head_aimmatrix",0,-a.yaw()/45,a.pitch()/45,1);
        if(c.landingSquash && motion.landSeconds()<library.duration(prefix+"jump_land"))
            base=library.layer(base,prefix+"jump_land",motion.landSeconds()/library.duration(prefix+"jump_land"),0,0,a.land());
        if(gun && c.characterRecoil && motion.fireSeconds()<library.duration("portalgun_standing_fire")) {
            double phase=motion.fireSeconds()/library.duration("portalgun_standing_fire");
            var high=library.layer(base,"portalgun_standing_fire",phase,0,0,1);
            var lowFire=library.layer(base,"portalgun_crouch_fire",motion.fireSeconds()/library.duration("portalgun_crouch_fire"),0,0,1);
            base=SourceAnimations.blend(high,lowFire,a.crouch());
        }
        return library.portalMeshPose(model,base);
    }
    private static double cycle(SourceAnimations library,String name,double distance,float units) {
        float travel=library.travel(name); return distance*units/Math.max(1,travel);
    }
}
