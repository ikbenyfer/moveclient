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
 * Jump height multiplier, applied as a reversible modifier on {@code Attributes.JUMP_STRENGTH}
 * (the same attribute vanilla's Jump Boost effect uses), so vanilla jump physics stay intact
 * and the effect is fully undone by removing the modifier on disable.
 */
public class HighJumpModule extends Module {

    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("moveclient", "jump_strength_modifier");

    private final Setting.DoubleSetting multiplier;

    public HighJumpModule() {
        super("HighJump", "Jump height multiplier", ModuleCategory.MOVEMENT);
        multiplier = registerDouble("Multiplier", 2.0, 1.0, 6.0, 0.1);
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
        return player == null ? null : player.getAttribute(Attributes.JUMP_STRENGTH);
    }
}
