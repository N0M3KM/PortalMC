package dev.portalmod.assets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Independently implemented MDL49 frame-animation reader; files and decoded poses remain in memory.
 * Format references: Valve source-sdk-2013/src/public/studio.h and compressed_vector.h;
 * MDL49 frame layout confirmed against SourceIO/library/models/mdl/structs/{frame_anim,local_animation}.py.
 * No Source code, engine or model/animation bytes are shipped with this implementation. */
public final class SourceAnimations {
    public record Bone(String name, int parent, Vector3f position, Quaternionf rotation) { }
    public record Clip(String name, float fps, int flags, int frames, float travelUnits, float[] data) { }
    public record Sequence(String name, int flags, int[] clips, int width, int height,
                           float startX,float startY,float endX,float endY,float[] weights) { }
    public static final class Pose {
        public final Vector3f[] position;
        public final Quaternionf[] rotation;
        Pose(int n) { position=new Vector3f[n]; rotation=new Quaternionf[n]; for(int i=0;i<n;i++) { position[i]=new Vector3f(); rotation[i]=new Quaternionf(); } }
    }
    private final Bone[] bones;
    private final Clip[] clips;
    private final Map<String,Sequence> sequences;
    private SourceAnimations(Bone[] bones,Clip[] clips,Map<String,Sequence> sequences) { this.bones=bones; this.clips=clips; this.sequences=Map.copyOf(sequences); }
    public int sequenceCount() { return sequences.size(); }
    public boolean has(String name) { return sequences.containsKey(name); }
    public Set<String> names() { return sequences.keySet(); }
    public float duration(String sequence) {
        Clip c=clips[sequences.get(sequence).clips[0]]; return Math.max(1,c.frames-1)/c.fps;
    }
    public float travel(String sequence) {
        Sequence s=sequences.get(sequence); float max=0;
        for(int id:s.clips) max=Math.max(max,clips[id].travelUnits); return max;
    }
    public static SourceAnimations read(VpkArchive archive,String path,int maxDecodedBytes) throws IOException {
        try {
            ByteBuffer mdl=buffer(archive.read(path));
            int blockCount=limit(mdl.getInt(352),4096), blockStart=mdl.getInt(356);
            ByteBuffer ani=null;
            if(blockCount>1) {
                String name=string(mdl,mdl.getInt(348));
                if(!archive.contains(name)) name=path.substring(0,path.length()-4)+".ani";
                ani=buffer(archive.read(name));
            }
            return decode(mdl,ani,maxDecodedBytes);
        } catch(RuntimeException e) { throw new IOException("Unsupported/malformed local animation library "+path,e); }
    }
    static SourceAnimations decode(ByteBuffer mdl,ByteBuffer ani,int maxBytes) {
        if(mdl.getInt(0)!=0x54534449 || mdl.getInt(4)!=49) throw new IllegalArgumentException("Expected MDL49");
        int count=limit(mdl.getInt(156),256),boneStart=mdl.getInt(160); Bone[] bones=new Bone[count];
        for(int i=0;i<count;i++) {
            int b=boneStart+i*216;
            bones[i]=new Bone(string(mdl,b+mdl.getInt(b)),mdl.getInt(b+4),vec(mdl,b+32),new Quaternionf(mdl.getFloat(b+44),mdl.getFloat(b+48),mdl.getFloat(b+52),mdl.getFloat(b+56)).normalize());
            if(bones[i].parent>=i || bones[i].parent<-1) throw new IllegalArgumentException("Bone hierarchy");
        }
        int n=limit(mdl.getInt(180),4096),start=mdl.getInt(184),blockStart=mdl.getInt(356); Clip[] clips=new Clip[n]; long used=0;
        for(int i=0;i<n;i++) {
            int a=start+i*100,flags=mdl.getInt(a+12),frames=limit(mdl.getInt(a+16),10000);
            if(frames<1 || (flags&64)==0) throw new IllegalArgumentException("Only MDL49 frame animations supported");
            used+=(long)frames*count*7*4; if(used>maxBytes) throw new IllegalArgumentException("Decoded animation memory cap");
            float fps=mdl.getFloat(a+8); if(!Float.isFinite(fps) || fps<=0) throw new IllegalArgumentException("Animation FPS");
            float[] data=new float[frames*count*7];
            for(int frame=0;frame<frames;frame++) {
                int block=mdl.getInt(a+52),index=mdl.getInt(a+56),localFrame=frame;
                int sectionFrames=mdl.getInt(a+84);
                if(sectionFrames>0) {
                    int section=frame/sectionFrames;
                    if(frames>sectionFrames && frame==frames-1) { section=frames/sectionFrames+1; localFrame=0; }
                    else localFrame=frame%sectionFrames;
                    int sectionAt=a+mdl.getInt(a+80)+section*8;
                    block=mdl.getInt(sectionAt); index=mdl.getInt(sectionAt+4);
                }
                ByteBuffer source=block==0?mdl:Objects.requireNonNull(ani,"Missing ANI");
                if(block<0 || block>=mdl.getInt(352)) throw new IllegalArgumentException("Animation block index");
                int at=block==0?a+index:mdl.getInt(blockStart+block*8)+index;
                int constants=source.getInt(at),frameOffset=source.getInt(at+4),frameLength=source.getInt(at+8);
                int cp=at+constants,fp=at+frameOffset+localFrame*frameLength;
                for(int bone=0;bone<count;bone++) {
                    int bits=Byte.toUnsignedInt(source.get(at+24+bone));
                    // The local Portal 2 library uses only half-vector/Quaternion48 channels.
                    if((bits&0xc0)!=0) throw new IllegalArgumentException("Quaternion48S channels unsupported");
                    Vector3f position=(flags&4)==0?new Vector3f(bones[bone].position):new Vector3f();
                    Quaternionf rotation=(flags&4)==0?new Quaternionf(bones[bone].rotation):new Quaternionf();
                    if((bits&2)!=0) { rotation=quaternion48(source,cp); cp+=6; }
                    if((bits&1)!=0) { position=halfVector(source,cp); cp+=6; }
                    if((bits&32)!=0) { position=vec(source,cp); cp+=12; }
                    if((bits&8)!=0) { rotation=quaternion48(source,fp); fp+=6; }
                    if((bits&4)!=0) { position=halfVector(source,fp); fp+=6; }
                    if((bits&16)!=0) { position=vec(source,fp); fp+=12; }
                    if(!position.isFinite() || !rotation.isFinite()) throw new IllegalArgumentException("Non-finite animation channel");
                    int p=(frame*count+bone)*7;
                    data[p]=position.x;data[p+1]=position.y;data[p+2]=position.z;
                    data[p+3]=rotation.x;data[p+4]=rotation.y;data[p+5]=rotation.z;data[p+6]=rotation.w;
                }
                if(frameLength>0 && fp!=at+frameOffset+(localFrame+1)*frameLength) throw new IllegalArgumentException("Frame stride mismatch");
            }
            int movements=limit(mdl.getInt(a+20),10000); float travel=0;
            if(movements>0) { int last=a+mdl.getInt(a+24)+(movements-1)*44; travel=(float)Math.hypot(mdl.getFloat(last+32),mdl.getFloat(last+36)); }
            clips[i]=new Clip(string(mdl,a+mdl.getInt(a+4)),fps,flags,frames,travel,data);
        }
        Map<String,Sequence> sequences=new HashMap<>(); int seqs=limit(mdl.getInt(188),4096),seqStart=mdl.getInt(192);
        for(int i=0;i<seqs;i++) {
            int q=seqStart+i*212,nx=limit(mdl.getInt(q+68),32),ny=limit(mdl.getInt(q+72),32); int[] ids=new int[nx*ny];
            for(int j=0;j<ids.length;j++) { ids[j]=Short.toUnsignedInt(mdl.getShort(q+mdl.getInt(q+60)+j*2)); if(ids[j]>=n) throw new IllegalArgumentException("Sequence clip index"); }
            float[] weights=new float[count]; int w=q+mdl.getInt(q+156);
            for(int j=0;j<count;j++) weights[j]=Math.clamp(mdl.getFloat(w+j*4),0,1);
            String name=string(mdl,q+mdl.getInt(q+4));
            sequences.put(name,new Sequence(name,mdl.getInt(q+12),ids,nx,ny,mdl.getFloat(q+84),mdl.getFloat(q+88),mdl.getFloat(q+92),mdl.getFloat(q+96),weights));
        }
        return new SourceAnimations(bones,clips,sequences);
    }
    public Pose sample(String name,double cycle,float x,float y) {
        Sequence s=Objects.requireNonNull(sequences.get(name),name);
        float gx=grid(x,s.startX,s.endX,s.width),gy=grid(y,s.startY,s.endY,s.height);
        int ix=(int)gx,iy=(int)gy,jx=Math.min(ix+1,s.width-1),jy=Math.min(iy+1,s.height-1);
        Pose a=blend(sample(clips[s.clips[iy*s.width+ix]],cycle),sample(clips[s.clips[iy*s.width+jx]],cycle),gx-ix);
        Pose b=blend(sample(clips[s.clips[jy*s.width+ix]],cycle),sample(clips[s.clips[jy*s.width+jx]],cycle),gx-ix);
        return blend(a,b,gy-iy);
    }
    private Pose sample(Clip clip,double cycle) {
        double normalized=(clip.flags&1)!=0?cycle-Math.floor(cycle):Math.clamp(cycle,0,1);
        double f=normalized*(clip.frames-1); int a=(int)f,b=Math.min(a+1,clip.frames-1); float fraction=(float)(f-a);
        Pose pose=new Pose(bones.length);
        for(int i=0;i<bones.length;i++) {
            int p=(a*bones.length+i)*7,q=(b*bones.length+i)*7;
            pose.position[i].set(clip.data[p],clip.data[p+1],clip.data[p+2]).lerp(new Vector3f(clip.data[q],clip.data[q+1],clip.data[q+2]),fraction);
            pose.rotation[i].set(clip.data[p+3],clip.data[p+4],clip.data[p+5],clip.data[p+6]).slerp(new Quaternionf(clip.data[q+3],clip.data[q+4],clip.data[q+5],clip.data[q+6]),fraction);
        }
        return pose;
    }
    public static Pose blend(Pose a,Pose b,float fraction) {
        float t=Math.clamp(fraction,0,1); Pose out=new Pose(a.position.length);
        for(int i=0;i<out.position.length;i++) { out.position[i].set(a.position[i]).lerp(b.position[i],t);out.rotation[i].set(a.rotation[i]).slerp(b.rotation[i],t); }
        return out;
    }
    public Pose layer(Pose base,String name,double cycle,float x,float y,float amount) {
        Sequence seq=sequences.get(name); if(seq==null || amount<=0) return base;
        Pose add=sample(name,cycle,x,y),out=blend(base,base,0);
        for(int i=0;i<bones.length;i++) {
            float t=Math.clamp(amount*seq.weights[i],0,1);
            if((seq.flags&4)!=0) { out.position[i].fma(t,add.position[i]);out.rotation[i].mul(new Quaternionf().slerp(add.rotation[i],t)).normalize(); }
            else { out.position[i].lerp(add.position[i],t);out.rotation[i].slerp(add.rotation[i],t); }
        }
        return out;
    }
    public Matrix4f[] modelPose(SourceModel target,Pose sampled) {
        Matrix4f[] rest=ArmaturePose.bind(target),out=new Matrix4f[rest.length];
        Map<String,Integer> lookup=new HashMap<>();for(int i=0;i<bones.length;i++) lookup.put(bones[i].name,i);
        for(int i=0;i<out.length;i++) {
            int parent=target.bones()[i].parent();
            Matrix4f local=parent<0?new Matrix4f(rest[i]):new Matrix4f(rest[parent]).invert().mul(rest[i]);
            Integer source=lookup.get(target.bones()[i].name());
            if(source!=null) {
                Bone b=bones[source];
                local.mul(new Matrix4f().translationRotate(b.position,b.rotation).invert())
                    .mul(new Matrix4f().translationRotate(sampled.position[source],sampled.rotation[source]));
            }
            out[i]=parent<0?local:new Matrix4f(out[parent]).mul(local);
        }
        return out;
    }
    /** Portal 2 animations use X-forward/Y-left/Z-up; the existing imported mesh path uses Y-up/Z-forward.
     * (x,y,z) -> (y,z,x), followed by the renderer's existing Z reflection. */
    public Matrix4f[] portalMeshPose(SourceModel target,Pose sampled) {
        Matrix4f[] result=modelPose(target,sampled);Matrix4f basis=sourceToMesh();
        for(int i=0;i<result.length;i++) result[i]=new Matrix4f(basis).mul(result[i]);
        return result;
    }
    static Matrix4f sourceToMesh() { return new Matrix4f().rotationY(-(float)Math.PI/2).rotateX(-(float)Math.PI/2); }
    private static float grid(float value,float start,float end,int n) { return n<=1 || start==end?0:Math.clamp((value-start)/(end-start),0,1)*(n-1); }
    static Quaternionf quaternion48(ByteBuffer b,int p) {
        float x=(Short.toUnsignedInt(b.getShort(p))-32768)/32768f,y=(Short.toUnsignedInt(b.getShort(p+2))-32768)/32768f;
        int packed=Short.toUnsignedInt(b.getShort(p+4));float z=((packed&32767)-16384)/16384f,w=(float)Math.sqrt(Math.max(0,1-x*x-y*y-z*z));
        return new Quaternionf(x,y,z,(packed&32768)!=0?-w:w).normalize();
    }
    private static Vector3f halfVector(ByteBuffer b,int p) { return new Vector3f(half(b.getShort(p)),half(b.getShort(p+2)),half(b.getShort(p+4))); }
    static float half(short bits) {
        int h=Short.toUnsignedInt(bits),sign=(h&0x8000)<<16,exponent=(h>>>10)&31,mantissa=h&1023;
        if(exponent==0) return (sign==0?1:-1)*Math.scalb((float)mantissa,-24);
        return Float.intBitsToFloat(sign|((exponent==31?255:exponent+112)<<23)|(mantissa<<13));
    }
    private static Vector3f vec(ByteBuffer b,int p) { return new Vector3f(b.getFloat(p),b.getFloat(p+4),b.getFloat(p+8)); }
    private static ByteBuffer buffer(byte[] data) { return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN); }
    private static String string(ByteBuffer b,int p) { int e=p;while(b.get(e)!=0)e++;return new String(b.array(),p,e-p,java.nio.charset.StandardCharsets.UTF_8); }
    private static int limit(int n,int max) { if(n<0 || n>max) throw new IllegalArgumentException("Animation count limit");return n; }
}
