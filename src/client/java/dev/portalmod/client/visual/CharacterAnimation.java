package dev.portalmod.client.visual;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Tick-driven animation uses tracked positions, velocity, ground/crouch flags and authoritative shot events.
 * No input buttons or render-frequency integration are used for remote characters. */
public final class CharacterAnimation {
    public record Pose(float stride, float speed, float lean, float tilt, float crouch, float air,
                       float jump, float land, float pitch, float yaw, float recoil, float idle) {
        public static final Pose ZERO = new Pose(0,0,0,0,0,0,0,0,0,0,0,0);
        Pose blend(Pose b,float a) { return new Pose(m(stride,b.stride,a),m(speed,b.speed,a),m(lean,b.lean,a),m(tilt,b.tilt,a),m(crouch,b.crouch,a),m(air,b.air,a),m(jump,b.jump,a),m(land,b.land,a),m(pitch,b.pitch,a),m(yaw,b.yaw,a),m(recoil,b.recoil,a),m(idle,b.idle,a)); }
    }
    private static class Track {
        UUID uuid; Vec3 position, velocity=Vec3.ZERO; boolean grounded;
        Pose old=Pose.ZERO, pose=Pose.ZERO;
        float phase, jump, land, recoil, fallSpeed;
    }
    private static final Map<Integer,Track> tracks=new HashMap<>();
    private static final Map<UUID,Long> fireTicks=new HashMap<>();
    private CharacterAnimation() { }
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.level==null || client.isPaused()) return;
            var players=client.level.players();
            tracks.entrySet().removeIf(e -> players.stream().noneMatch(p -> p.getId()==e.getKey() && p.getUUID().equals(e.getValue().uuid)));
            for(var player:players) update(player);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client) -> { tracks.clear(); fireTicks.clear(); });
    }
    public static void fired(UUID owner,long tick) {
        Long previous=fireTicks.put(owner,tick); if(previous!=null && previous>=tick) return;
        for(Track t:tracks.values()) if(owner.equals(t.uuid)) t.recoil=1;
    }
    private static void update(Player p) {
        var c=PortalVisualConfig.current;
        Track t=tracks.computeIfAbsent(p.getId(),id -> { Track n=new Track(); n.uuid=p.getUUID(); n.position=p.position(); n.grounded=p.onGround(); return n; });
        Vec3 velocity=p.isLocalPlayer()?p.getDeltaMovement().scale(20):p.position().subtract(t.position).scale(20);
        boolean discontinuity=p.position().distanceToSqr(t.position)>64;
        if(discontinuity) { velocity=Vec3.ZERO; t.fallSpeed=0; t.land=0; t.jump=0; }
        float speed=(float)Math.sqrt(velocity.x*velocity.x+velocity.z*velocity.z);
        if(!p.onGround()) t.fallSpeed=Math.max(t.fallSpeed,(float)-velocity.y);
        if(!discontinuity && p.onGround() && !t.grounded) {
            if(p.isLocalPlayer()) CameraEffects.landed(t.fallSpeed);
            t.land=Math.clamp(t.fallSpeed/c.landingReferenceSpeed,0,1); t.fallSpeed=0;
        }
        if(!discontinuity && !p.onGround() && t.grounded && velocity.y>0) t.jump=1;
        double yaw=Math.toRadians(p.yBodyRot),right=velocity.x*Math.cos(yaw)+velocity.z*Math.sin(yaw);
        Vec3 acceleration=velocity.subtract(t.velocity).scale(20);
        float forwardAcceleration=(float)(-acceleration.x*Math.sin(yaw)+acceleration.z*Math.cos(yaw));
        t.phase+=speed*.05f*c.strideRadiansPerBlock;
        float bodyYaw=p.getYRot()-p.yBodyRot; bodyYaw=(bodyYaw+540)%360-180;
        Pose target=new Pose(t.phase,Math.clamp(speed/c.runSpeed,0,1),c.strafeLean?(float)Math.clamp(-right/c.runSpeed,-1,1)*c.strafeLeanDegrees:0,
            c.accelerationTilt?Math.clamp(forwardAcceleration/c.accelerationScale,-1,1)*c.accelerationTiltDegrees:0,
            c.crouchPose && p.isCrouching()?1:0,c.airbornePose && !p.onGround()?1:0,c.jumpPose?t.jump:0,c.landingSquash?t.land:0,
            Math.clamp(p.getXRot(),-c.aimPitchLimit,c.aimPitchLimit),Math.clamp(bodyYaw,-c.headYawLimit,c.headYawLimit),
            c.characterRecoil?t.recoil:0,c.idleWeightShift?(float)Math.sin(p.tickCount*.05*c.idleFrequency)*c.idleShiftDegrees*(1-Math.clamp(speed/c.runSpeed,0,1)):0);
        t.old=t.pose;
        float smooth=1-(float)Math.exp(-c.poseResponse*.05);
        t.pose=t.pose.blend(target,smooth);
        float aim=1-(float)Math.exp(-c.aimResponse*.05);
        t.pose=new Pose(target.stride,t.pose.speed,t.pose.lean,t.pose.tilt,t.pose.crouch,t.pose.air,t.pose.jump,t.pose.land,m(t.old.pitch,target.pitch,aim),m(t.old.yaw,target.yaw,aim),t.pose.recoil,t.pose.idle);
        t.jump=Math.max(0,t.jump-.05f/c.takeoffRecovery); t.land=Math.max(0,t.land-.05f/c.landingRecovery); t.recoil=Math.max(0,t.recoil-.05f/c.characterRecoilRecovery);
        t.position=p.position(); t.velocity=velocity; t.grounded=p.onGround();
    }
    public static Pose pose(int id,float partial) {
        Track t=tracks.get(id); return t==null || !PortalVisualConfig.current.characterAnimation?Pose.ZERO:t.old.blend(t.pose,Math.clamp(partial,0,1));
    }
    public static float partialTicks() { return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false); }
    private static float m(float a,float b,float t) { return a+(b-a)*t; }
}
