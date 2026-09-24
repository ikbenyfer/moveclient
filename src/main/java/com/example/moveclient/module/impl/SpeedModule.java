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
 * Ground movement speed multiplier, applied as a reversible attribute modifier on
 * {@code Attributes.MOVEMENT_SPEED} rather than overwriting velocity directly, so it stacks
 * correctly with potion effects/enchantments and is fully undone by removing the modifier
 * on disable.
 */
public class SpeedModule extends Module {

    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("moveclient", "speed_modifier");

    private final Setting.DoubleSetting multiplier;

    public SpeedModule() {
        super("Speed", "Ground movement speed multiplier", ModuleCategory.MOVEMENT);
        multiplier = registerDouble("Multiplier", 1.5, 1.0, 5.0, 0.1);
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
        // Re-applied every tick so a live slider change takes effect immediately and the
        // modifier survives vanilla attribute recalculation (e.g. respawn).
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
        return player == null ? null : player.getAttribute(Attributes.MOVEMENT_SPEED);
    }
}
