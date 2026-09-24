package com.example.moveclient.module;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for a single toggleable client feature (Bhop, Flight, Anti-Knockback, NoFall, ...).
 *
 * Lifecycle contract: {@link #onEnable()} runs exactly once when a module transitions from
 * disabled to enabled, {@link #onDisable()} runs exactly once on the reverse transition (including
 * when the mod is unloaded/the game closes while a module is still active), and {@link #onTick()}
 * runs once per client tick while the module is enabled and a player exists. Implementations must
 * restore any world/player state they changed inside {@link #onDisable()}.
 */
public abstract class Module {

    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;
    private KeyMapping keyMapping;

    protected Module(String name, String description, ModuleCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public final ModuleCategory getCategory() {
        return category;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final void setKeyMapping(KeyMapping keyMapping) {
        this.keyMapping = keyMapping;
    }

    public final KeyMapping getKeyMapping() {
        return keyMapping;
    }

    public final List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    protected final Setting.BooleanSetting registerBoolean(String name, boolean defaultValue) {
        Setting.BooleanSetting setting = new Setting.BooleanSetting(name, defaultValue);
        settings.add(setting);
        return setting;
    }

    protected final Setting.DoubleSetting registerDouble(String name, double defaultValue, double min, double max, double step) {
        Setting.DoubleSetting setting = new Setting.DoubleSetting(name, defaultValue, min, max, step);
        settings.add(setting);
        return setting;
    }

    /**
     * Flips the module's enabled state and invokes the matching lifecycle hook.
     * Safe to call even when no player/world is loaded.
     */
    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean newState) {
        if (newState == enabled) {
            return;
        }
        enabled = newState;
        try {
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
        } catch (RuntimeException e) {
            System.err.println("[moveclient] " + name + " threw during " + (enabled ? "onEnable" : "onDisable") + ":");
            e.printStackTrace();
            enabled = false;
        }
        sendActionBar(name + ": " + (enabled ? "ON" : "OFF"));
    }

    /** Called once when the module becomes active. Override to acquire/initialize state. */
    protected void onEnable() {
    }

    /** Called once when the module becomes inactive. Override to release/restore state. */
    protected void onDisable() {
    }

    /** Called every client tick while enabled and a player is present. */
    protected void onTick() {
    }

    /**
     * Re-runs {@link #onEnable()} if this module is already enabled. Used when the player joins a
     * new world/server: the previous {@link #onEnable()} call applied its side effects (e.g.
     * disabling gravity) to the now-discarded old player entity, so an already-enabled module needs
     * those effects re-applied to the fresh player instance.
     */
    public final void reapplyIfEnabled() {
        if (enabled) {
            onEnable();
        }
    }

    /**
     * Internal dispatch used by ModuleManager; guards against ticking without a loaded player.
     * A module's {@link #onTick()} runs every client tick regardless of whether the ClickGUI is
     * open (the GUI doesn't pause the game), so a single misbehaving module must never be able to
     * crash the whole client: any exception here is logged and the module is disabled instead of
     * propagating.
     */
    public final void tickIfReady(Minecraft client) {
        if (!enabled) {
            return;
        }
        if (client.player == null || client.level == null) {
            return;
        }
        try {
            onTick();
        } catch (RuntimeException e) {
            System.err.println("[moveclient] " + name + " threw during tick, disabling it:");
            e.printStackTrace();
            setEnabled(false);
        }
    }

    protected final void sendActionBar(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(message));
        }
    }
}
