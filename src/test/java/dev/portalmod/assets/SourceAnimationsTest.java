package dev.portalmod.assets;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceAnimationsTest {
    @Test void mixedConstantAndAnimatedChannelsInterpolateAndRetarget() {
        var library=SourceAnimations.decode(fixture(),null,1024);
        var a=library.sample("test",.5,0,0);
        assertEquals(1,a.position[0].y,1e-5); assertEquals(1,a.position[1].x,1e-5);
        var target=new SourceModel(new SourceModel.Vertex[0],List.of(),new SourceModel.Bone[]{joint("root",-1,0),joint("child",0,1)});
        var pose=library.modelPose(target,a);
        assertTrue(pose[0].getTranslation(new Vector3f()).distance(new Vector3f(0,1,0))<1e-5);
        assertTrue(pose[1].getTranslation(new Vector3f()).distance(new Vector3f(1,1,0))<1e-5);
    }
    @Test void budgetUnsupportedEncodingAndMalformedFrameStrideFailExplicitly() {
        assertThrows(IllegalArgumentException.class,()->SourceAnimations.decode(fixture(),null,1));
        var wrong=fixture();wrong.put(1536+24,(byte)128);
        assertThrows(IllegalArgumentException.class,()->SourceAnimations.decode(wrong,null,1024));
        var stride=fixture();stride.putInt(1536+8,14);
        assertThrows(IllegalArgumentException.class,()->SourceAnimations.decode(stride,null,1024));
    }
    @Test void packedValuesKeepTheirSignsAndFiniteQuaternion() {
        assertEquals(1,SourceAnimations.half((short)0x3c00));
        assertEquals(-2,SourceAnimations.half((short)0xc000));
        assertEquals(Math.scalb(1f,-24),SourceAnimations.half((short)1));
        var b=ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);quat(b,0);
        assertEquals(1,SourceAnimations.quaternion48(b,0).w,1e-5);
        b.putShort(4,(short)0xc000);assertEquals(-1,SourceAnimations.quaternion48(b,0).w,1e-5);
    }
    @Test void sourceAxesBecomeTheExistingMeshUpAndFacingAxes() {
        var basis=SourceAnimations.sourceToMesh();
        assertTrue(basis.transformDirection(new Vector3f(0,0,1)).distance(new Vector3f(0,1,0))<1e-5);
        assertTrue(basis.transformDirection(new Vector3f(1,0,0)).distance(new Vector3f(0,0,1))<1e-5);
        assertTrue(basis.transformDirection(new Vector3f(0,1,0)).distance(new Vector3f(1,0,0))<1e-5);
    }
    private static SourceModel.Bone joint(String name,int parent,float x) {
        return new SourceModel.Bone(name,parent,new float[]{x,0,0},new float[]{0,0,0,1},new float[]{1,0,0,-x,0,1,0,0,0,0,1,0});
    }
    private static ByteBuffer fixture() {
        var b=ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0,0x54534449);b.putInt(4,49);b.putInt(156,2);b.putInt(160,512);
        for(int i=0;i<2;i++) {
            int bone=512+i*216,at=2800+i*32;b.putInt(bone,at-bone);text(b,at,i==0?"root":"child");b.putInt(bone+4,i-1);
            b.putFloat(bone+32,i);b.putFloat(bone+56,1);
        }
        int a=1024;b.putInt(180,1);b.putInt(184,a);b.putInt(a+4,2900-a);text(b,2900,"@test");
        b.putFloat(a+8,30);b.putInt(a+12,64);b.putInt(a+16,2);b.putInt(a+56,1536-a);b.putInt(352,1);
        int f=1536;b.putInt(f,32);b.putInt(f+4,44);b.putInt(f+8,12);b.put(f+24,(byte)12);b.put(f+25,(byte)3);
        quat(b,f+32);b.putShort(f+38,(short)0x3c00); // Constant child translation (1,0,0).
        quat(b,f+44);quat(b,f+56);b.putShort(f+64,(short)0x4000); // Root rises to y=2 at frame one.
        int s=1152;b.putInt(188,1);b.putInt(192,s);b.putInt(s+4,2932-s);text(b,2932,"test");
        b.putInt(s+56,1);b.putInt(s+60,3000-s);b.putInt(s+68,1);b.putInt(s+72,1);
        b.putInt(s+156,3020-s);b.putFloat(3020,1);b.putFloat(3024,1);
        return b;
    }
    private static void quat(ByteBuffer b,int p) { b.putShort(p,(short)0x8000);b.putShort(p+2,(short)0x8000);b.putShort(p+4,(short)0x4000); }
    private static void text(ByteBuffer b,int p,String text) { var data=text.getBytes(StandardCharsets.UTF_8);for(int i=0;i<data.length;i++)b.put(p+i,data[i]); }
}
