package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

/**
 * When health drops to a configurable threshold, swaps a Totem of Undying from anywhere in the
 * inventory (hotbar or main inventory) directly into the offhand.
 *
 * This calls {@code MultiPlayerGameMode.handleContainerInput(containerId, slotId, button,
 * ContainerInput.SWAP, player)} with {@code button = Inventory.SLOT_OFFHAND} (40) — verified
 * against this build's actual {@code AbstractContainerMenu.clicked} bytecode, which only accepts
 * a SWAP button of 0-8 (hotbar) or exactly 40 (offhand). That's the same client method and the
 * same slot-swap operation vanilla's own inventory screen performs when you hover a slot and
 * press F, so this applies the swap locally and sends the real packet, the intended way, rather
 * than reimplementing container networking from scratch. The slot-number mapping from the
 * player's own {@code Inventory} (0-8 hotbar, 9-35 main) to the container menu's slot numbering
 * (9-35 main, 36-44 hotbar, 45 offhand) is the long-standing vanilla layout, also confirmed
 * against this build's {@code InventoryMenu} slot-range constants before relying on it.
 */
public class AutoTotemModule extends Module {

    private static final int INVENTORY_SEARCH_SIZE = 36; // hotbar (0-8) + main inventory (9-35)
    private static final int HOTBAR_SIZE = 9;
    private static final int MENU_HOTBAR_START = 36;

    private final Setting.DoubleSetting healthThreshold;

    private int cooldownTicks;

    public AutoTotemModule() {
        super("AutoTotem", "Swaps a totem from anywhere in your inventory to offhand when low on health", ModuleCategory.PLAYER);
        healthThreshold = registerDouble("Health Threshold", 6.0, 1.0, 18.0, 1.0);
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
        if (player == null || client.gameMode == null || player.getHealth() > healthThreshold.get()) {
            return;
        }

        if (player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) {
            return;
        }

        int inventoryIndex = findTotem(player.getInventory());
        if (inventoryIndex < 0) {
            return;
        }

        int menuSlot = inventoryIndex < HOTBAR_SIZE ? MENU_HOTBAR_START + inventoryIndex : inventoryIndex;
        client.gameMode.handleContainerInput(
                player.inventoryMenu.containerId, menuSlot, Inventory.SLOT_OFFHAND, ContainerInput.SWAP, player);

        // Wait for the server's inventory update to come back before considering another swap,
        // so a slow round trip can't cause a rapid swap/swap-back loop that undoes itself.
        cooldownTicks = 10;
    }

    private static int findTotem(Inventory inventory) {
        for (int i = 0; i < INVENTORY_SEARCH_SIZE; i++) {
            if (inventory.getItem(i).getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        return -1;
    }
}
