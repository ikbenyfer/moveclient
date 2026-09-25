package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

/**
 * Automatically respawns the instant you die, sending the same packet vanilla's own death-screen
 * "Respawn" button sends ({@code ServerboundClientCommandPacket(Action.PERFORM_RESPAWN)}, via
 * {@code LocalPlayer#connection}) instead of waiting for you to click it.
 *
 * Edge-detected on {@link net.minecraft.world.entity.LivingEntity#isDeadOrDying()} - sent once per
 * death, not every tick while dead - for the same reason {@code CriticalsModule} edge-detects its
 * key: sending the respawn packet repeatedly every tick while the first one is already in flight
 * would just be redundant spam, not make anything happen sooner.
 */
public class AutoRespawnModule extends Module {

    private boolean wasDead;

    public AutoRespawnModule() {
        super("AutoRespawn", "Automatically respawns the instant you die", ModuleCategory.PLAYER);
    }

    @Override
    protected void onDisable() {
        wasDead = false;
    }

    @Override
    protected void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        boolean dead = player.isDeadOrDying();
        if (dead && !wasDead) {
            player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        }
        wasDead = dead;
    }
}
