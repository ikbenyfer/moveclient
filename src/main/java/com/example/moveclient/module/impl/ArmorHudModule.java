package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;

/**
 * Enables the worn-armor icon row drawn by {@code ArmorStatusHud}. Purely a display toggle - the
 * HUD reads {@code player.getItemBySlot(EquipmentSlot)} directly at render time, so this module
 * needs no tick logic of its own.
 */
public class ArmorHudModule extends Module {

    public ArmorHudModule() {
        super("ArmorHUD", "Shows your currently worn armor and its durability", ModuleCategory.PLAYER);
    }
}
