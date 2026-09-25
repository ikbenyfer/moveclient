package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Equips any armor piece sitting unequipped in your hotbar/inventory into its matching, currently
 * empty armor slot - the automated version of shift-clicking it there yourself.
 *
 * Uses {@code ContainerInput.QUICK_MOVE} (the same click type a real shift-click sends) rather
 * than computing a target armor-slot menu index directly: vanilla's own {@code InventoryMenu}
 * quick-move logic already knows how to route an armor item to its matching empty slot, so this
 * only needs to identify the *source* slot, the same "let vanilla's own container logic do the
 * routing" approach {@code AutoTotemModule} uses for its offhand swap.
 *
 * An item's armor slot is read from its {@code DataComponents.EQUIPPABLE} component
 * ({@link Equippable#slot()}) - this build replaced the old {@code ArmorItem} class entirely with
 * this data-component system (verified: no class named {@code net.minecraft.world.item.ArmorItem}
 * exists anywhere in this build's jar), so an {@code instanceof ArmorItem} check would no longer
 * even compile, let alone work. Deliberately checks the slot is one of HEAD/CHEST/LEGS/FEET rather
 * than using {@code EquipmentSlot#isArmor()} - that also returns true for the animal-only
 * {@code BODY} slot type (horse/wolf/llama armor), which isn't meant for this module to touch.
 */
public class AutoArmorModule extends Module {

    private static final int INVENTORY_SEARCH_SIZE = 36; // hotbar (0-8) + main inventory (9-35)
    private static final int HOTBAR_SIZE = 9;
    private static final int MENU_HOTBAR_START = 36;

    private int cooldownTicks;

    public AutoArmorModule() {
        super("AutoArmor", "Auto-equips armor from your inventory into empty armor slots", ModuleCategory.PLAYER);
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

        int slot = findUnequippedArmorPiece(player);
        if (slot < 0) {
            return;
        }

        int menuSlot = slot < HOTBAR_SIZE ? MENU_HOTBAR_START + slot : slot;
        client.gameMode.handleContainerInput(
                player.inventoryMenu.containerId, menuSlot, 0, ContainerInput.QUICK_MOVE, player);

        // Wait for the server's inventory update before considering another piece - same
        // reasoning as AutoTotem: a slow round trip could otherwise cause a rapid re-trigger loop.
        cooldownTicks = 10;
    }

    private int findUnequippedArmorPiece(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < INVENTORY_SEARCH_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable == null || !isArmorSlot(equippable.slot())) {
                continue;
            }
            if (!player.getItemBySlot(equippable.slot()).isEmpty()) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }
}
