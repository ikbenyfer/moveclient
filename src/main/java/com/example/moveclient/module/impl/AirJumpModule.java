package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Allows extra mid-air jumps (double/triple/... jump), up to a configurable count per airtime.
 *
 * Uses {@code KeyMapping.consumeClick()} rather than per-tick {@code isDown()} polling: a plain
 * "was it down last tick vs. this tick" comparison can miss a genuine press entirely if the key
 * goes down and up again faster than one client tick (~50ms), which made the module feel
 * unreliable/broken. {@code consumeClick()} is backed by the actual GLFW key-press event, so
 * every real press is always caught regardless of tick timing.
 */
public class AirJumpModule extends Module {

    private final Setting.DoubleSetting maxJumps;
    private final Setting.DoubleSetting jumpVelocity;

    private int jumpsUsed;

    public AirJumpModule() {
        super("AirJump", "Extra mid-air jumps beyond the first", ModuleCategory.MOVEMENT);
        maxJumps = registerDouble("Extra Jumps", 1, 1, 5, 1);
        jumpVelocity = registerDouble("Jump Velocity", 0.42, 0.1, 1.0, 0.01);
    }

    @Override
    protected void onDisable() {
        jumpsUsed = 0;
        // Drain any clicks queued while briefly airborne right before disabling, so they don't
        // get replayed as a surprise jump the moment this module is re-enabled.
        while (Minecraft.getInstance().options.keyJump.consumeClick()) {
            // discard
        }
    }

    @Override
    protected void onTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        if (player.onGround()) {
            jumpsUsed = 0;
        }

        boolean pressed = false;
        while (client.options.keyJump.consumeClick()) {
            pressed = true;
        }

        if (pressed && !player.onGround() && jumpsUsed < maxJumps.get()) {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x, jumpVelocity.get(), v.z);
            jumpsUsed++;
        }
    }
}
