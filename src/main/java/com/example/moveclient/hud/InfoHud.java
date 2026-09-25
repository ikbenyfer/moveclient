package com.example.moveclient.hud;

import com.example.moveclient.module.ModuleManager;
import com.example.moveclient.module.impl.CoordinatesModule;
import com.example.moveclient.module.impl.CpsModule;
import com.example.moveclient.module.impl.VelocityModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draws the Coordinates/Velocity/CPS readouts, stacked bottom-left, one line per module that's
 * currently enabled (and nothing at all when none are). Each of those three modules is a pure
 * display toggle with no state of its own to render (Coordinates/Velocity read the player
 * directly; CPS reads its own click-tracking deques) - this class is just their shared renderer,
 * the same separation {@code XrayModule}/{@code XrayHud} already use.
 */
public final class InfoHud {

    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int LINE_HEIGHT = 10;
    private static final int MARGIN = 4;

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        List<String> lines = new ArrayList<>();

        if (ModuleManager.getInstance().getByName("Coordinates") instanceof CoordinatesModule module && module.isEnabled()) {
            lines.add(describeCoordinates(player));
        }
        if (ModuleManager.getInstance().getByName("Velocity") instanceof VelocityModule module && module.isEnabled()) {
            lines.add(describeVelocity(player));
        }
        if (ModuleManager.getInstance().getByName("CPS") instanceof CpsModule module && module.isEnabled()) {
            lines.add(String.format(Locale.ROOT, "CPS: %d atk / %d use", module.getAttackCps(), module.getUseCps()));
        }

        if (lines.isEmpty()) {
            return;
        }

        int y = graphics.guiHeight() - MARGIN - LINE_HEIGHT * lines.size();
        for (String line : lines) {
            graphics.text(client.font, line, MARGIN, y, TEXT_COLOR, true);
            y += LINE_HEIGHT;
        }
    }

    private static String describeCoordinates(LocalPlayer player) {
        return String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f (%s)",
                player.getX(), player.getY(), player.getZ(), facing(player.getYRot()));
    }

    // Vanilla's yaw convention: 0 deg faces south, increasing yaw rotates toward west (same
    // convention the F3 debug screen's own facing readout uses).
    private static String facing(float yawDegrees) {
        String[] directions = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int index = Math.floorMod(Math.round(yawDegrees / 45.0f), 8);
        return directions[index];
    }

    private static String describeVelocity(LocalPlayer player) {
        Vec3 motion = player.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z) * 20.0;
        double vertical = motion.y * 20.0;
        return String.format(Locale.ROOT, "Speed: %.1f m/s (vert %.1f)", horizontal, vertical);
    }
}
