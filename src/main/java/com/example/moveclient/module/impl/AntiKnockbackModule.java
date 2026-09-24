package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Local movement simulation that neutralizes externally applied velocity (knockback, explosions,
 * projectile hits) in both the horizontal and vertical axes.
 *
 * There is no way to reach literal zero tolerance without either (a) fully replicating vanilla's
 * friction/acceleration formula to compute exactly what velocity the player's own input should
 * have produced, or (b) intercepting the incoming knockback packet directly. (b) needs a Mixin
 * into networking code whose exact target this build's API churn made too risky to guess at
 * (see the other modules' class docs for concrete examples of that churn); (a) is a much larger
 * undertaking with its own correctness risk (getting vanilla's block-friction/sprint-jump/water
 * physics subtly wrong would make ordinary movement feel broken, not just let some knockback
 * through). So this stays a tolerance-based comparison against a self-maintained "trusted"
 * baseline, but tightened as far as it can go without false-positiving on the game's own
 * legitimate per-tick velocity changes:
 *
 * - Horizontal: tolerance is only as large as the small acceleration ordinary WASD input can
 *   produce in one tick (real knockback, ~0.4+, is several times that).
 * - Vertical: rather than a flat threshold, the expected value is computed each tick (current
 *   vertical velocity minus the player's actual gravity attribute while airborne, or the actual
 *   jump-strength attribute right as you leave the ground) and only a genuine mismatch against
 *   that prediction is corrected — so normal falling and normal/boosted jumping (including
 *   HighJump) are unaffected, while anything that doesn't fit that model (an explosion, a punch
 *   launching you up, an arrow of knockback) gets cancelled almost immediately regardless of size.
 *   Known edge case: a slime block bounce looks like an external upward launch from this model's
 *   perspective too, and will get cancelled along with real knock-up while this is enabled.
 */
public class AntiKnockbackModule extends Module {

    private final Setting.DoubleSetting horizontalTolerance;
    private final Setting.BooleanSetting protectVertical;
    private final Setting.DoubleSetting verticalTolerance;

    private Vec3 trustedVelocity = Vec3.ZERO;
    private boolean hasTrustedVelocity;
    private boolean wasOnGround = true;

    private final ClientTickEvents.EndTick endHandler = this::onEndTick;
    private boolean listenerRegistered;

    public AntiKnockbackModule() {
        super("Anti-Knockback", "Cancels externally applied velocity via local movement prediction", ModuleCategory.PLAYER);
        horizontalTolerance = registerDouble("Horizontal Tolerance", 0.08, 0.0, 2.0, 0.01);
        protectVertical = registerBoolean("Protect Vertical", true);
        verticalTolerance = registerDouble("Vertical Tolerance", 0.1, 0.0, 2.0, 0.01);
    }

    @Override
    protected void onEnable() {
        if (!listenerRegistered) {
            ClientTickEvents.END_CLIENT_TICK.register(endHandler);
            listenerRegistered = true;
        }
        hasTrustedVelocity = false;
        wasOnGround = true;
    }

    @Override
    protected void onDisable() {
        hasTrustedVelocity = false;
    }

    @Override
    protected void onTick() {
        // All correction happens in the END_CLIENT_TICK handler below (registered once, always
        // active while enabled), so it runs on every real tick boundary regardless of when the
        // dispatcher in Module/ModuleManager happens to invoke onTick().
    }

    private void onEndTick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }
        LocalPlayer player = client.player;
        if (player == null) {
            hasTrustedVelocity = false;
            return;
        }

        boolean onGround = player.onGround();
        Vec3 current = player.getDeltaMovement();

        if (!hasTrustedVelocity) {
            trustedVelocity = current;
            hasTrustedVelocity = true;
            wasOnGround = onGround;
            return;
        }

        double preHorizSpeed = Math.hypot(trustedVelocity.x, trustedVelocity.z);
        double curHorizSpeed = Math.hypot(current.x, current.z);
        double newX = current.x;
        double newZ = current.z;
        if (curHorizSpeed - preHorizSpeed > horizontalTolerance.get()) {
            newX = trustedVelocity.x;
            newZ = trustedVelocity.z;
        }

        double newY = current.y;
        if (protectVertical.get()) {
            boolean justJumped = wasOnGround && !onGround && current.y > 0;
            double expectedY;
            if (justJumped) {
                expectedY = player.getAttributeValue(Attributes.JUMP_STRENGTH);
            } else if (onGround) {
                expectedY = trustedVelocity.y;
            } else {
                expectedY = trustedVelocity.y - player.getAttributeValue(Attributes.GRAVITY);
            }
            if (Math.abs(current.y - expectedY) > verticalTolerance.get()) {
                newY = expectedY;
            }
        }

        if (newX != current.x || newY != current.y || newZ != current.z) {
            player.setDeltaMovement(newX, newY, newZ);
        }

        trustedVelocity = new Vec3(newX, newY, newZ);
        wasOnGround = onGround;
    }
}
