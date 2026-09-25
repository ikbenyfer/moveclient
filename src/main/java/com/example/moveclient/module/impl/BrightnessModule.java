package com.example.moveclient.module.impl;

import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/**
 * Locks the in-game brightness slider to its true maximum (gamma = 1.0, vanilla's own "Bright"
 * setting) while enabled, and restores whatever value it was set to before on disable.
 *
 * Deliberately doesn't try to push gamma past vanilla's own range: {@code OptionInstance.set}
 * routes through {@code OptionInstance$UnitDouble.validateValue}, which rejects - and silently
 * ignores, leaving the value unchanged - anything outside 0.0-1.0. Verified against this build's
 * actual bytecode rather than assumed, since some other clients push gamma far higher for a true
 * "see in the dark" fullbright, which requires a Mixin bypassing that validation entirely. This
 * module intentionally stays within the documented, validated option API instead: it's vanilla's
 * own legitimate maximum brightness, just applied automatically instead of via the options menu.
 */
public class BrightnessModule extends Module {

    private double previousGamma;

    public BrightnessModule() {
        super("Brightness", "Locks in-game brightness to its maximum value", ModuleCategory.WORLD);
    }

    @Override
    protected void onEnable() {
        OptionInstance<Double> gamma = Minecraft.getInstance().options.gamma();
        previousGamma = gamma.get();
        gamma.set(1.0);
    }

    @Override
    protected void onDisable() {
        Minecraft.getInstance().options.gamma().set(previousGamma);
    }

    @Override
    protected void onTick() {
        // Re-applied every tick in case something else (a resource pack, another mod, the vanilla
        // options screen) changes it back while this module is active.
        Minecraft.getInstance().options.gamma().set(1.0);
    }
}
