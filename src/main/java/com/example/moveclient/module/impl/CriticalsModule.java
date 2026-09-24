package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Times a tiny hop the instant you attack while standing on the ground, so the hit registers as a
 * critical (vanilla's 1.5x bonus for hitting while falling) — automating the manual "jump-reset
 * crit" timing trick. Only ever reacts to the player's own attack key; it never targets, aims, or
 * attacks anything by itself (see {@code KillauraModule} for that, which is deliberately a
 * separate, narrowly-scoped module).
 *
 * Uses read-only {@code isDown()} polling for the attack key rather than {@code consumeClick()}:
 * unlike the movement keys, it isn't certain whether this build's attack/interaction handling
 * itself depends on that key's click counter, and breaking the ability to attack at all would be
 * a far worse failure than this module occasionally missing a very fast single tap.
 */
public class CriticalsModule extends Module {

    private final Setting.DoubleSetting nudgeVelocity;

    private boolean wasAttackDown;

    public CriticalsModule() {
        super("Criticals", "Times a tiny hop so your own attacks land as critical hits", ModuleCategory.PLAYER);
        nudgeVelocity = registerDouble("Nudge Velocity", 0.1, 0.02, 0.3, 0.01);
    }

    @Override
    protected void onDisable() {
        wasAttackDown = false;
    }

    @Override
    protected void onTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        boolean attackDown = client.options.keyAttack.isDown();
        boolean justPressed = attackDown && !wasAttackDown;
        wasAttackDown = attackDown;

        if (!justPressed || !player.onGround()) {
            return;
        }

        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x, nudgeVelocity.get(), v.z);
    }
}
