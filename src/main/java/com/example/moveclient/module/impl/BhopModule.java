package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Classic "bunny hop" movement mechanic: auto-hop removes the manual jump-timing requirement,
 * and air-strafe adds a Quake/CS-style acceleration impulse while airborne. Ported from the
 * standalone bhop-mod project in the sibling directory, adapted to the ModuleManager lifecycle.
 */
public class BhopModule extends Module {

    private final Setting.DoubleSetting airAccel;
    private final Setting.DoubleSetting maxAirStrafeSpeed;
    private final Setting.DoubleSetting jumpVelocity;
    private final Setting.BooleanSetting autoHop;

    public BhopModule() {
        super("Bhop", "Auto-hop + air-strafe acceleration", ModuleCategory.MOVEMENT);
        airAccel = registerDouble("Air Accel", 4.0, 0.5, 12.0, 0.1);
        maxAirStrafeSpeed = registerDouble("Max Air Speed", 0.55, 0.1, 1.5, 0.01);
        jumpVelocity = registerDouble("Jump Velocity", 0.42, 0.1, 1.0, 0.01);
        autoHop = registerBoolean("Auto Hop", true);
    }

    @Override
    protected void onTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        boolean onGround = player.onGround();
        boolean jumpHeld = client.options.keyJump.isDown();

        if (autoHop.get() && onGround && jumpHeld) {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x, jumpVelocity.get(), v.z);
        }

        if (!onGround) {
            applyAirStrafe(player, client);
        }
    }

    private void applyAirStrafe(LocalPlayer player, Minecraft client) {
        double forward = 0;
        double strafe = 0;
        if (client.options.keyUp.isDown()) forward += 1;
        if (client.options.keyDown.isDown()) forward -= 1;
        if (client.options.keyRight.isDown()) strafe += 1;
        if (client.options.keyLeft.isDown()) strafe -= 1;
        if (forward == 0 && strafe == 0) return;

        float yaw = player.getYRot();
        double yawRad = Math.toRadians(yaw);

        double wishX = -Math.sin(yawRad) * forward - Math.cos(yawRad) * strafe;
        double wishZ = Math.cos(yawRad) * forward - Math.sin(yawRad) * strafe;
        double len = Math.sqrt(wishX * wishX + wishZ * wishZ);
        if (len < 1e-6) return;
        wishX /= len;
        wishZ /= len;

        Vec3 velocity = player.getDeltaMovement();
        double currentSpeedInWishDir = velocity.x * wishX + velocity.z * wishZ;
        double addSpeed = maxAirStrafeSpeed.get() - currentSpeedInWishDir;
        if (addSpeed <= 0) return;

        double accelSpeed = Math.min(airAccel.get() * 0.05, addSpeed);

        double newX = velocity.x + wishX * accelSpeed;
        double newZ = velocity.z + wishZ * accelSpeed;
        player.setDeltaMovement(newX, velocity.y, newZ);
    }
}
