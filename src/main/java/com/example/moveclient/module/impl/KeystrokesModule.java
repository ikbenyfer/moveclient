package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;

/**
 * Enables the WASD/jump/sneak key-state overlay drawn by {@code KeystrokesHud}. Purely a display
 * toggle - the HUD reads each key's live {@code isDown()} state directly at render time, so this
 * module needs no tick logic of its own.
 */
public class KeystrokesModule extends Module {

    public KeystrokesModule() {
        super("Keystrokes", "Shows which movement keys are currently held", ModuleCategory.MOVEMENT);
    }
}
