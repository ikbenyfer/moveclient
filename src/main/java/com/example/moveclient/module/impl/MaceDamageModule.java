package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * Adds bonus attack damage while your main hand holds a Mace, via a reversible {@code ADD_VALUE}
 * modifier on {@code Attributes.ATTACK_DAMAGE} - the same attribute vanilla's own smash-attack
 * fall-distance bonus stacks on top of.
 *
 * Unlike Speed/HighJump/Gravity/Reach (which affect purely client-simulated physics that the
 * client then just reports to the server), actually dealt damage is resolved SERVER-SIDE from the
 * server's own copy of this attribute - a client-only modifier here would only change the
 * client's own damage prediction/tooltip number, not what really gets dealt to anything. So,
 * mirroring {@code NoFallModule}'s approach, this module ALSO reaches into the integrated
 * singleplayer/LAN server's own {@link ServerPlayer} copy (via
 * {@link Minecraft#getSingleplayerServer()} + {@link MinecraftServer#execute}) and applies the
 * same modifier there, which is genuinely authoritative. Against a real remote dedicated server
 * this only affects the client-side prediction, not real dealt damage - the same honest
 * limitation NoFall documents.
 */
public class MaceDamageModule extends Module {

    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("moveclient", "mace_damage");

    private final Setting.DoubleSetting bonusDamage;

    public MaceDamageModule() {
        super("MaceDamage", "Bonus attack damage while holding a Mace", ModuleCategory.PLAYER);
        bonusDamage = registerDouble("Bonus Damage", 5.0, 0.0, 50.0, 0.5);
    }

    @Override
    protected void onDisable() {
        LocalPlayer player = Minecraft.getInstance().player;
        removeModifier(player);
        withServerPlayer(this::removeModifier);
    }

    @Override
    protected void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        boolean holdingMace = player.getMainHandItem().getItem() == Items.MACE;
        Consumer<Player> action = holdingMace ? this::applyModifier : this::removeModifier;
        action.accept(player);
        withServerPlayer(action);
    }

    private void applyModifier(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute != null) {
            attribute.addOrUpdateTransientModifier(
                    new AttributeModifier(MODIFIER_ID, bonusDamage.get(), AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private void removeModifier(Player player) {
        if (player == null) {
            return;
        }
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute != null) {
            attribute.removeModifier(MODIFIER_ID);
        }
    }

    private void withServerPlayer(Consumer<Player> action) {
        LocalPlayer player = Minecraft.getInstance().player;
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (player == null || server == null) {
            return;
        }
        java.util.UUID uuid = player.getUUID();
        server.execute(() -> {
            PlayerList playerList = server.getPlayerList();
            if (playerList == null) {
                return;
            }
            ServerPlayer serverPlayer = playerList.getPlayer(uuid);
            if (serverPlayer != null) {
                action.accept(serverPlayer);
            }
        });
    }
}
