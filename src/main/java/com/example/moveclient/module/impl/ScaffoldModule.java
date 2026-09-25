package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
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
 * Deliberately only acts while your main hand already holds a {@link BlockItem} - it doesn't
 * silently switch your held hotbar slot to grab one, which would need verifying an additional,
 * separate slot-selection packet path. You just need a block selected while walking, the same way
 * you'd need one selected to place it by hand.
 */
public class ScaffoldModule extends Module {

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

        if (!(player.getMainHandItem().getItem() instanceof BlockItem)) {
            return;
        }

        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.1, player.getZ());
        BlockState stateBelow = level.getBlockState(below);
        if (!stateBelow.getCollisionShape(level, below).isEmpty()) {
            return; // already something solid to stand on
        }

        AirPlaceModule.place(client, player, InteractionHand.MAIN_HAND, below);
    }
}
