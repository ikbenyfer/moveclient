package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Automatically places a block directly beneath your feet each tick when there's nothing solid
 * there, so walking over open air bridges it as you go - the classic "scaffold" building assist.
 *
 * Reuses {@link AirPlaceModule#place}, the same synthetic-{@code BlockHitResult} placement
 * technique, targeting the position directly below the player instead of one along the look ray:
 * the block being walked off of isn't guaranteed to be a usable adjacent face to click against,
 * but - as documented on {@code AirPlaceModule} - vanilla's own placement logic places directly at
 * the clicked position when it's air/replaceable rather than requiring a real neighbor at all.
 *
 * Your main hand always takes priority: if it already holds a {@link BlockItem}, that's what gets
 * placed. Only when it doesn't does this search the hotbar (indices 0-8, not the full 36-slot
 * inventory - a slot outside the hotbar can't be "held" without first moving it there, which would
 * mean rearranging your inventory mid-walk) for the first block it finds, briefly switching the
 * selected slot to it, placing, then switching back. The slot switch is sent to the server for
 * real ({@code ServerboundSetCarriedItemPacket}, the same packet vanilla sends when you scroll the
 * hotbar or press a number key) rather than only updated locally, since the server needs to agree
 * on which item is selected to accept the placement - so this does cause a brief, real one-tick
 * flicker of your held item, not an invisible swap.
 */
public class ScaffoldModule extends Module {

    private static final int HOTBAR_SIZE = 9;

    public ScaffoldModule() {
        super("Scaffold", "Auto-places a block under your feet as you walk over air", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null || client.gameMode == null || player.getAbilities().flying) {
            return;
        }

        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.1, player.getZ());
        BlockState stateBelow = level.getBlockState(below);
        if (!stateBelow.getCollisionShape(level, below).isEmpty()) {
            return; // already something solid to stand on
        }

        if (player.getMainHandItem().getItem() instanceof BlockItem) {
            AirPlaceModule.place(client, player, InteractionHand.MAIN_HAND, below);
            return;
        }

        Inventory inventory = player.getInventory();
        int hotbarBlockSlot = findHotbarBlockItem(inventory);
        if (hotbarBlockSlot < 0) {
            return;
        }

        int originalSlot = inventory.getSelectedSlot();
        selectSlot(player, hotbarBlockSlot);
        AirPlaceModule.place(client, player, InteractionHand.MAIN_HAND, below);
        selectSlot(player, originalSlot);
    }

    private static int findHotbarBlockItem(Inventory inventory) {
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (inventory.getItem(i).getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    private static void selectSlot(LocalPlayer player, int slot) {
        player.getInventory().setSelectedSlot(slot);
        player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }
}
