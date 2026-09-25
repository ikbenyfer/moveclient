package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;

/**
 * Enables the X/Y/Z + facing-direction HUD readout drawn by {@code InfoHud}. Purely a display
 * toggle - the HUD reads the player's live position directly at render time, so this module needs
 * no tick logic of its own.
 */
public class CoordinatesModule extends Module {

    public CoordinatesModule() {
        super("Coordinates", "Shows your current X/Y/Z position and facing direction", ModuleCategory.WORLD);
    }
}
