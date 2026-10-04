package dev.portalmod.client.portal;

import dev.portalmod.config.ConfigManager;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;

/** Bounded client rendering work; independent of authoritative portal geometry. */
public final class PortalViewConfig {
    public boolean enabled = true;
    public int recursionDepth = 1;
    public int maxRenderPasses = 2;
    public double resolutionScale = 0.35;
    public int maxResolution = 768;
    public int sceneRadius = 12;
    public int maxSceneBlocks = 4096;
    public int maxSceneEntities = 32;
    public int maxSceneBlockEntities = 64;
    public int maxSceneFluidBlocks = 256;
    public int sceneRefreshTicks = 10;
    public double maxViewDistance = 128;
    public double surfaceOffset = 0.002;
    public double rimWidth = 0.035;
    public boolean ovalSurface = true, animatedRim = true, portalOpenAnimation = true, portalCloseAnimation = true;
    public boolean ambientWisps = true, openParticles = true, closeParticles = true, fizzleParticles = true;
    // Estimated request/reference proportion: no accessible primary source verified 64x112.
    // Uniformly fit this aspect inside the existing 1x2 collision contract; never change traversal.
    public double referenceWidthUnits = 64, referenceHeightUnits = 112;
    public int ovalSegments = 48, particleCap = 128, particleBurst = 16;
    public double openSeconds = .22, closeSeconds = .18, rimPulseFrequency = 6, rimPulseStrength = .2;
    public double particleDensity = .7, particleLifeSeconds = .45, particleSize = .035, particleSpeed = .3;
    public boolean adaptiveRecursion = true, frustumCulling = true, sceneFrustumCulling = true, cacheModelPoses = true;
    public double frameBudgetMs = 20, portalBudgetMs = 4;
    public int overloadFrames = 3, recoveryFrames = 120, sceneVoxelBudget = 2048;
    public static PortalViewConfig load() {
        var path = FabricLoader.getInstance().getConfigDir().resolve("portalmod-portals-client.json");
        try {
            PortalViewConfig c = Files.exists(path) ? ConfigManager.JSON.fromJson(Files.readString(path), PortalViewConfig.class) : new PortalViewConfig();
            if (c == null) c = new PortalViewConfig();
            c.recursionDepth = Math.clamp(c.recursionDepth, 0, 4);
            c.maxRenderPasses = Math.clamp(c.maxRenderPasses, 1, 16);
            c.resolutionScale = Double.isFinite(c.resolutionScale) ? Math.clamp(c.resolutionScale, 0.1, 1) : 0.5;
            c.maxResolution = Math.clamp(c.maxResolution, 128, 2048);
            c.sceneRadius = Math.clamp(c.sceneRadius, 4, 32);
            c.maxSceneBlocks = Math.clamp(c.maxSceneBlocks, 128, 16384);
            c.maxSceneEntities = Math.clamp(c.maxSceneEntities, 0, 256);
            c.maxSceneBlockEntities = Math.clamp(c.maxSceneBlockEntities, 0, 256);
            c.maxSceneFluidBlocks = Math.clamp(c.maxSceneFluidBlocks, 0, 2048);
            c.sceneRefreshTicks = Math.clamp(c.sceneRefreshTicks, 1, 100);
            c.maxViewDistance = Double.isFinite(c.maxViewDistance) ? Math.clamp(c.maxViewDistance, 8, 256) : 128;
            c.surfaceOffset = Double.isFinite(c.surfaceOffset) ? Math.clamp(c.surfaceOffset, 0.001, 0.02) : 0.002;
            c.rimWidth = Double.isFinite(c.rimWidth) ? Math.clamp(c.rimWidth, 0.01, 0.1) : 0.035;
            c.referenceWidthUnits = Double.isFinite(c.referenceWidthUnits)?Math.clamp(c.referenceWidthUnits,16,128):64;
            c.referenceHeightUnits = Double.isFinite(c.referenceHeightUnits)?Math.clamp(c.referenceHeightUnits,32,224):112;
            c.ovalSegments = Math.clamp(c.ovalSegments,16,96); c.particleCap = Math.clamp(c.particleCap,0,512); c.particleBurst = Math.clamp(c.particleBurst,0,64);
            c.openSeconds = finite(c.openSeconds,.22,.05,2); c.closeSeconds = finite(c.closeSeconds,.18,.05,2);
            c.rimPulseFrequency=finite(c.rimPulseFrequency,6,0,30); c.rimPulseStrength=finite(c.rimPulseStrength,.2,0,1);
            c.particleDensity=finite(c.particleDensity,.7,0,4); c.particleLifeSeconds=finite(c.particleLifeSeconds,.45,.05,2);
            c.particleSize=finite(c.particleSize,.035,.005,.15); c.particleSpeed=finite(c.particleSpeed,.3,0,4);
            c.frameBudgetMs=finite(c.frameBudgetMs,20,8,100); c.portalBudgetMs=finite(c.portalBudgetMs,4,.5,50);
            c.overloadFrames=Math.clamp(c.overloadFrames,1,120); c.recoveryFrames=Math.clamp(c.recoveryFrames,30,600);
            c.sceneVoxelBudget=Math.clamp(c.sceneVoxelBudget,128,8192);
            Files.createDirectories(path.getParent()); Files.writeString(path, ConfigManager.JSON.toJson(c));
            return c;
        } catch (Exception e) { throw new IllegalStateException("Cannot load portal view config", e); }
    }
    private static double finite(double value,double fallback,double min,double max) { return Double.isFinite(value)?Math.clamp(value,min,max):fallback; }
    public void save() {
        var path=FabricLoader.getInstance().getConfigDir().resolve("portalmod-portals-client.json");
        try { Files.createDirectories(path.getParent()); Files.writeString(path,ConfigManager.JSON.toJson(this)); }
        catch(Exception e) { throw new IllegalStateException("Cannot save portal view settings",e); }
    }
}
