package com.example.moveclient.hud;

import com.example.moveclient.module.ModuleManager;
import com.example.moveclient.module.impl.KeystrokesModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Draws a classic WASD key-state grid, bottom-center, while the Keystrokes module is enabled. */
public final class KeystrokesHud {

    private static final int BOX_SIZE = 18;
    private static final int GAP = 2;
    private static final int MARGIN_BOTTOM = 4;
    private static final int HELD_FILL = 0xFFFFFFFF;
    private static final int IDLE_FILL = 0x80000000;
    private static final int OUTLINE_COLOR = 0xFFFFFFFF;
    private static final int HELD_TEXT_COLOR = 0x000000;
    private static final int IDLE_TEXT_COLOR = 0xFFFFFF;

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        if (!(ModuleManager.getInstance().getByName("Keystrokes") instanceof KeystrokesModule keystrokes) || !keystrokes.isEnabled()) {
            return;
        }

        Options options = client.options;
        int gridWidth = BOX_SIZE * 3 + GAP * 2;
        int left = graphics.guiWidth() / 2 - gridWidth / 2;
        int row2Top = graphics.guiHeight() - MARGIN_BOTTOM - BOX_SIZE;
        int row1Top = row2Top - GAP - BOX_SIZE;

        drawKey(graphics, client, "W", left + BOX_SIZE + GAP, row1Top, options.keyUp.isDown());
        drawKey(graphics, client, "A", left, row2Top, options.keyLeft.isDown());
        drawKey(graphics, client, "S", left + BOX_SIZE + GAP, row2Top, options.keyDown.isDown());
        drawKey(graphics, client, "D", left + (BOX_SIZE + GAP) * 2, row2Top, options.keyRight.isDown());
    }

    private void drawKey(GuiGraphicsExtractor graphics, Minecraft client, String label, int x, int y, boolean held) {
        graphics.fill(x, y, x + BOX_SIZE, y + BOX_SIZE, held ? HELD_FILL : IDLE_FILL);
        graphics.outline(x, y, BOX_SIZE, BOX_SIZE, OUTLINE_COLOR);
        int textWidth = client.font.width(label);
        int textX = x + (BOX_SIZE - textWidth) / 2;
        int textY = y + (BOX_SIZE - 8) / 2;
        graphics.text(client.font, label, textX, textY, held ? HELD_TEXT_COLOR : IDLE_TEXT_COLOR, false);
    }
}
