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
 * Raises step height via a reversible attribute modifier on {@code Attributes.STEP_HEIGHT},
 * letting the player walk up full blocks without jumping. Useful for testing terrain traversal
 * alongside the other movement modules.
 */
public class StepModule extends Module {

    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("moveclient", "step_height_modifier");

    private final Setting.DoubleSetting extraHeight;

    public StepModule() {
        super("Step", "Raises step height to walk up full blocks", ModuleCategory.MOVEMENT);
        extraHeight = registerDouble("Extra Height", 0.6, 0.0, 1.5, 0.05);
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
        attribute.addOrUpdateTransientModifier(
                new AttributeModifier(MODIFIER_ID, extraHeight.get(), AttributeModifier.Operation.ADD_VALUE));
    }

    private AttributeInstance attribute() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : player.getAttribute(Attributes.STEP_HEIGHT);
    }
}
