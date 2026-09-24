package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Keeps the player sprinting while moving forward, without needing to hold the sprint key.
 * Purely a per-tick nudge (no state acquired), so there is nothing to restore on disable:
 * releasing control simply hands sprint state back to vanilla's own input handling.
 */
public class AutoSprintModule extends Module {

    public AutoSprintModule() {
        super("AutoSprint", "Sprints automatically while moving forward", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        boolean movingForward = client.options.keyUp.isDown();
        if (movingForward && !player.isSprinting() && !player.isShiftKeyDown() && player.canSprint()) {
            player.setSprinting(true);
        }
    }
}
