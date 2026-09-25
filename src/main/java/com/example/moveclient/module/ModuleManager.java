package com.example.moveclient.module;

import com.example.moveclient.module.impl.AirJumpModule;
import com.example.moveclient.module.impl.AntiKnockbackModule;
import com.example.moveclient.module.impl.AutoArmorModule;
import com.example.moveclient.module.impl.AutoRespawnModule;
import com.example.moveclient.module.impl.AutoSprintModule;
import com.example.moveclient.module.impl.AutoTotemModule;
import com.example.moveclient.module.impl.BhopModule;
import com.example.moveclient.module.impl.BrightnessModule;
import com.example.moveclient.module.impl.ClickAuraModule;
import com.example.moveclient.module.impl.CriticalsModule;
import com.example.moveclient.module.impl.ElytraBoostModule;
import com.example.moveclient.module.impl.FlightModule;
import com.example.moveclient.module.impl.GravityModule;
import com.example.moveclient.module.impl.HighJumpModule;
import com.example.moveclient.module.impl.JesusModule;
import com.example.moveclient.module.impl.KillauraModule;
import com.example.moveclient.module.impl.NoClipModule;
import com.example.moveclient.module.impl.NoFallModule;
import com.example.moveclient.module.impl.ReachModule;
import com.example.moveclient.module.impl.SneakModule;
import com.example.moveclient.module.impl.SpeedModule;
import com.example.moveclient.module.impl.SpiderModule;
import com.example.moveclient.module.impl.StepModule;
import com.example.moveclient.module.impl.TeleportModule;
import com.example.moveclient.module.impl.XrayModule;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Central registry that owns every module instance and drives their per-tick lifecycle. */
public final class ModuleManager {

    private static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();

    private ModuleManager() {
        register(new BhopModule());
        register(new FlightModule());
        register(new SpeedModule());
        register(new StepModule());
        register(new HighJumpModule());
        register(new GravityModule());
        register(new AutoSprintModule());
        register(new SpiderModule());
        register(new AirJumpModule());
        register(new NoClipModule());
        register(new ElytraBoostModule());
        register(new TeleportModule());
        register(new SneakModule());
        register(new AntiKnockbackModule());
        register(new NoFallModule());
        register(new JesusModule());
        register(new AutoTotemModule());
        register(new AutoArmorModule());
        register(new AutoRespawnModule());
        register(new ReachModule());
        register(new XrayModule());
        register(new BrightnessModule());
        register(new CriticalsModule());
        register(new KillauraModule());
        register(new ClickAuraModule());
    }

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    private void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public Module getByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    /** Ticks every enabled module. Called once per client tick from the mod's tick handler. */
    public void tickAll(Minecraft client) {
        for (Module module : modules) {
            module.tickIfReady(client);
        }
    }

    /** Re-applies enabled modules' onEnable side effects to a freshly joined player. See {@link Module#reapplyIfEnabled()}. */
    public void reapplyEnabledOnJoin() {
        for (Module module : modules) {
            module.reapplyIfEnabled();
        }
    }

    /**
     * Disables every currently-enabled module, running each one's cleanup hook.
     * Used when a world/server connection ends, so nothing leaks state (e.g. no-gravity flight)
     * into the next session.
     */
    public void disableAll() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.setEnabled(false);
            }
        }
    }
}
