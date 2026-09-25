package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Teleports you to wherever you're currently looking, up to Max Range - a classic client-side
 * "blink"/ClickTP. One-shot: pressing its bind performs the teleport once, then the module
 * immediately turns itself back off ({@link #onTick()} unconditionally disables on the very next
 * tick), so every press teleports - a normal persistent toggle would only fire on every *other*
 * press, since the second press would just turn it back off silently.
 *
 * Ray-marches your own look direction with {@link ClipContext} rather than using vanilla's
 * {@code Minecraft.hitResult} - that's capped to the short block-interaction reach distance, not
 * useful for teleporting any real distance. Stops at the first solid block along the ray and backs
 * off slightly so you land just in front of it instead of inside it; with a clear line of sight
 * the whole way, you land at the full Max Range in open air.
 *
 * Purely a client-side position set ({@link net.minecraft.world.entity.Entity#teleportTo(double,
 * double, double)}) - no server command, no special packet. Like {@code NoFallModule}'s
 * server-side fall-distance reset, this is only authoritative in singleplayer/LAN (the integrated
 * server trusts the client's own reported position); a real remote server's anti-cheat will very
 * likely reject or snap back a position jump this large. Doesn't check whether the destination has
 * room for the player - landing partway stuck inside terrain is possible if the spot isn't clear.
 */
public class TeleportModule extends Module {

    private final Setting.DoubleSetting maxRange;

    public TeleportModule() {
        super("Teleport", "Teleports you to wherever you're looking, up to Max Range", ModuleCategory.MOVEMENT);
        maxRange = registerDouble("Max Range", 128, 8, 512, 8);
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 target = eye.add(look.scale(maxRange.get()));

        BlockHitResult hit = level.clip(new ClipContext(
                eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 destination = hit.getType() == HitResult.Type.BLOCK
                ? hit.getLocation().subtract(look.scale(0.3))
                : target;

        player.teleportTo(destination.x, destination.y, destination.z);
    }

    @Override
    protected void onTick() {
        // One-shot action, not a persistent state - see the class doc.
        setEnabled(false);
    }
}
