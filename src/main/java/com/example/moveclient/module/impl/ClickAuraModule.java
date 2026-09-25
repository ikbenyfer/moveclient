package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Attacks the nearest entity you're looking at, but only on your own click - unlike
 * {@code KillauraModule} (fully automatic, no aiming or clicking required), this reacts to
 * {@code keyAttack} exactly like {@code CriticalsModule} does, and only ever supplements a click
 * that vanilla wouldn't already resolve to an entity itself ({@link Minecraft#crosshairPickEntity}
 * is null - your crosshair isn't precisely on a hitbox), picking the nearest living entity within
 * a small cone around your actual look direction instead. Two clean hits never happen on the same
 * click: if vanilla already has a crosshair target, this module does nothing and lets the normal
 * attack happen untouched.
 *
 * {@link Player} is unconditionally excluded from targeting in code, not behind a setting - the
 * same hard rule {@code KillauraModule} enforces and for the same reason: an "aura" that can hit
 * players is overwhelmingly used as a PvP cheat against real people, which this project isn't
 * willing to build, no matter how its settings are configured.
 *
 * Uses read-only {@code isDown()} polling for the attack key, not {@code consumeClick()}, for the
 * same reason documented on {@code CriticalsModule}: it isn't certain this build's own attack
 * handling doesn't depend on that key's click counter, and breaking normal attacking entirely
 * would be a far worse failure than this module occasionally missing a very fast single tap.
 */
public class ClickAuraModule extends Module {

    private final Setting.DoubleSetting range;
    private final Setting.DoubleSetting aimAssistAngle;

    private boolean wasAttackDown;

    public ClickAuraModule() {
        super("ClickAura", "Attacks the nearest entity you're looking at when you click (never targets players)", ModuleCategory.PLAYER);
        range = registerDouble("Range", 4.5, 2.0, 8.0, 0.5);
        aimAssistAngle = registerDouble("Aim Assist Angle", 10, 1, 45, 1);
    }

    @Override
    protected void onDisable() {
        wasAttackDown = false;
    }

    @Override
    protected void onTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            return;
        }

        boolean attackDown = client.options.keyAttack.isDown();
        boolean justPressed = attackDown && !wasAttackDown;
        wasAttackDown = attackDown;

        if (!justPressed || client.crosshairPickEntity != null) {
            return;
        }

        Entity target = findNearestLookedAtEntity(player, range.get(), aimAssistAngle.get());
        if (target == null) {
            return;
        }

        client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
    }

    private Entity findNearestLookedAtEntity(LocalPlayer player, double reach, double maxAngleDegrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double cosThreshold = Math.cos(Math.toRadians(maxAngleDegrees));

        AABB box = player.getBoundingBox().inflate(reach);
        List<Entity> candidates = player.level().getEntities(player, box,
                entity -> !(entity instanceof Player) && entity instanceof LivingEntity living && living.isAlive());

        Entity closest = null;
        double closestDistSqr = Double.MAX_VALUE;
        for (Entity entity : candidates) {
            Vec3 toEntity = entity.getBoundingBox().getCenter().subtract(eye);
            double dist = toEntity.length();
            if (dist > reach || dist == 0) {
                continue;
            }
            double cosAngle = look.dot(toEntity.scale(1.0 / dist));
            if (cosAngle < cosThreshold) {
                continue;
            }
            double distSqr = dist * dist;
            if (distSqr < closestDistSqr) {
                closest = entity;
                closestDistSqr = distSqr;
            }
        }
        return closest;
    }
}
