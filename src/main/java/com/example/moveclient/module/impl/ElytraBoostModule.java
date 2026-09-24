package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Adds forward thrust in the look direction while gliding on an elytra and holding forward,
 * capped to a configurable max added speed so it stays a tunable boost rather than unlimited
 * acceleration.
 */
public class ElytraBoostModule extends Module {

    private final Setting.DoubleSetting thrust;
    private final Setting.DoubleSetting maxSpeed;

    public ElytraBoostModule() {
        super("ElytraBoost", "Forward thrust while gliding on an elytra", ModuleCategory.MOVEMENT);
        thrust = registerDouble("Thrust", 0.15, 0.01, 0.6, 0.01);
        maxSpeed = registerDouble("Max Speed", 2.5, 0.5, 6.0, 0.1);
    }

    @Override
    protected void onTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || !player.isFallFlying() || !client.options.keyUp.isDown()) {
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 velocity = player.getDeltaMovement();
        double currentSpeedInLookDir = velocity.dot(look);
        double addSpeed = maxSpeed.get() - currentSpeedInLookDir;
        if (addSpeed <= 0) {
            return;
        }

        double accel = Math.min(thrust.get(), addSpeed);
        player.setDeltaMovement(velocity.add(look.scale(accel)));
    }
}
