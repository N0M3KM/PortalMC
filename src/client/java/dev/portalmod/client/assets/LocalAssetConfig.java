package dev.portalmod.client.assets;

import dev.portalmod.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LocalAssetConfig {
    public boolean enabled = true;
    public String portal2Directory = "D:\\SteamLibrary\\steamapps\\common\\Portal 2";
    public boolean chellCharacter = true;
    public boolean portalGunModel = true;
    public int maxTextureDimension = 1024;
    public float unitsPerBlock = 40;
    public float gunScale = 0.025f;
    public float gunX = 0.45f;
    public float gunY = -0.25f;
    public float gunZ = -0.45f;
    public float gunModelOriginY = 48;
    public float idleSway = 0.005f;
    public float fireRecoil = 0.06f;
    public float fireDurationSeconds = 0.25f;
    public float walkSwingDegrees = 25;
    public float armRestDegrees = 65;
    public static LocalAssetConfig load() throws Exception {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("portalmod-client.json");
        LocalAssetConfig c = Files.exists(path) ? ConfigManager.JSON.fromJson(Files.readString(path), LocalAssetConfig.class) : new LocalAssetConfig();
        if (c == null || c.portal2Directory == null || c.maxTextureDimension < 64 || c.maxTextureDimension > 2048
                || !Float.isFinite(c.unitsPerBlock) || c.unitsPerBlock < 1
                || !Float.isFinite(c.gunScale) || c.gunScale <= 0 || c.gunScale > 1
                || !Float.isFinite(c.gunX) || !Float.isFinite(c.gunY) || !Float.isFinite(c.gunZ) || !Float.isFinite(c.gunModelOriginY)
                || !Float.isFinite(c.idleSway) || c.idleSway < 0 || c.idleSway > 0.1
                || !Float.isFinite(c.fireRecoil) || c.fireRecoil < 0 || c.fireRecoil > 1
                || !Float.isFinite(c.fireDurationSeconds) || c.fireDurationSeconds <= 0 || c.fireDurationSeconds > 5
                || !Float.isFinite(c.walkSwingDegrees) || c.walkSwingDegrees < 0 || c.walkSwingDegrees > 90
                || !Float.isFinite(c.armRestDegrees) || c.armRestDegrees < 0 || c.armRestDegrees > 90)
            throw new IllegalArgumentException("Invalid local model settings in " + path);
        if (!Files.exists(path)) { Files.createDirectories(path.getParent()); Files.writeString(path, ConfigManager.JSON.toJson(c) + "\n"); }
        return c;
    }
}
