package com.example.moveclient.mixin;

import com.example.moveclient.xray.XrayOcclusionState;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.EmptyBlockModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Backs Xray's Allowlist Mode: instead of denylisting ~30 common terrain blocks (see
 * {@code resourcepacks/xray/}), this hides every block that ISN'T an ore by redirecting its
 * resolved render model to {@link EmptyBlockModel#INSTANCE} — vanilla's own "render nothing"
 * model. Scales to the whole game's block palette without enumerating hundreds of individual
 * resource-pack model files, which the denylist approach can't do.
 *
 * {@link BlockModelResolver#update} is this build's per-blockstate model resolution entry point
 * (the renamed/restructured replacement for the old {@code BlockRenderDispatcher} - verified via
 * {@code javap} against the real 26.2 jar, since no class of that name exists here). Injecting at
 * its head and cancelling when the block should be hidden is equivalent to vanilla resolving that
 * state to an empty model directly; the seed argument only affects which model *variant* multipart
 * random selection would pick, which is irrelevant for a model with no geometry.
 *
 * Only touches the render model - collision, light-blocking shape, and everything else about the
 * block are completely untouched, exactly like the denylist pack's {@code "elements": []} models.
 * The complementary face-culling problem (a neighboring ore block's face against a hidden
 * block never being added to the chunk mesh) is handled separately by {@code BlockOcclusionMixin}.
 *
 * Registered with {@code "required": false} in {@code moveclient.mixins.json}: if this mixin's
 * target doesn't match on some future build, Fabric logs a warning and the rest of the mod still
 * loads instead of failing to start.
 */
@Mixin(BlockModelResolver.class)
public abstract class XrayAllowlistModelMixin {

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void moveclient$hideNonOreBlocks(
            BlockModelRenderState state, BlockState blockState, BlockDisplayContext context, CallbackInfo ci) {
        if (XrayOcclusionState.isAllowlistActive() && !XrayOcclusionState.ORE_BLOCKS.contains(blockState.getBlock())) {
            EmptyBlockModel.INSTANCE.update(state, blockState, context, 0L);
            ci.cancel();
        }
    }
}
