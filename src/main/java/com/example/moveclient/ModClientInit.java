package com.example.moveclient;

import com.example.moveclient.config.ConfigManager;
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

    @Override
    public void onInitializeClient() {
        keybindManager.registerAll();
        ConfigManager.load();

        // Built-in resource pack backing the Xray module's texture-based view: transparent
        // textures for common stone/dirt-family blocks, bundled inside this mod's own jar under
        // resourcepacks/xray/. NORMAL activation means it's known to the pack repository but not
        // enabled by default; XrayModule enables/disables it itself via addPack/removePack + a
        // resource reload when the module is toggled.
        FabricLoader.getInstance().getModContainer("moveclient").ifPresent(mod ->
                ResourceLoader.registerBuiltinPack(
                        Identifier.fromNamespaceAndPath("moveclient", "xray"), mod, PackActivationType.NORMAL));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            keybindManager.onClientTick(client);
            ModuleManager.getInstance().tickAll(client);
        });

        // Registered as a single HudElement rather than two separate ones: this is a defensive
        // consolidation onto the one registration already confirmed to render (the module list),
        // so Xray's HUD text can no longer be affected by anything specific to being its own,
        // second HudElementRegistry entry. A failure inside xrayHud.render() is caught and logged
        // instead of silently disappearing, in case that's what was happening.
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("moveclient", "hud"),
                (graphics, deltaTracker) -> {
                    hud.render(graphics, deltaTracker);
                    try {
                        xrayHud.render(graphics, deltaTracker);
                    } catch (RuntimeException e) {
                        System.err.println("[moveclient] XrayHud.render threw:");
                        e.printStackTrace();
                    }
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
}
