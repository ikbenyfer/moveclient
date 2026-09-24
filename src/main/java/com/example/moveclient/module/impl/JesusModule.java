package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps the player at the surface of water (and optionally lava) instead of sinking, so
 * horizontal movement mechanics can be tested over liquid the same way as over solid ground.
 * Holding sneak overrides it for a normal dive, matching the common "Jesus" convention.
 */
public class JesusModule extends Module {

    private final Setting.BooleanSetting affectLava;

    public JesusModule() {
        super("Jesus", "Walk on the surface of water (sneak to dive)", ModuleCategory.PLAYER);
        affectLava = registerBoolean("Affect Lava", false);
    }

    @Override
    protected void onTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || player.isShiftKeyDown()) {
            return;
        }

        boolean onFluid = player.isInWater() || (affectLava.get() && player.isInLava());
        if (!onFluid) {
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y < 0) {
            player.setDeltaMovement(velocity.x, 0.0, velocity.z);
            player.resetFallDistance();
        }
    }
}
