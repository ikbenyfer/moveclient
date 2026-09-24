package com.example.moveclient.gui.widget;

import com.example.moveclient.config.ConfigManager;
import com.example.moveclient.module.Setting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Text field bound to a {@link Setting.DoubleSetting}, for typing an exact value instead of
 * dragging the paired {@link SettingSlider}. Invalid/incomplete input (e.g. "-", "1.", "") is
 * simply ignored rather than applied, so the field doesn't fight the user mid-keystroke; only a
 * value that parses cleanly (and gets clamped/stepped by {@link Setting.DoubleSetting#set}) is
 * ever written to the setting.
 */
public class SettingNumberField extends EditBox {

    private final Setting.DoubleSetting setting;
    private Runnable onValueSynced = () -> { };

    public SettingNumberField(Font font, int x, int y, int width, int height, Setting.DoubleSetting setting) {
        super(font, x, y, width, height, Component.literal(setting.getName() + " value"));
        this.setting = setting;
        setMaxLength(16);
        setValue(format(setting.get()));
        setResponder(this::onTextChanged);
    }

    /** Registers a callback fired whenever a typed value is successfully applied (e.g. to move a paired slider). */
    public void setOnValueSynced(Runnable callback) {
        this.onValueSynced = callback;
    }

    /** Re-reads the setting's current value (e.g. after it was dragged on a paired slider) and redisplays it. */
    public void syncFromSetting() {
        setValue(format(setting.get()));
    }

    private void onTextChanged(String text) {
        try {
            double parsed = Double.parseDouble(text.trim());
            setting.set(parsed);
            onValueSynced.run();
            ConfigManager.save();
        } catch (NumberFormatException ignored) {
            // Incomplete/invalid input while typing; wait for a value that actually parses.
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
