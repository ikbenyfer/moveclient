package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Places blocks even when nothing is actually in range to click against. Vanilla's own placement
 * logic doesn't require the clicked position to already contain a real block: when the block at
 * the clicked position is air (replaceable), the new block is placed directly there instead of
 * offset by the clicked face's direction - confirmed via {@code MultiPlayerGameMode#useItemOn}'s
 * real signature, which just takes a {@link BlockHitResult} with no requirement that it came from
 * a real raycast hit. This module's whole mechanism is building its own synthetic
 * {@code BlockHitResult} targeting empty air along your look direction, instead of relying on
 * {@link Minecraft#hitResult} - which is only ever a real {@code BLOCK} hit when you're already
 * looking at a real block, useless for placing where there's nothing.
 *
 * Only activates on a use-key press when vanilla's own crosshair target ISN'T already a real
 * block ({@code Minecraft#hitResult}'s type isn't {@code BLOCK}) and a hand holds a
 * {@link BlockItem}, so it never interferes with normal placement against real blocks - that
 * continues to go through vanilla's own unmodified input handling, completely untouched.
 */
public class AirPlaceModule extends Module {

    private final Setting.DoubleSetting range;

    private boolean wasUseDown;

    public AirPlaceModule() {
        super("AirPlace", "Places blocks even when nothing is in range to click against", ModuleCategory.WORLD);
        range = registerDouble("Range", 4.5, 2.0, 8.0, 0.5);
    }

    @Override
    protected void onDisable() {
        wasUseDown = false;
    }

    @Override
    protected void onTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            return;
        }

        boolean useDown = client.options.keyUse.isDown();
        boolean justPressed = useDown && !wasUseDown;
        wasUseDown = useDown;

        HitResult hitResult = client.hitResult;
        boolean alreadyHasRealTarget = hitResult instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK;
        if (!justPressed || alreadyHasRealTarget) {
            return;
        }

        InteractionHand hand = blockItemHand(player);
        if (hand == null) {
            return;
        }

        BlockPos targetPos = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(range.get())));
        place(client, player, hand, targetPos);
    }

    private static InteractionHand blockItemHand(LocalPlayer player) {
        if (player.getMainHandItem().getItem() instanceof BlockItem) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().getItem() instanceof BlockItem) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    static void place(Minecraft client, LocalPlayer player, InteractionHand hand, BlockPos targetPos) {
        Vec3 center = Vec3.atCenterOf(targetPos);
        BlockHitResult syntheticHit = new BlockHitResult(center, Direction.UP, targetPos, false);
        client.gameMode.useItemOn(player, hand, syntheticHit);
        player.swing(hand);
    }
}
