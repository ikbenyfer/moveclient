package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Lets the player climb any wall they are pressed against while holding jump, like a ladder.
 * Purely a per-tick velocity nudge gated on {@code Entity.horizontalCollision}; nothing is
 * acquired on enable, so there is nothing to restore on disable.
 */
public class SpiderModule extends Module {

    private final Setting.DoubleSetting climbSpeed;

    public SpiderModule() {
        super("Spider", "Climb walls you're pressed against while holding jump", ModuleCategory.MOVEMENT);
        climbSpeed = registerDouble("Climb Speed", 0.2, 0.02, 0.6, 0.02);
    }

    @Override
    protected void onTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        if (!player.horizontalCollision || !client.options.keyJump.isDown()) {
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, climbSpeed.get(), velocity.z);
        player.resetFallDistance();
    }
}
