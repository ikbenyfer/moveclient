package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Disables collision via {@code Entity.noPhysics}, for testing level geometry/movement without
 * being blocked by it. Restored to {@code false} on disable, and re-applied on world join in
 * case it was already enabled (see {@link Module#reapplyIfEnabled()}).
 */
public class NoClipModule extends Module {

    public NoClipModule() {
        super("NoClip", "Disables collision with the world", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.noPhysics = true;
        }
    }

    @Override
    protected void onDisable() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.noPhysics = false;
        }
    }
}
