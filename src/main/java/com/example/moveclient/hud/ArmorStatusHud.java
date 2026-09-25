package com.example.moveclient.hud;

import com.example.moveclient.module.ModuleManager;
import com.example.moveclient.module.impl.ArmorHudModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a row of worn-armor item icons, bottom-right, while the ArmorHUD module is enabled.
 * {@code GuiGraphicsExtractor#itemDecorations} draws the same durability bar/stack count overlay
 * vanilla's own inventory screen draws for a stack, so an item's wear is shown without this class
 * needing to compute or render a bar itself.
 */
public final class ArmorStatusHud {

    private static final int ICON_SIZE = 18;
    private static final int MARGIN = 4;
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        if (!(ModuleManager.getInstance().getByName("ArmorHUD") instanceof ArmorHudModule armorHud) || !armorHud.isEnabled()) {
            return;
        }

        int x = graphics.guiWidth() - MARGIN - ICON_SIZE * SLOTS.length;
        int y = graphics.guiHeight() - MARGIN - ICON_SIZE;

        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                graphics.item(stack, x, y);
                graphics.itemDecorations(client.font, stack, x, y);
            }
            x += ICON_SIZE;
        }
    }
}
