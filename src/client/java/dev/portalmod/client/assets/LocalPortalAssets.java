package dev.portalmod.client.assets;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.portalmod.PortalMod;
import dev.portalmod.assets.SourceModel;
import dev.portalmod.assets.SourceModelReader;
import dev.portalmod.assets.VpkArchive;
import dev.portalmod.assets.VtfImage;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
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
    private static final Map<String, Identifier> textures = new HashMap<>();
    private static long lastFire;
    private LocalPortalAssets() { }
    public static boolean gunReady() { return gun != null && config.portalGunModel; }
    public static boolean chellReady() { return chell != null && config.chellCharacter; }
    public static int loadedTriangles() { return (gun == null ? 0 : gun.triangleCount()) + (chell == null ? 0 : chell.triangleCount()); }
    public static void fired() { lastFire = System.nanoTime(); }
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
            gun = loadedGun; chell = loadedChell;
            PortalMod.LOGGER.info("Local Portal 2 assets loaded in memory: gun {} triangles, Chell {} triangles. No assets exported.", gun.triangleCount(), chell.triangleCount());
        } catch (Exception e) { PortalMod.LOGGER.warn("Portal 2 local models unavailable; using Minecraft/placeholder visuals: {}", e.toString()); }
    }
    private static void loadTexture(Minecraft client, VpkArchive archive, String material) throws Exception {
        String vmt = new String(archive.read("materials/" + material + ".vmt"), StandardCharsets.UTF_8);
        var match = Pattern.compile("(?i)\\$basetexture\\\"?\\s+\\\"?([^\\\"\\s]+)").matcher(vmt);
        VtfImage decoded;
        if (match.find()) decoded = VtfImage.decode(archive.read("materials/" + VpkArchive.normalize(match.group(1)) + ".vtf"), config.maxTextureDimension);
        else decoded = new VtfImage(1, 1, new int[]{0xffdddddd});
        NativeImage image = new NativeImage(decoded.width(), decoded.height(), false);
        for (int y = 0; y < decoded.height(); y++) for (int x = 0; x < decoded.width(); x++) image.setPixel(x, y, decoded.argb()[y * decoded.width() + x]);
        Identifier id = Identifier.fromNamespaceAndPath("portalmod", "local/" + material.replaceAll("[^a-z0-9/._-]", "_"));
        client.getTextureManager().register(id, new DynamicTexture(() -> "Portal 2 local " + material, image));
        textures.put(material, id);
    }
    public static void submitGun(PoseStack pose, SubmitNodeCollector collector, int light, float age, float swing) {
        pose.pushPose();
        float time = (System.nanoTime() - lastFire) / 1.0e9f;
        float kick = time >= 0 && time < config.fireDurationSeconds ? (float) Math.sin(time / config.fireDurationSeconds * Math.PI) : 0;
        pose.translate(config.gunX, config.gunY + Math.sin(age * 0.08f) * config.idleSway, config.gunZ + kick * config.fireRecoil);
        pose.rotateDegrees(Axis.XP, kick * -8);
        pose.scale(config.gunScale, config.gunScale, config.gunScale);
        pose.translate(0, -config.gunModelOriginY, 0);
        submit(gun, pose, collector, light, null);
        pose.popPose();
    }
    public static void submitChell(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        pose.pushPose();
        pose.rotateDegrees(Axis.YP, 180 - state.bodyRot);
        float scale = state.scale / config.unitsPerBlock;
        pose.scale(scale, scale, scale);
        if (state.isCrouching) pose.translate(0, -8, 0);
        Matrix4f[] skin = skinMatrices(chell, state);
        submit(chell, pose, collector, state.lightCoords, skin);
        pose.popPose();
    }
    private static Matrix4f[] skinMatrices(SourceModel model, AvatarRenderState state) {
        Matrix4f[] global = new Matrix4f[model.bones().length], result = new Matrix4f[global.length];
        float swing = (float) Math.sin(state.walkAnimationPos) * Math.min(1, state.walkAnimationSpeed) * (float) Math.toRadians(config.walkSwingDegrees);
        for (int i = 0; i < global.length; i++) {
            SourceModel.Bone b = model.bones()[i]; float[] p = b.localPosition(), q = b.localQuaternion();
            Matrix4f local = new Matrix4f().translation(p[0], p[1], p[2]).rotate(new Quaternionf(q[0], q[1], q[2], q[3]));
            // Original procedural animation on the user's locally loaded skeleton.
            global[i] = b.parent() < 0 ? local : new Matrix4f(global[b.parent()]).mul(local);
            Vector3f pivot = global[i].getTranslation(new Vector3f());
            Matrix4f adjustment = new Matrix4f().translation(pivot);
            boolean changed = false;
            if (b.name().startsWith("bicep_")) {
                adjustment.rotateZ((float) Math.toRadians(config.armRestDegrees) * (pivot.x > 0 ? -1 : 1)); changed = true;
            }
            if (b.name().equals("thigh_L") || b.name().equals("thigh_R")) {
                adjustment.rotateX(b.name().endsWith("_L") ? swing : -swing); changed = true;
            }
            if (b.name().equals("head")) { adjustment.rotateX((float) Math.toRadians(state.xRot)); changed = true; }
            if (changed) global[i] = adjustment.translate(-pivot.x, -pivot.y, -pivot.z).mul(global[i]);
            result[i] = new Matrix4f(global[i]).mul(matrix(b.inverseBind()));
        }
        return result;
    }
    private static Matrix4f matrix(float[] a) {
        return new Matrix4f(a[0], a[4], a[8], 0, a[1], a[5], a[9], 0, a[2], a[6], a[10], 0, a[3], a[7], a[11], 1);
    }
    private static void submit(SourceModel model, PoseStack pose, SubmitNodeCollector collector, int light, Matrix4f[] skin) {
        float[] vertices = new float[model.vertices().length * 8];
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
        for (SourceModel.Mesh mesh : model.meshes()) {
            if (mesh.material().contains("glass")) continue;
            Identifier texture = textures.get(mesh.material());
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (savedPose, buffer) -> {
                int[] indices = mesh.triangles();
                for (int t = 0; t < indices.length; t += 3) for (int corner : TRIANGLE_QUAD) {
                    int p = indices[t + corner] * 8;
                    buffer.addVertex(savedPose, vertices[p], vertices[p + 1], vertices[p + 2]).setColor(-1)
                            .setUv(vertices[p + 6], vertices[p + 7]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                            .setNormal(savedPose, vertices[p + 3], vertices[p + 4], vertices[p + 5]);
                }
            });
        }
    }
}
