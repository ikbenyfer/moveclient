package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Automatically attacks the nearest hostile mob within range — a mob-farm-style combat assist,
 * not an aim-bot. {@link Player} is unconditionally excluded from targeting in code, not behind a
 * setting: the overwhelmingly common real-world use of an "aura" that can hit players is as a PvP
 * cheat against other people on a shared server, which this project isn't willing to build. This
 * module can only ever attack {@link Monster} instances (zombies, skeletons, creepers, ...), no
 * matter how its settings are configured.
 */
public class KillauraModule extends Module {

    private final Setting.DoubleSetting range;
    private final Setting.DoubleSetting attackIntervalTicks;

    private int cooldownTicks;

    public KillauraModule() {
        super("Killaura", "Auto-attacks the nearest hostile mob in range (never targets players)", ModuleCategory.PLAYER);
        range = registerDouble("Range", 4.0, 2.0, 6.0, 0.5);
        attackIntervalTicks = registerDouble("Attack Interval (ticks)", 10, 4, 40, 1);
    }

    @Override
    protected void onDisable() {
        cooldownTicks = 0;
    }

    @Override
    protected void onTick() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            return;
        }

        Entity target = findNearestHostileMob(player, range.get());
        if (target == null) {
            return;
        }

        client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
        cooldownTicks = attackIntervalTicks.get().intValue();
    }

    private Entity findNearestHostileMob(LocalPlayer player, double reach) {
        AABB box = player.getBoundingBox().inflate(reach);
        List<Entity> candidates = player.level().getEntities(player, box,
                entity -> !(entity instanceof Player) && entity instanceof Monster monster && monster.isAlive());

        Entity closest = null;
        double closestDistSqr = Double.MAX_VALUE;
        for (Entity entity : candidates) {
            double distSqr = player.distanceToSqr(entity);
            if (distSqr <= reach * reach && distSqr < closestDistSqr) {
                closest = entity;
                closestDistSqr = distSqr;
            }
        }
        return closest;
    }
}
