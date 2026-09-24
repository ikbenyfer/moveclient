package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Scales fall/gravity acceleration via a reversible modifier on {@code Attributes.GRAVITY} (the
 * same attribute vanilla's Slow Falling effect uses), for testing how movement mechanics feel
 * under lighter or heavier gravity without touching Flight's no-gravity behavior.
 */
public class GravityModule extends Module {

    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("moveclient", "gravity_modifier");

    private final Setting.DoubleSetting multiplier;

    public GravityModule() {
        super("Gravity", "Scales fall acceleration (below 1.0 = floaty, above 1.0 = heavy)", ModuleCategory.MOVEMENT);
        multiplier = registerDouble("Multiplier", 0.5, 0.05, 3.0, 0.05);
    }

    @Override
    protected void onEnable() {
        applyModifier();
    }

    @Override
    protected void onDisable() {
        AttributeInstance attribute = attribute();
        if (attribute != null) {
            attribute.removeModifier(MODIFIER_ID);
        }
    }

    @Override
    protected void onTick() {
        applyModifier();
    }

    private void applyModifier() {
        AttributeInstance attribute = attribute();
        if (attribute == null) {
            return;
        }
        double amount = multiplier.get() - 1.0;
        attribute.addOrUpdateTransientModifier(
                new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private AttributeInstance attribute() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : player.getAttribute(Attributes.GRAVITY);
    }
}
