package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;

/**
 * Enables the current-speed HUD readout drawn by {@code InfoHud}. Purely a display toggle - the
 * HUD reads {@code player.getDeltaMovement()} (already updated every client tick regardless of
 * this module) directly at render time, so this module needs no tick logic of its own.
 */
public class VelocityModule extends Module {

    public VelocityModule() {
        super("Velocity", "Shows your current movement speed in blocks/second", ModuleCategory.MOVEMENT);
    }
}
