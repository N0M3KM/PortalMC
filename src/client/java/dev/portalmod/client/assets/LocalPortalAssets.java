package dev.portalmod.client.assets;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.portalmod.PortalMod;
import dev.portalmod.assets.SourceModel;
import dev.portalmod.assets.SourceAnimations;
import dev.portalmod.assets.SourceMaterial;
import dev.portalmod.assets.LegMotion;
import dev.portalmod.assets.ArmaturePose;
import dev.portalmod.client.visual.OriginalGeometry;
import dev.portalmod.client.visual.CharacterAnimation;
import dev.portalmod.client.visual.GunAnimation;
import dev.portalmod.client.visual.OriginalCharacter;
import dev.portalmod.client.visual.PortalVisualConfig;
import dev.portalmod.assets.SourceModelReader;
import dev.portalmod.assets.VpkArchive;
import dev.portalmod.assets.VtfImage;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Client-only runtime imports. Valve data stays in memory and never enters assets/, builds or Git. */
public final class LocalPortalAssets {
    private static final int[] TRIANGLE_QUAD = {0, 2, 1, 1};
    public static LocalAssetConfig config = new LocalAssetConfig();
    private static SourceModel gun, chell;
    private static SourceAnimations chellAnimations;
    private static Vector3f gunGrip = new Vector3f(), gunEmitter = new Vector3f();
    private static final Map<String, Identifier> textures = new HashMap<>();
    private static final Map<String, SourceMaterial> materials = new HashMap<>();
    private record CacheKey(SourceModel model,int entity) { }
    private static final class CachedMesh {
        long poseFrame=-1,vertexFrame=-1; Matrix4f[] pose; float[] vertices;
    }
    private static final Map<CacheKey,CachedMesh> meshCache=new java.util.LinkedHashMap<>();
    public static void clearPoseCache() { meshCache.clear(); }
    private static CachedMesh cache(SourceModel model,int entity) {
        CacheKey key=new CacheKey(model,entity); CachedMesh found=meshCache.get(key);
        if(found==null) { if(meshCache.size()>=512) meshCache.remove(meshCache.keySet().iterator().next()); found=new CachedMesh(); meshCache.put(key,found); }
        return found;
    }
    private static Matrix4f[] poseCached(SourceModel model,int entity,java.util.function.Supplier<Matrix4f[]> posing) {
        var c=dev.portalmod.client.portal.PortalRenderer.config(); if(c==null || !c.cacheModelPoses) return posing.get();
        CachedMesh cached=cache(model,entity); long frame=dev.portalmod.client.portal.PortalRenderer.frameIndex;
        if(cached.poseFrame!=frame) { cached.pose=posing.get(); cached.poseFrame=frame; }
        return cached.pose;
    }
    private LocalPortalAssets() { }
    public static boolean gunReady() { return gun != null && config.portalGunModel; }
    public static boolean chellReady() { return (chell != null && config.chellCharacter) || PortalVisualConfig.current.originalCharacterFallback; }
    public static int loadedTriangles() { return (gun == null ? 0 : gun.triangleCount()) + (chell == null ? 0 : chell.triangleCount()); }
    public static void load(Minecraft client) {
        try {
            config = LocalAssetConfig.load();
            if (!config.enabled) return;
            VpkArchive archive = new VpkArchive(Path.of(config.portal2Directory).resolve("portal2/pak01_dir.vpk"));
            SourceModel loadedGun = SourceModelReader.read(archive, "models/weapons/v_portalgun.mdl");
            SourceModel loadedChell = SourceModelReader.read(archive, "models/player/chell/player.mdl");
            for (SourceModel model : new SourceModel[]{loadedGun, loadedChell}) for (SourceModel.Mesh mesh : model.meshes()) {
                if (!textures.containsKey(mesh.material())) loadTexture(client, archive, mesh.material());
            }
            // Optional authored colour skins; failure here does not disable either model.
            for (String variant : new String[]{"models/weapons/v_models/v_portalgun/v_portalgun_blue", "models/weapons/v_models/v_portalgun/v_portalgun_orange"})
                if (archive.contains("materials/"+variant+".vmt")) {
                    try { loadTexture(client,archive,variant); }
                    catch (Exception e) { PortalMod.LOGGER.warn("Optional local gun colour unavailable: {}",variant); }
                }
            gun = loadedGun; chell = loadedChell;
            // Chell delegates its animation library through the MDL include table.
            try {
                chellAnimations=SourceAnimations.read(archive,"models/player_animations.mdl",PortalVisualConfig.current.maxDecodedAnimationMegabytes*1024*1024);
                PortalMod.LOGGER.info("Local Chell authored animations loaded: {} sequences; no animation data exported",chellAnimations.sequenceCount());
            } catch(Exception e) { PortalMod.LOGGER.warn("Local Chell animation playback unavailable: {}",e.toString()); }
            int hand = ArmaturePose.bone(gun, "ValveBiped.Bip01_R_Hand");
            if (hand >= 0) gunGrip = ArmaturePose.bind(gun)[hand].getTranslation(new Vector3f());
            int emitter=ArmaturePose.bone(gun,"ValveBiped.Front_Cover_Stop");
            if(emitter>=0) gunEmitter=ArmaturePose.bind(gun)[emitter].getTranslation(new Vector3f());
            PortalMod.LOGGER.info("Local Portal 2 assets loaded in memory: gun {} triangles, Chell {} triangles. No assets exported.", gun.triangleCount(), chell.triangleCount());
        } catch (Exception e) { PortalMod.LOGGER.warn("Portal 2 local models unavailable; using Minecraft/placeholder visuals: {}", e.toString()); }
    }
    private static void loadTexture(Minecraft client, VpkArchive archive, String material) throws Exception {
        String vmt = new String(archive.read("materials/" + material + ".vmt"), StandardCharsets.UTF_8);
        SourceMaterial materialInfo = SourceMaterial.parse(vmt);
        materials.put(material,materialInfo);
        VtfImage decoded = materialInfo.texture() == null ? new VtfImage(1,1,new int[]{0xffdddddd})
            : VtfImage.decode(archive.read("materials/"+VpkArchive.normalize(materialInfo.texture())+".vtf"),config.maxTextureDimension);
        NativeImage image = new NativeImage(decoded.width(), decoded.height(), false);
        for (int y = 0; y < decoded.height(); y++) for (int x = 0; x < decoded.width(); x++) image.setPixel(x, y, materialInfo.pixel(decoded.argb()[y * decoded.width() + x]));
        Identifier id = Identifier.fromNamespaceAndPath("portalmod", "local/" + material.replaceAll("[^a-z0-9/._-]", "_"));
        client.getTextureManager().register(id, new DynamicTexture(() -> "Portal 2 local " + material, image));
        textures.put(material, id);
    }
    public static void submitGun(PoseStack pose, SubmitNodeCollector collector, int light, float age, float swing) {
        var c = PortalVisualConfig.current;
        if (config.gunFullBright) light = net.minecraft.util.LightCoordsUtil.FULL_BRIGHT;
        pose.pushPose();
        var animation=GunAnimation.sample();
        pose.translate(config.gunX+animation.bobX(),config.gunY+animation.sway()+animation.bobY(),config.gunZ+animation.kick()*config.fireRecoil);
        pose.rotateDegrees(Axis.XP,-animation.kick()*c.fireKickDegrees);
        pose.rotateDegrees(Axis.ZP,animation.fizzle()*c.fizzleShakeDegrees);
        submitHeldGun(pose, collector, light);
        pose.popPose();
    }
    public static void submitHeldGun(PoseStack pose, SubmitNodeCollector collector, int light) {
        var player=Minecraft.getInstance().player; submitHeldGun(pose,collector,light,player==null?-1:player.getId());
    }
    public static void submitHeldGun(PoseStack pose,SubmitNodeCollector collector,int light,int entityId) {
        var animation=GunAnimation.forEntity(entityId);
        if (!gunReady()) { OriginalGeometry.gun(pose,collector,light,animation); return; }
        pose.pushPose(); pose.scale(config.gunScale,config.gunScale,config.gunScale);
        // The gun grip is the authored wrist pivot, not the mesh origin or an arbitrary world offset.
        pose.translate(-gunGrip.x,-gunGrip.y,gunGrip.z);
        Matrix4f[] mechanism=poseCached(gun,entityId,() -> gunMechanism(animation));
        submit(gun,pose,collector,light,ArmaturePose.skin(gun,mechanism),entityId);
        pose.popPose();
    }
    public static void submitChell(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        var c=PortalVisualConfig.current;
        var entity=Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(state.id);
        boolean holding=entity instanceof net.minecraft.world.entity.player.Player player &&
            (player.getMainHandItem().is(dev.portalmod.PortalItems.PORTAL_GUN) || player.getOffhandItem().is(dev.portalmod.PortalItems.PORTAL_GUN));
        pose.pushPose(); pose.rotateDegrees(Axis.YP,180-state.bodyRot);
        float scale=state.scale/config.unitsPerBlock; pose.scale(scale,scale,scale);
        var animation=CharacterAnimation.pose(state.id,CharacterAnimation.partialTicks());
        if(!authoredAnimationReady()) pose.translate(0,-(c.characterAnimation?animation.crouch():(state.isCrouching?1:0))*c.crouchDropUnits-animation.land()*c.landingMaxDropUnits,0);
        if(chell==null || !config.chellCharacter) {
            OriginalCharacter.submit(state,animation,pose,collector,holding); pose.popPose(); return;
        }
        Matrix4f[] global=poseCached(chell,state.id,() -> posedBones(chell,state,holding));
        submit(chell,pose,collector,state.lightCoords,ArmaturePose.skin(chell,global),state.id);
        if(holding && c.thirdPersonGun) {
            int wrist=ArmaturePose.bone(chell,"wrist_R");
            if(wrist>=0) {
                Vector3f grip=global[wrist].getTranslation(new Vector3f());
                pose.pushPose(); pose.translate(grip.x,grip.y,-grip.z);
                pose.rotateDegrees(Axis.YP,-(c.characterAnimation?animation.yaw():Math.clamp(state.yRot,-c.headYawLimit,c.headYawLimit)));
                pose.rotateDegrees(Axis.XP,-Math.clamp(c.characterAnimation?animation.pitch():state.xRot,-c.aimPitchLimit,c.aimPitchLimit)+animation.recoil()*c.characterRecoilDegrees);
                pose.scale(config.unitsPerBlock,config.unitsPerBlock,config.unitsPerBlock);
                submitHeldGun(pose,collector,state.lightCoords,state.id); pose.popPose();
            }
        }
        pose.popPose();
    }
    private static Matrix4f[] posedBones(SourceModel model,AvatarRenderState state,boolean holding) {
        if(authoredAnimationReady()) return dev.portalmod.client.visual.AuthoredCharacterAnimation.pose(chellAnimations,model,state.id,holding);
        var c=PortalVisualConfig.current; Matrix4f[] global=ArmaturePose.bind(model);
        var a=CharacterAnimation.pose(state.id,CharacterAnimation.partialTicks());
        float swing=(float)Math.sin(a.stride())*a.speed()*(float)Math.toRadians(c.strideDegrees);
        if(!c.characterAnimation) swing=(float)Math.sin(state.walkAnimationPos)*Math.min(1,state.walkAnimationSpeed)*(float)Math.toRadians(config.walkSwingDegrees);
        if(!c.strideAnimation && c.characterAnimation) swing=0;
        ArmaturePose.rotate(model,global,"spine1",new Quaternionf().rotationXYZ((float)Math.toRadians(a.tilt()+a.crouch()*c.crouchBendDegrees),0,(float)Math.toRadians(a.lean()+a.idle())));
        Matrix4f[] rest=ArmaturePose.bind(model);
        float drop=(c.characterAnimation?a.crouch():(state.isCrouching?1:0))*c.crouchDropUnits+a.land()*c.landingMaxDropUnits;
        for(String side:new String[]{"R","L"}) {
            int ankle=ArmaturePose.bone(model,"ankle_"+side); if(ankle<0) continue;
            float sign=side.equals("R")?-1:1;
            var step=LegMotion.step(a.stride()+(side.equals("L")?(float)Math.PI:0),c.strideAnimation?a.speed():0,a.crouch(),c.footStrideUnits,c.footLiftUnits,c.crouchStanceUnits);
            Vector3f target=rest[ankle].getTranslation(new Vector3f()).add(sign*step.stance(),drop+step.lift(),step.forward());
            if(a.air()>0) target.add(0,a.air()*c.airFootLiftUnits,a.jump()*c.jumpFootForwardUnits);
            ArmaturePose.limb(model,global,"thigh_"+side,"knee_"+side,"ankle_"+side,target,new Vector3f(sign*.1f,0,1));
            ArmaturePose.upright(model,global,rest,"ankle_"+side);
        }
        float pitch=c.characterAnimation?a.pitch():Math.clamp(state.xRot,-c.headPitchLimit,c.headPitchLimit);
        float yaw=c.characterAnimation?a.yaw():Math.clamp(state.yRot,-c.headYawLimit,c.headYawLimit);
        ArmaturePose.rotate(model,global,"head",new Quaternionf().rotationYXZ((float)Math.toRadians(c.headFollow?-yaw:0),(float)Math.toRadians(c.headFollow?Math.clamp(pitch,-c.headPitchLimit,c.headPitchLimit):0),0));
        if(c.armPose) {
            float aimPitch=(float)Math.toRadians(-Math.clamp(pitch,-c.aimPitchLimit,c.aimPitchLimit)+a.recoil()*c.characterRecoilDegrees);
            float aimYaw=(float)Math.toRadians(yaw);
            Quaternionf aim=new Quaternionf().rotationYXZ(aimYaw,aimPitch,0);
            for(String side:new String[]{"R","L"}) {
                float sign=side.equals("R")?-1:1;
                Vector3f target=holding ? new Vector3f(side.equals("R")?c.gripX:c.supportGripX,side.equals("R")?c.gripY:c.supportGripY,side.equals("R")?c.gripZ:c.supportGripZ) : new Vector3f(sign*9,42,(side.equals("R")?-1:1)*swing*8);
                if(holding) target.sub(0,60,0).rotate(aim).add(0,60,0);
                ArmaturePose.arm(model,global,side,target,new Vector3f(sign*c.armPoleOut,c.armPoleDown,c.armPoleForward));
            }
        }
        return global;
    }
    private static boolean authoredAnimationReady() {
        var c=PortalVisualConfig.current;
        return chell!=null && config.chellCharacter && chellAnimations!=null && c.characterAnimation && c.authoredCharacterAnimations;
    }
    private static Matrix4f[] gunMechanism(GunAnimation.Pose animation) {
        Matrix4f[] mechanism=ArmaturePose.bind(gun); var c=PortalVisualConfig.current;
        if(c.gunProngs) {
            int cover=ArmaturePose.bone(gun,"ValveBiped.Front_Cover");
            if(cover>=0) ArmaturePose.translate(gun,mechanism,"ValveBiped.Front_Cover",new Vector3f(0,0,animation.opening()*c.emitterTravel/config.gunScale));
            for(String name:new String[]{"ValveBiped.Arm1_A","ValveBiped.Arm2_A","ValveBiped.Arm3_A"}) {
                int joint=ArmaturePose.bone(gun,name); if(joint<0) continue;
                Vector3f radial=mechanism[joint].getTranslation(new Vector3f()).sub(gunEmitter); radial.z=0;
                if(radial.lengthSquared()<1e-6f) radial.set(1,0,0); else radial.normalize();
                ArmaturePose.rotate(gun,mechanism,name,new Quaternionf().fromAxisAngleRad(radial.y,-radial.x,0,(float)Math.toRadians(animation.opening()*c.prongOpenDegrees)));
            }
        }
        return mechanism;
    }
    private static void submit(SourceModel model, PoseStack pose, SubmitNodeCollector collector, int light, Matrix4f[] skin,int entityId) {
        var settings=dev.portalmod.client.portal.PortalRenderer.config();
        boolean caching=settings!=null && settings.cacheModelPoses;
        CachedMesh cached=cache(model,entityId); long frame=dev.portalmod.client.portal.PortalRenderer.frameIndex;
        if(cached.vertices==null) cached.vertices=new float[model.vertices().length*8];
        float[] vertices=caching?cached.vertices:new float[model.vertices().length*8];
        if(!caching || cached.vertexFrame!=frame) {
        Vector3f point = new Vector3f(), normal = new Vector3f(), temp = new Vector3f();
        for (int i = 0; i < model.vertices().length; i++) {
            SourceModel.Vertex v = model.vertices()[i]; point.set(v.x(), v.y(), v.z()); normal.set(v.nx(), v.ny(), v.nz());
            if (skin != null) {
                point.zero(); normal.zero();
                for (int j = 0; j < 3; j++) if (v.weights()[j] > 0 && v.bones()[j] < skin.length) {
                    skin[v.bones()[j]].transformPosition(temp.set(v.x(), v.y(), v.z())); point.fma(v.weights()[j], temp);
                    skin[v.bones()[j]].transformDirection(temp.set(v.nx(), v.ny(), v.nz())); normal.fma(v.weights()[j], temp);
                }
                normal.normalize();
            }
            int p = i * 8; // These Portal 2 meshes have an authored Y-up bind pose; facing maps to -Z.
            vertices[p] = point.x; vertices[p + 1] = point.y; vertices[p + 2] = -point.z;
            vertices[p + 3] = normal.x; vertices[p + 4] = normal.y; vertices[p + 5] = -normal.z;
            vertices[p + 6] = v.u(); vertices[p + 7] = v.v();
        }
        cached.vertexFrame=caching?frame:-1;
        }
        for (SourceModel.Mesh mesh : model.meshes()) {
            String materialName=mesh.material();
            var materialInfo=materials.get(materialName);
            boolean glass=materialName.contains("portalgun_glass");
            int tint=-1;
            if(model==gun && PortalVisualConfig.current.gunGlow) {
                var animation=GunAnimation.forEntity(entityId);
                if(glass) tint=animation.color() & 0xffffff | (int)(PortalVisualConfig.current.gunGlassOpacity*255)<<24;
                else {
                    String variant="models/weapons/v_models/v_portalgun/v_portalgun_"+(((animation.color()>>16)&255)>((animation.color())&255)?"orange":"blue");
                    if(textures.containsKey(variant)) materialName=variant;
                }
            }
            Identifier texture=textures.get(materialName);
            var renderType=glass && PortalVisualConfig.current.gunGlow?RenderTypes.entityTranslucentEmissive(texture)
                : materialInfo!=null && materialInfo.translucent()?RenderTypes.entityTranslucent(texture):RenderTypes.entityCutout(texture);
            final int color=tint;
            collector.submitCustomGeometry(pose, renderType, (savedPose, buffer) -> {
                int[] indices = mesh.triangles();
                for (int t = 0; t < indices.length; t += 3) for (int corner : TRIANGLE_QUAD) {
                    int p = indices[t + corner] * 8;
                    buffer.addVertex(savedPose, vertices[p], vertices[p + 1], vertices[p + 2]).setColor(color)
                            .setUv(vertices[p + 6], vertices[p + 7]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                            .setNormal(savedPose, vertices[p + 3], vertices[p + 4], vertices[p + 5]);
                }
            });
        }
    }
}
