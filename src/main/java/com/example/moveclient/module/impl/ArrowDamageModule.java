package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.List;
import java.util.UUID;

/**
 * Overrides the damage of every arrow you personally fire (bow or crossbow) to a fixed value,
 * instead of vanilla's own draw-charge/Power-enchantment-scaled damage.
 *
 * {@code AbstractArrow} only exposes {@code setBaseDamage(double)}, not a getter for whatever
 * value vanilla already computed for a given shot (verified against this build's actual API
 * surface - {@code net.minecraft.world.entity.projectile.arrow.AbstractArrow}, moved from its old
 * package in this build), so this can't add a bonus on top of that the way
 * {@code MaceDamageModule} adds a bonus on top of vanilla's own attack-damage calculation - it can
 * only overwrite it outright. Every fired arrow ends up dealing exactly the configured amount, no
 * more, no less, regardless of how fully the bow was drawn or whether the bow/arrow had Power.
 * Re-applied every tick to every one of your own in-flight arrows (a plain overwrite, so repeating
 * it is harmless - no compounding).
 *
 * Like {@code MaceDamageModule}, arrow damage is resolved server-side, so this only has a real
 * effect when hosting the integrated singleplayer/LAN server (reached via
 * {@link Minecraft#getSingleplayerServer()} + {@link MinecraftServer#execute}, the same technique
 * {@code NoFallModule} uses); against a remote dedicated server this module has no effect at all,
 * since it never touches anything client-side.
 */
public class ArrowDamageModule extends Module {

    private final Setting.DoubleSetting damage;

    public ArrowDamageModule() {
        super("ArrowDamage", "Overrides the damage of arrows you fire", ModuleCategory.PLAYER);
        damage = registerDouble("Damage", 6.0, 1.0, 50.0, 0.5);
    }

    @Override
    protected void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (player == null || server == null) {
            return;
        }

        UUID uuid = player.getUUID();
        double targetDamage = damage.get();
        server.execute(() -> {
            PlayerList playerList = server.getPlayerList();
            if (playerList == null) {
                return;
            }
            ServerPlayer serverPlayer = playerList.getPlayer(uuid);
            if (serverPlayer == null) {
                return;
            }
            ServerLevel level = (ServerLevel) serverPlayer.level();
            List<? extends AbstractArrow> arrows = level.getEntities(
                    EntityTypeTest.forClass(AbstractArrow.class), arrow -> arrow.getOwner() == serverPlayer);
            for (AbstractArrow arrow : arrows) {
                arrow.setBaseDamage(targetDamage);
            }
        });
    }
}
