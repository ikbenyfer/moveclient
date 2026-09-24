package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Toggleable three-dimensional flight: WASD moves horizontally relative to look direction,
 * jump/sneak act as separate ascend/descend controls, and vertical speed is independently
 * configurable from horizontal speed. Disables gravity while active and restores it on disable.
 */
public class FlightModule extends Module {

    private final Setting.DoubleSetting horizontalSpeed;
    private final Setting.DoubleSetting verticalSpeed;
    private final Setting.BooleanSetting hoverWhenIdle;

    public FlightModule() {
        super("Flight", "Toggleable 3D flight with independent ascend/descend speed", ModuleCategory.MOVEMENT);
        horizontalSpeed = registerDouble("Horizontal Speed", 0.6, 0.05, 3.0, 0.05);
        verticalSpeed = registerDouble("Vertical Speed", 0.6, 0.05, 3.0, 0.05);
        hoverWhenIdle = registerBoolean("Hover When Idle", true);
    }

    @Override
    protected void onEnable() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setNoGravity(true);
            player.resetFallDistance();
        }
    }

    @Override
    protected void onDisable() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setNoGravity(false);
            // Prevent the sudden drop from flight altitude being scored as a fall on landing.
            player.resetFallDistance();
        }
    }

    @Override
    protected void onTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        double forward = 0;
        double strafe = 0;
        if (client.options.keyUp.isDown()) forward += 1;
        if (client.options.keyDown.isDown()) forward -= 1;
        if (client.options.keyRight.isDown()) strafe += 1;
        if (client.options.keyLeft.isDown()) strafe -= 1;

        double wishX = 0;
        double wishZ = 0;
        if (forward != 0 || strafe != 0) {
            float yaw = player.getYRot();
            double yawRad = Math.toRadians(yaw);
            wishX = -Math.sin(yawRad) * forward - Math.cos(yawRad) * strafe;
            wishZ = Math.cos(yawRad) * forward - Math.sin(yawRad) * strafe;
            double len = Math.sqrt(wishX * wishX + wishZ * wishZ);
            if (len > 1e-6) {
                wishX = wishX / len * horizontalSpeed.get();
                wishZ = wishZ / len * horizontalSpeed.get();
            }
        }

        boolean ascend = client.options.keyJump.isDown();
        boolean descend = client.options.keyShift.isDown();
        double wishY;
        if (ascend && !descend) {
            wishY = verticalSpeed.get();
        } else if (descend && !ascend) {
            wishY = -verticalSpeed.get();
        } else {
            wishY = hoverWhenIdle.get() ? 0.0 : player.getDeltaMovement().y;
        }

        player.setDeltaMovement(wishX, wishY, wishZ);
        player.resetFallDistance();
    }
}
