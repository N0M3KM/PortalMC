package dev.portalmod.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.portalmod.assets.ArmaturePose;
import dev.portalmod.assets.LegMotion;
import dev.portalmod.client.assets.LocalPortalAssets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Independently authored human proportions, orange trousers, tank top and long-fall braces. */
public final class OriginalCharacter {
    private OriginalCharacter() { }
    public static void submit(AvatarRenderState state,CharacterAnimation.Pose a,PoseStack pose,SubmitNodeCollector nodes,boolean holding) {
        var c=PortalVisualConfig.current; int light=state.lightCoords;
        Quaternionf torso=new Quaternionf().rotationXYZ(rad(a.tilt()+a.crouch()*c.crouchBendDegrees),0,rad(a.lean()+a.idle()));
        Vector3f hip=new Vector3f(0,38,0),chest=around(new Vector3f(0,59,0),hip,torso);
        OriginalGeometry.tube(pose,nodes,hip,chest,8,0xffdeddda,light);
        OriginalGeometry.tube(pose,nodes,new Vector3f(0,34,0),hip,9,0xffe77227,light);
        Vector3f head=around(new Vector3f(0,67,0),hip,torso);
        pose.pushPose(); pose.translate(head.x,head.y,head.z);
        pose.rotateDegrees(Axis.YP,c.headFollow?-a.yaw():0); pose.rotateDegrees(Axis.XP,c.headFollow?-Math.clamp(a.pitch(),-c.headPitchLimit,c.headPitchLimit):0);
        OriginalGeometry.tube(pose,nodes,new Vector3f(0,-4,0),new Vector3f(0,3,0),4.5f,0xffefc5a4,light);
        OriginalGeometry.tube(pose,nodes,new Vector3f(0,2,1),new Vector3f(0,5,1),4.8f,0xff4c3024,light);
        OriginalGeometry.tube(pose,nodes,new Vector3f(0,2,3),new Vector3f(0,-5,4),2,0xff4c3024,light); pose.popPose();
        for(String side:new String[]{"R","L"}) {
            float sign=side.equals("R")?-1:1;
            var step=LegMotion.step(a.stride()+(side.equals("L")?(float)Math.PI:0),c.strideAnimation?a.speed():0,a.crouch(),c.footStrideUnits,c.footLiftUnits,c.crouchStanceUnits);
            float drop=a.crouch()*c.crouchDropUnits+a.land()*c.landingMaxDropUnits;
            Vector3f leg=new Vector3f(sign*5,36,0),footTarget=new Vector3f(sign*(5+step.stance()),2+drop+step.lift()+a.air()*c.airFootLiftUnits,-step.forward()-a.jump()*c.jumpFootForwardUnits);
            var legJoints=ArmaturePose.solve(leg,17,17,footTarget,new Vector3f(sign*.1f,0,-1));
            Vector3f knee=legJoints[0],ankle=legJoints[1];
            OriginalGeometry.tube(pose,nodes,leg,knee,4.3f,0xffe77227,light);
            OriginalGeometry.tube(pose,nodes,knee,ankle,3.1f,0xffed812b,light);
            OriginalGeometry.tube(pose,nodes,new Vector3f(knee).add(sign*3,0,2),new Vector3f(ankle).add(sign*3,0,3),1.5f,0xffeff4f4,light);
            OriginalGeometry.tube(pose,nodes,ankle,new Vector3f(ankle).add(0,0,-5),3.4f,0xff202a31,light);
            Vector3f shoulder=around(new Vector3f(sign*7,61,0),hip,torso);
            Vector3f target=holding?new Vector3f(side.equals("R")?c.gripX:c.supportGripX,side.equals("R")?c.gripY:c.supportGripY,-(side.equals("R")?c.gripZ:c.supportGripZ)):new Vector3f(sign*9,40,(float)Math.sin(a.stride())*a.speed()*sign*4);
            if(holding) target.sub(0,60,0).rotate(new Quaternionf().rotationYXZ(rad(-a.yaw()),rad(-a.pitch()+a.recoil()*c.characterRecoilDegrees),0)).add(0,60,0);
            var joints=ArmaturePose.solve(shoulder,11,11,target,new Vector3f(sign*c.armPoleOut,c.armPoleDown,-c.armPoleForward));
            OriginalGeometry.tube(pose,nodes,shoulder,joints[0],2.2f,0xffefc5a4,light);
            OriginalGeometry.tube(pose,nodes,joints[0],joints[1],1.8f,0xffefc5a4,light);
            OriginalGeometry.tube(pose,nodes,joints[1],new Vector3f(joints[1]).add(0,0,-2),2,0xffefc5a4,light);
            if(holding && side.equals("R") && c.thirdPersonGun) {
                pose.pushPose(); pose.translate(joints[1].x,joints[1].y,joints[1].z);
                pose.rotateDegrees(Axis.YP,-a.yaw()); pose.rotateDegrees(Axis.XP,-a.pitch()+a.recoil()*c.characterRecoilDegrees);
                pose.scale(LocalPortalAssets.config.unitsPerBlock,LocalPortalAssets.config.unitsPerBlock,LocalPortalAssets.config.unitsPerBlock);
                LocalPortalAssets.submitHeldGun(pose,nodes,light,state.id); pose.popPose();
            }
        }
    }
    private static Vector3f around(Vector3f point,Vector3f pivot,Quaternionf rotation) { return point.sub(pivot).rotate(rotation).add(pivot); }
    private static float rad(float angle) { return (float)Math.toRadians(angle); }
}
