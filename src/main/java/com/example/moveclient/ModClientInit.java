package com.example.moveclient;

import com.example.moveclient.config.ConfigManager;
import com.example.moveclient.hud.ArmorStatusHud;
import com.example.moveclient.hud.InfoHud;
import com.example.moveclient.hud.KeystrokesHud;
import com.example.moveclient.hud.ModuleHud;
import com.example.moveclient.hud.XrayHud;
import com.example.moveclient.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

/**
 * Client entrypoint for the movement mechanics test suite. Wires the ModuleManager, keybinds,
 * HUD and JSON config to Fabric's client lifecycle/tick/render events. Purely client-side and
 * intended for use in a private/controlled test world.
 */
public class ModClientInit implements ClientModInitializer {

    private final KeybindManager keybindManager = new KeybindManager();
    private final ModuleHud hud = new ModuleHud();
    private final XrayHud xrayHud = new XrayHud();
    private final InfoHud infoHud = new InfoHud();
    private final KeystrokesHud keystrokesHud = new KeystrokesHud();
    private final ArmorStatusHud armorStatusHud = new ArmorStatusHud();

    @Override
    public void onInitializeClient() {
        keybindManager.registerAll();
        ConfigManager.load();

        // Built-in resource pack backing the Xray module's texture-based view: transparent
        // textures for common stone/dirt-family blocks, bundled inside this mod's own jar under
        // resourcepacks/xray/. NORMAL activation means it's known to the pack repository but not
        // enabled by default; XrayModule enables/disables it itself via addPack/removePack + a
        // resource reload when the module is toggled.
        FabricLoader.getInstance().getModContainer("moveclient").ifPresent(mod -> {
            ResourceLoader.registerBuiltinPack(
                    Identifier.fromNamespaceAndPath("moveclient", "xray"), mod, PackActivationType.NORMAL);

            // Second, independently-toggleable pack: fullbright glow models for ore blocks only
            // (resourcepacks/xray_ores/). Kept separate from the denylist pack above so "Fullbright
            // Ores" can be turned on/off regardless of which hide mode (denylist or allowlist) is
            // active, or with neither active at all.
            ResourceLoader.registerBuiltinPack(
                    Identifier.fromNamespaceAndPath("moveclient", "xray_ores"), mod, PackActivationType.NORMAL);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            keybindManager.onClientTick(client);
            ModuleManager.getInstance().tickAll(client);
        });

        // Registered as a single HudElement rather than several separate ones: this is a
        // defensive consolidation onto the one registration already confirmed to render (the
        // module list), so no HUD piece can be affected by anything specific to being its own,
        // separate HudElementRegistry entry. Each additional renderer's call is individually
        // wrapped in try/catch and logged instead of letting one broken renderer silently take
        // down every other HUD element sharing this same registration.
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("moveclient", "hud"),
                (graphics, deltaTracker) -> {
                    hud.render(graphics, deltaTracker);
                    renderSafely("XrayHud", () -> xrayHud.render(graphics, deltaTracker));
                    renderSafely("InfoHud", () -> infoHud.render(graphics, deltaTracker));
                    renderSafely("KeystrokesHud", () -> keystrokesHud.render(graphics, deltaTracker));
                    renderSafely("ArmorStatusHud", () -> armorStatusHud.render(graphics, deltaTracker));
                });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ModuleManager.getInstance().reapplyEnabledOnJoin());

        // Safety net: modules that changed world/player state (e.g. Flight's no-gravity flag)
        // should never leak past the client shutting down, and the config should always reflect
        // the last known state even if a save was missed elsewhere.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ModuleManager.getInstance().disableAll();
            ConfigManager.save();
        });
    }

    private static void renderSafely(String name, Runnable render) {
        try {
            render.run();
        } catch (RuntimeException e) {
            System.err.println("[moveclient] " + name + ".render threw:");
            e.printStackTrace();
        }
    }
}
