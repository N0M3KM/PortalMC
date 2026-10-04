package dev.portalmod.portal;

import dev.portalmod.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PortalConfig {
    public boolean enabled = true;
    public double shotRange = 64;
    public int shotCooldownTicks = 4;
    public int maxPairs = 64;
    public int chunkRadius = 1;
    public int chunkRefreshTicks = 40;
    public double exitEpsilon = 0.025;
    public double collisionMargin = 0.1;
    public int maxCrossingsPerTick = 4;
    public int fizzleParticles = 8;
    public double fizzleSpread = 0.08;
    public double fizzleSpeed = 0.02;
    public double fizzleMissDistance = 3;
    public int maxRemoteChunks = 128;
    public double remoteViewDistance = 128;
    public double entityCaptureRadius = 2;
    public int entityRefreshTicks = 10;
    private static PortalConfig current = new PortalConfig();
    public static PortalConfig get() { return current; }
    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("portalmod-portals.json");
        try {
            PortalConfig c = Files.exists(path) ? ConfigManager.JSON.fromJson(Files.readString(path), PortalConfig.class) : new PortalConfig();
            c.validate(); current = c;
            if (!Files.exists(path)) { Files.createDirectories(path.getParent()); Files.writeString(path, ConfigManager.JSON.toJson(c) + "\n"); }
        } catch (Exception e) { throw new IllegalStateException("Cannot load " + path, e); }
    }
    public void validate() {
        if (!Double.isFinite(shotRange) || shotRange < 1 || shotRange > 256 || shotCooldownTicks < 1 || shotCooldownTicks > 100
                || maxPairs < 1 || maxPairs > 128 || chunkRadius < 1 || chunkRadius > 4 || chunkRefreshTicks < 10 || chunkRefreshTicks > 200
                || !Double.isFinite(exitEpsilon) || exitEpsilon < 0.001 || exitEpsilon > 0.2
                || !Double.isFinite(collisionMargin) || collisionMargin < 0 || collisionMargin > 0.5
                || maxCrossingsPerTick < 1 || maxCrossingsPerTick > 8 || fizzleParticles < 1 || fizzleParticles > 64
                || maxRemoteChunks < 9 || maxRemoteChunks > 512 || !Double.isFinite(remoteViewDistance) || remoteViewDistance < 8 || remoteViewDistance > 256
                || !Double.isFinite(entityCaptureRadius) || entityCaptureRadius < 2 || entityCaptureRadius > 32
                || entityRefreshTicks < 1 || entityRefreshTicks > 100)
            throw new IllegalArgumentException("Invalid portal settings");
        if (!Double.isFinite(fizzleSpread) || fizzleSpread < 0 || fizzleSpread > 1
                || !Double.isFinite(fizzleSpeed) || fizzleSpeed < 0 || fizzleSpeed > 1
                || !Double.isFinite(fizzleMissDistance) || fizzleMissDistance < 0 || fizzleMissDistance > shotRange)
            throw new IllegalArgumentException("Invalid fizzle settings");
    }
}
