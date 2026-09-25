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
 * Extends how far you can break/place blocks and interact with entities, via reversible attribute
 * modifiers on {@code Attributes.BLOCK_INTERACTION_RANGE} / {@code ENTITY_INTERACTION_RANGE} - the
 * same real, vanilla-native attributes the game's own interaction-range checks read (added in the
 * same attribute-driven rework that replaced the old hardcoded reach-distance constants), rather
 * than anything hooked into the interaction code itself. Uses {@code ADD_VALUE} (a flat bonus on
 * top of the vanilla base range, same pattern {@code SpeedModule} uses for movement speed) so it
 * stacks correctly with anything else touching the same attribute and is fully undone by removing
 * the modifier on disable.
 */
public class ReachModule extends Module {

    private static final Identifier BLOCK_MODIFIER_ID = Identifier.fromNamespaceAndPath("moveclient", "reach_block");
    private static final Identifier ENTITY_MODIFIER_ID = Identifier.fromNamespaceAndPath("moveclient", "reach_entity");

    private final Setting.DoubleSetting blockBonus;
    private final Setting.DoubleSetting entityBonus;

    public ReachModule() {
        super("Reach", "Extends block and entity interaction range", ModuleCategory.PLAYER);
        blockBonus = registerDouble("Block Reach Bonus", 3.0, 0.0, 15.0, 0.5);
        entityBonus = registerDouble("Entity Reach Bonus", 3.0, 0.0, 15.0, 0.5);
    }

    @Override
    protected void onEnable() {
        applyModifiers();
    }

    @Override
    protected void onDisable() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        AttributeInstance block = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (block != null) {
            block.removeModifier(BLOCK_MODIFIER_ID);
        }
        AttributeInstance entity = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (entity != null) {
            entity.removeModifier(ENTITY_MODIFIER_ID);
        }
    }

    @Override
    protected void onTick() {
        // Re-applied every tick so a live slider change takes effect immediately and the
        // modifier survives vanilla attribute recalculation (e.g. respawn) - same reasoning as
        // SpeedModule.
        applyModifiers();
    }

    private void applyModifiers() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        AttributeInstance block = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (block != null) {
            block.addOrUpdateTransientModifier(
                    new AttributeModifier(BLOCK_MODIFIER_ID, blockBonus.get(), AttributeModifier.Operation.ADD_VALUE));
        }
        AttributeInstance entity = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (entity != null) {
            entity.addOrUpdateTransientModifier(
                    new AttributeModifier(ENTITY_MODIFIER_ID, entityBonus.get(), AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
