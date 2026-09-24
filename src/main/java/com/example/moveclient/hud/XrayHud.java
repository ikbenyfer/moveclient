package com.example.moveclient.hud;

import com.example.moveclient.module.ModuleManager;
import com.example.moveclient.module.impl.XrayModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Locale;

/** Draws the Xray module's nearest-ore readout (block, direction, distance) in the top-left corner. */
public final class XrayHud {

    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int TEXT_COLOR = 0xFFD54A;
    private static final int LINE_HEIGHT = 10;
    private static final int MARGIN = 4;

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        Object module = ModuleManager.getInstance().getByName("Xray");
        if (!(module instanceof XrayModule xray) || !xray.isEnabled()) {
            return;
        }

        List<XrayModule.OreHit> ores = xray.getNearestOres();

        int y = MARGIN;
        graphics.text(client.font, "Xray", MARGIN, y, TITLE_COLOR, true);
        y += LINE_HEIGHT;

        // Always draw *something* while enabled, even with zero matches: a module that goes
        // fully silent when its result set is empty is indistinguishable from a broken one.
        if (ores.isEmpty()) {
            String message = String.format(Locale.ROOT, "no ores within %.0fm", xray.getRadius());
            graphics.text(client.font, message, MARGIN, y, 0x808080, true);
            return;
        }

        for (XrayModule.OreHit hit : ores) {
            graphics.text(client.font, describe(player, hit), MARGIN, y, TEXT_COLOR, true);
            y += LINE_HEIGHT;
        }
    }

    private static String describe(LocalPlayer player, XrayModule.OreHit hit) {
        BlockPos pos = hit.pos();
        double dx = pos.getX() + 0.5 - player.getX();
        double dy = pos.getY() + 0.5 - player.getY();
        double dz = pos.getZ() + 0.5 - player.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        StringBuilder direction = new StringBuilder();
        if (Math.round(Math.abs(dx)) >= 1) {
            direction.append(Math.round(Math.abs(dx))).append(dx > 0 ? "E " : "W ");
        }
        if (Math.round(Math.abs(dz)) >= 1) {
            direction.append(Math.round(Math.abs(dz))).append(dz > 0 ? "S " : "N ");
        }
        if (Math.round(Math.abs(dy)) >= 1) {
            direction.append(Math.round(Math.abs(dy))).append(dy > 0 ? " up" : " down");
        }

        return String.format(Locale.ROOT, "%s: %s(%.0fm)", hit.block().getName().getString(), direction, distance);
    }
}
