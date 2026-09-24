package com.example.moveclient.config;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleManager;
import com.example.moveclient.module.Setting;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and saves every module's enabled state and settings as a single JSON file under the
 * game's config directory. Storage is a flat {@code {"ModuleName": {"enabled": bool, "settings": {...}}}}
 * structure rather than a reflected POJO, so adding a setting to a module never requires touching
 * this class.
 */
public final class ConfigManager {

    private static final com.google.gson.Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "moveclient.json";

    private ConfigManager() {
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return;
        }

        JsonObject root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return;
            }
            root = parsed.getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            System.err.println("[moveclient] Failed to read " + FILE_NAME + ", keeping defaults: " + e);
            return;
        }

        for (Module module : ModuleManager.getInstance().getModules()) {
            if (!root.has(module.getName())) {
                continue;
            }
            JsonObject moduleObj = root.getAsJsonObject(module.getName());

            if (moduleObj.has("enabled")) {
                module.setEnabled(moduleObj.get("enabled").getAsBoolean());
            }

            if (moduleObj.has("settings")) {
                JsonObject settingsObj = moduleObj.getAsJsonObject("settings");
                for (Setting<?> setting : module.getSettings()) {
                    if (!settingsObj.has(setting.getName())) {
                        continue;
                    }
                    JsonElement raw = settingsObj.get(setting.getName());
                    if (setting instanceof Setting.BooleanSetting booleanSetting) {
                        booleanSetting.set(raw.getAsBoolean());
                    } else if (setting instanceof Setting.DoubleSetting doubleSetting) {
                        doubleSetting.set(raw.getAsDouble());
                    }
                }
            }
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();

        for (Module module : ModuleManager.getInstance().getModules()) {
            JsonObject moduleObj = new JsonObject();
            moduleObj.addProperty("enabled", module.isEnabled());

            JsonObject settingsObj = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                Object value = setting.get();
                if (value instanceof Boolean b) {
                    settingsObj.addProperty(setting.getName(), b);
                } else if (value instanceof Double d) {
                    settingsObj.addProperty(setting.getName(), d);
                }
            }
            moduleObj.add("settings", settingsObj);

            root.add(module.getName(), moduleObj);
        }

        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            System.err.println("[moveclient] Failed to write " + FILE_NAME + ": " + e);
        }
    }
}
