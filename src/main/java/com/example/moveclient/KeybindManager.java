package com.example.moveclient;

import com.example.moveclient.config.ConfigManager;
import com.example.moveclient.gui.ClickGuiScreen;
import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Registers and dispatches every keybind owned by the mod: the ClickGUI toggle plus one per module. */
public final class KeybindManager {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("moveclient", "general"));

    private KeyMapping openGuiKey;
    private final Map<Module, KeyMapping> moduleKeys = new LinkedHashMap<>();

    public void registerAll() {
        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.moveclient.opengui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_SEMICOLON,
                CATEGORY
        ));

        for (Module module : ModuleManager.getInstance().getModules()) {
            KeyMapping mapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    translationKeyFor(module),
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    CATEGORY
            ));
            module.setKeyMapping(mapping);
            moduleKeys.put(module, mapping);
        }
    }

    private String translationKeyFor(Module module) {
        return "key.moveclient.toggle_" + module.getName().toLowerCase(Locale.ROOT).replace("-", "");
    }

    /** Polls every registered keybind for clicks. Must run once per client tick. */
    public void onClientTick(Minecraft client) {
        while (openGuiKey.consumeClick()) {
            if (client.gui.screen() instanceof ClickGuiScreen) {
                client.gui.setScreen(null);
            } else if (client.gui.screen() == null) {
                client.gui.setScreen(new ClickGuiScreen());
            }
        }

        for (Map.Entry<Module, KeyMapping> entry : moduleKeys.entrySet()) {
            KeyMapping mapping = entry.getValue();
            boolean toggled = false;
            while (mapping.consumeClick()) {
                toggled = true;
            }
            if (toggled) {
                entry.getKey().toggle();
                ConfigManager.save();
            }
        }
    }
}
