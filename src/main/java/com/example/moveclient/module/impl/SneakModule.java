package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Forces continuous sneaking while enabled - sets the same {@code shiftKeyDown} flag vanilla sets
 * while you hold the real sneak key, so it gets the same reduced hitbox, no-falling-off-edges
 * movement, and server-side sync as normal sneaking, without needing to hold the key yourself.
 */
public class SneakModule extends Module {

    public SneakModule() {
        super("Sneak", "Forces continuous sneaking without holding the key", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        setShiftKeyDown(true);
    }

    @Override
    protected void onDisable() {
        setShiftKeyDown(false);
    }

    @Override
    protected void onTick() {
        setShiftKeyDown(true);
    }

    private void setShiftKeyDown(boolean down) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setShiftKeyDown(down);
        }
    }
}
