package com.example.moveclient.hud;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** Draws a compact list of currently-enabled modules in the top-right corner of the HUD. */
public final class ModuleHud {

    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int LINE_HEIGHT = 10;
    private static final int MARGIN = 4;

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        List<Module> enabled = ModuleManager.getInstance().getModules().stream()
                .filter(Module::isEnabled)
                .toList();
        if (enabled.isEmpty()) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        int y = MARGIN;
        for (Module module : enabled) {
            String text = module.getName();
            int textWidth = client.font.width(text);
            int x = screenWidth - textWidth - MARGIN;
            graphics.text(client.font, text, x, y, TEXT_COLOR, true);
            y += LINE_HEIGHT;
        }
    }
}
