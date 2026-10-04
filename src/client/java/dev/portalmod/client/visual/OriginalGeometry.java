package dev.portalmod.client.visual;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Original untextured rounded geometry; no Valve meshes or textures are inputs. */
public final class OriginalGeometry {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("portalmod", "runtime/original_white");
    private static boolean ready;
    private static final Identifier PARTICLE = Identifier.fromNamespaceAndPath("portalmod","runtime/original_wisp");
    private static boolean particleReady;
    private OriginalGeometry() { }
    public static void sprite(SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera,
                              dev.portalmod.movement.Vector position,float size,int color) {
        if(!particleReady) {
            NativeImage image=new NativeImage(16,16,false);
            for(int y=0;y<16;y++) for(int x=0;x<16;x++) {
                double r=Math.sqrt(Math.pow((x-7.5)/7.5,2)+Math.pow((y-7.5)/7.5,2));
                image.setPixel(x,y,((int)(Math.pow(Math.max(0,1-r),2)*255)<<24)|0xffffff);
            }
            Minecraft.getInstance().getTextureManager().register(PARTICLE,new DynamicTexture(() -> "Original radial wisp",image)); particleReady=true;
        }
        Vector3f center=new Vector3f((float)(position.x()-camera.pos.x),(float)(position.y()-camera.pos.y),(float)(position.z()-camera.pos.z));
        Vector3f right=new Vector3f(size,0,0).rotate(camera.orientation),up=new Vector3f(0,size,0).rotate(camera.orientation);
        Vector3f[] v={new Vector3f(center).sub(right).sub(up),new Vector3f(center).add(right).sub(up),new Vector3f(center).add(right).add(up),new Vector3f(center).sub(right).add(up)};
        collector.submitCustomGeometry(new PoseStack(),RenderTypes.entityTranslucent(PARTICLE),(pose,buffer) -> {
            for(int i=0;i<4;i++) buffer.addVertex(pose,v[i].x,v[i].y,v[i].z).setColor(color).setUv(i==0 || i==3?0:1,i<2?1:0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT).setNormal(pose,0,0,1);
        });
    }
    public static void tube(PoseStack pose, SubmitNodeCollector collector, Vector3f start, Vector3f end, float radius, int color, int light) {
        if (!ready) {
            NativeImage image = new NativeImage(1,1,false); image.setPixel(0,0,-1);
            Minecraft.getInstance().getTextureManager().register(WHITE, new DynamicTexture(() -> "Original procedural material", image)); ready = true;
        }
        Vector3f axis = new Vector3f(end).sub(start); float length = axis.length();
        if (length < 1e-5f) return;
        pose.pushPose(); pose.translate(start.x,start.y,start.z); pose.mulPose(new org.joml.Matrix4f().rotation(new Quaternionf().rotationTo(new Vector3f(0,1,0),axis.div(length))));
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (saved, buffer) -> {
            // Eight radial segments, two caps. Winding faces outward.
            for (int i=0;i<8;i++) {
                double a=i*Math.PI/4,b=(i+1)*Math.PI/4;
                float x=(float)Math.cos(a),z=(float)Math.sin(a),nx=(float)Math.cos(b),nz=(float)Math.sin(b);
                float[][] v={{x*radius,0,z*radius},{x*radius,length,z*radius},{nx*radius,length,nz*radius},{nx*radius,0,nz*radius}};
                for(float[] p:v) buffer.addVertex(saved,p[0],p[1],p[2]).setColor(color).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(saved,(x+nx)*.5f,0,(z+nz)*.5f);
                for (int cap=0;cap<2;cap++) {
                    float y=cap*length; float[][] c=cap==0?new float[][]{{0,y,0},{x*radius,y,z*radius},{nx*radius,y,nz*radius},{0,y,0}}:new float[][]{{0,y,0},{nx*radius,y,nz*radius},{x*radius,y,z*radius},{0,y,0}};
                    for(float[] p:c) buffer.addVertex(saved,p[0],p[1],p[2]).setColor(color).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(saved,0,cap==0?-1:1,0);
                }
            }
        }); pose.popPose();
    }
    public static void hand(PoseStack pose, SubmitNodeCollector collector, Vector3f wrist, Vector3f grip, int light) {
        var c=PortalVisualConfig.current;
        tube(pose,collector,wrist,grip,c.handRadius,0xffefc5a4,light);
        // Original thumb wraps under the grip, fingers wrap around the near side.
        for(int i=0;i<4;i++) {
            Vector3f a=new Vector3f(grip).add(-c.handRadius*.6f,(i-1.5f)*c.handRadius*.35f,0);
            tube(pose,collector,a,new Vector3f(a).add(c.handRadius*1.5f,0,-c.handRadius*.5f),c.handRadius*.18f,0xffefc5a4,light);
        }
        tube(pose,collector,new Vector3f(grip).add(0,-c.handRadius,0),new Vector3f(grip).add(c.handRadius*.9f,0,-c.handRadius),c.handRadius*.23f,0xffefc5a4,light);
    }
    public static void gun(PoseStack pose, SubmitNodeCollector collector, int light,GunAnimation.Pose a) {
        var c=PortalVisualConfig.current;
        tube(pose,collector,new Vector3f(0,.06f,.08f),new Vector3f(0,.06f,-c.originalGunLength),c.originalGunRadius,0xffeceff2,light);
        tube(pose,collector,new Vector3f(0,.06f,-c.originalGunLength*.75f),new Vector3f(0,.06f,-c.originalGunLength*1.1f),c.originalGunRadius*.65f,0xff19232b,light);
        tube(pose,collector,new Vector3f(0,0,0),new Vector3f(0,.08f,-.02f),c.handRadius*.7f,0xff26323a,light);
        float tip=-c.originalGunLength*1.1f-a.opening()*c.emitterTravel;
        for(int i=0;i<3;i++) {
            double angle=i*Math.PI*2/3; float x=(float)Math.cos(angle)*c.originalGunRadius,y=.06f+(float)Math.sin(angle)*c.originalGunRadius;
            tube(pose,collector,new Vector3f(x,y,-c.originalGunLength*.7f),new Vector3f(x*(1+a.opening()),.06f+(y-.06f)*(1+a.opening()),tip),c.originalGunRadius*.1f,0xff1a252d,light);
        }
        if(c.gunGlow) tube(pose,collector,new Vector3f(0,.06f,tip),new Vector3f(0,.06f,tip-c.emitterTipOffset),c.emitterRadius,a.color(),net.minecraft.util.LightCoordsUtil.FULL_BRIGHT);
    }
}
