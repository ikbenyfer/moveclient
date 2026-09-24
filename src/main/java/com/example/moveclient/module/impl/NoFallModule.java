package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

/**
 * Prevents fall damage in a controlled test world. Intentionally scoped to a world the player
 * fully controls: it resets the client-predicted fall distance every tick while airborne past the
 * threshold, and when the game is hosting its own integrated singleplayer/LAN server it also
 * resets the authoritative fall distance on that server's copy of the player entity (via
 * {@link MinecraftServer#execute}, which safely schedules the write on the server thread). Against
 * a remote dedicated server this only suppresses the client-side prediction/hurt animation; the
 * activation conditions below let you restrict it to situations where that matters.
 *
 * The server-side reset only fires once, exactly on the tick the player lands after a
 * significant fall, instead of on every airborne tick: a fall from the build limit to bedrock
 * takes several seconds, and resetting server-side every tick throughout that meant dozens of
 * redundant cross-thread tasks for a single fall. Landing is the only tick that actually matters
 * (it's when vanilla would apply the damage), so that's the only tick this schedules one.
 */
public class NoFallModule extends Module {

    private final Setting.DoubleSetting minFallDistance;
    private final Setting.BooleanSetting onlySurvivalOrAdventure;
    private final Setting.BooleanSetting singleplayerOnly;

    private boolean wasOnGround = true;
    private boolean significantFallInProgress;

    public NoFallModule() {
        super("NoFall", "Prevents fall damage in a controlled test world", ModuleCategory.PLAYER);
        minFallDistance = registerDouble("Min Fall Distance", 3.0, 0.0, 30.0, 0.5);
        onlySurvivalOrAdventure = registerBoolean("Only Survival/Adventure", true);
        singleplayerOnly = registerBoolean("Singleplayer/LAN Only", true);
    }

    @Override
    protected void onDisable() {
        wasOnGround = true;
        significantFallInProgress = false;
    }

    @Override
    protected void onTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        // Creative/spectator abilities already grant fall-damage immunity (invulnerable=true);
        // when restricted to survival/adventure, skip acting there since it would be a no-op.
        if (onlySurvivalOrAdventure.get() && player.getAbilities().invulnerable) {
            return;
        }

        if (singleplayerOnly.get() && !client.hasSingleplayerServer()) {
            return;
        }

        boolean onGround = player.onGround();
        boolean justLanded = onGround && !wasOnGround;

        if (!onGround) {
            if (player.fallDistance >= minFallDistance.get()) {
                significantFallInProgress = true;
                player.resetFallDistance();
            }
        } else if (justLanded && significantFallInProgress) {
            resetServerSideFallDistance(client, player);
            significantFallInProgress = false;
        }

        wasOnGround = onGround;
    }

    private void resetServerSideFallDistance(Minecraft client, LocalPlayer player) {
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            PlayerList playerList = server.getPlayerList();
            if (playerList == null) {
                return;
            }
            ServerPlayer serverPlayer = playerList.getPlayer(player.getUUID());
            if (serverPlayer != null) {
                serverPlayer.resetFallDistance();
            }
        });
    }
}
