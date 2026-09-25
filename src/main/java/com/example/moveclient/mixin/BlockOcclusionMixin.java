package com.example.moveclient.mixin;

import com.example.moveclient.xray.XrayOcclusionState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes a block's face render normally even when it's fully buried against one of Xray's
 * covered blocks (which itself renders as nothing — see {@code resourcepacks/xray/}).
 *
 * Without this, {@code Block.shouldRenderFace(state, adjacentState, direction)} — which decides
 * whether a face is even added to the chunk mesh at all — still treats a covered block (e.g.
 * stone) as a fully opaque occluder, because that decision is driven by the block's actual,
 * server-shared shape/behavior, not by what its resource-pack model visually shows. A resource
 * pack alone cannot change this: it was confirmed (via a diagnostic build, then this project's
 * own reasoning about {@code Block.shouldRenderFace}'s bytecode) to be why ore veins with no face
 * touching air/an already-exposed block — i.e. anything not already partially mined into — never
 * rendered no matter which of the three prior resource-pack-only Xray techniques was used.
 *
 * Also covers Allowlist Mode ({@link XrayOcclusionState#isAllowlistActive()}): the same face-
 * culling problem applies there too, just against a much larger set of adjacent blocks (anything
 * that isn't an ore, rather than one fixed "common terrain" list) — see
 * {@code XrayAllowlistModelMixin}, which hides those blocks' geometry itself but can't fix the
 * mesh-building decision for a *neighboring* ore block's faces.
 *
 * Deliberately narrow: only intercepts the decision when one of Xray's hide modes is actually
 * active and only when the specific *adjacent* block is one that mode hides, so this has zero
 * effect on anything else — including all normal rendering whenever the module is off. Registered
 * with {@code "required": false} in
 * {@code moveclient.mixins.json}: if this mixin fails to find its target on some future game
 * version, Fabric logs a warning and the rest of the mod still loads, instead of the whole mod
 * failing to start.
 */
@Mixin(Block.class)
public abstract class BlockOcclusionMixin {

    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void moveclient$alwaysRenderAgainstCoveredBlocks(
            BlockState state, BlockState adjacentState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (XrayOcclusionState.isActive() && XrayOcclusionState.COVERED_BLOCKS.contains(adjacentState.getBlock())) {
            cir.setReturnValue(true);
            return;
        }
        if (XrayOcclusionState.isAllowlistActive() && !XrayOcclusionState.ORE_BLOCKS.contains(adjacentState.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}
