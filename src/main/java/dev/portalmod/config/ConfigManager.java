package dev.portalmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static MovementConfig serverConfig = new MovementConfig();

    private ConfigManager() { }

    public static MovementConfig server() { return serverConfig; }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("portalmod.json");
    }

    public static void load() {
        Path path = path();
        try {
            MovementConfig loaded = Files.exists(path)
                    ? JSON.fromJson(Files.readString(path), MovementConfig.class) : new MovementConfig();
            if (loaded == null) throw new IllegalArgumentException("Config must be a JSON object");
            loaded.validate();
            serverConfig = loaded;
            if (!Files.exists(path)) save();
        } catch (IOException | RuntimeException e) {
            // Preserve invalid files; never quietly change authoritative physics.
            throw new IllegalStateException("Cannot load " + path + ": " + e.getMessage(), e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), JSON.toJson(serverConfig) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save " + path(), e);
        }
    }
}
