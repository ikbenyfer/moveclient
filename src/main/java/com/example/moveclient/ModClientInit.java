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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            keybindManager.onClientTick(client);
            ModuleManager.getInstance().tickAll(client);
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("moveclient", "module_list"),
                (graphics, deltaTracker) -> hud.render(graphics, deltaTracker));
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("moveclient", "xray_list"),
                (graphics, deltaTracker) -> xrayHud.render(graphics, deltaTracker));

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
