package dev.portalmod.client.portal;

import dev.portalmod.config.ConfigManager;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;

/** Bounded client rendering work; independent of authoritative portal geometry. */
public final class PortalViewConfig {
    public boolean enabled = true;
    public int recursionDepth = 2;
    public int maxRenderPasses = 6;
    public double resolutionScale = 0.5;
    public int maxResolution = 1024;
    public int sceneRadius = 16;
    public int maxSceneBlocks = 4096;
    public int maxSceneEntities = 128;
    public int maxSceneBlockEntities = 64;
    public int maxSceneFluidBlocks = 256;
    public int sceneRefreshTicks = 10;
    public double maxViewDistance = 128;
    public double surfaceOffset = 0.002;
    public double rimWidth = 0.035;
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
            Files.createDirectories(path.getParent()); Files.writeString(path, ConfigManager.JSON.toJson(c));
            return c;
        } catch (Exception e) { throw new IllegalStateException("Cannot load portal view config", e); }
    }
}
