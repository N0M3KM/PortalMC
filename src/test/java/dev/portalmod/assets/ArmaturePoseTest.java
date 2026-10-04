package dev.portalmod.assets;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArmaturePoseTest {
    @Test void gripKeepsBothBoneLengthsAndReachesTarget() {
        Vector3f start=new Vector3f(0,0,0),target=new Vector3f(1,1,0);
        var p=ArmaturePose.solve(start,1,1,target,new Vector3f(0,-1,0));
        assertEquals(1,p[0].distance(start),1e-5); assertEquals(1,p[0].distance(p[1]),1e-5);
        assertTrue(p[1].distance(target)<1e-5);
    }
    @Test void unreachableOrParallelPoleStaysFiniteWithoutStretching() {
        var p=ArmaturePose.solve(new Vector3f(),1,1,new Vector3f(0,100,0),new Vector3f(0,1,0));
        assertEquals(1,p[0].length(),1e-4); assertEquals(1,p[0].distance(p[1]),1e-4); assertTrue(p[1].y<2);
        assertTrue(Float.isFinite(p[0].x)); assertTrue(Float.isFinite(p[0].z));
    }
    @Test void shoulderRotationMovesDescendantsWithoutMovingAnotherArm() {
        var bones=new SourceModel.Bone[]{
            joint("root",-1,0,0),joint("bicep_R",0,0,1),joint("elbow_R",1,1,1),joint("other",0,-1,0)};
        var model=new SourceModel(new SourceModel.Vertex[0],java.util.List.of(),bones);
        var pose=ArmaturePose.bind(model);
        ArmaturePose.rotate(model,pose,"bicep_R",new org.joml.Quaternionf().rotationZ((float)Math.PI/2));
        assertTrue(pose[2].getTranslation(new Vector3f()).distance(new Vector3f(0,2,0))<1e-5);
        assertTrue(pose[3].getTranslation(new Vector3f()).distance(new Vector3f(-1,0,0))<1e-5);
    }
    @Test void bindPoseSkinningIsIdentity() {
        var model=new SourceModel(new SourceModel.Vertex[0],java.util.List.of(),new SourceModel.Bone[]{joint("root",-1,0,0),joint("child",0,2,3)});
        for(var skin:ArmaturePose.skin(model,ArmaturePose.bind(model))) assertTrue(skin.equals(new org.joml.Matrix4f(),1e-6f));
    }
    private static SourceModel.Bone joint(String name,int parent,float x,float y) {
        return new SourceModel.Bone(name,parent,new float[]{x,y,0},new float[]{0,0,0,1},new float[]{1,0,0,-x,0,1,0,-y,0,0,1,0});
    }
}
