package com.example.moveclient.gui.widget;

import com.example.moveclient.config.ConfigManager;
import com.example.moveclient.module.Setting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Slider widget bound to a {@link com.example.moveclient.module.Setting.DoubleSetting}. */
public class SettingSlider extends AbstractSliderButton {

    private final Setting.DoubleSetting setting;
    private Runnable onValueSynced = () -> { };

    public SettingSlider(int x, int y, int width, int height, Setting.DoubleSetting setting) {
        super(x, y, width, height, Component.literal(formatLabel(setting)), normalize(setting));
        this.setting = setting;
    }

    /** Registers a callback fired whenever this slider changes the setting's value (e.g. to keep a paired number field in sync). */
    public void setOnValueSynced(Runnable callback) {
        this.onValueSynced = callback;
    }

    /** Re-reads the setting's current value (e.g. after it was typed into a paired number field) and repositions the handle. */
    public void syncFromSetting() {
        this.value = normalize(setting);
        updateMessage();
    }

    private static double normalize(Setting.DoubleSetting setting) {
        double range = setting.getMax() - setting.getMin();
        if (range <= 0) {
            return 0;
        }
        return (setting.get() - setting.getMin()) / range;
    }

    private static String formatLabel(Setting.DoubleSetting setting) {
        return setting.getName() + ": " + String.format(Locale.ROOT, "%.2f", setting.get());
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(formatLabel(setting)));
    }

    @Override
    protected void applyValue() {
        double range = setting.getMax() - setting.getMin();
        setting.set(setting.getMin() + this.value * range);
        onValueSynced.run();
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        super.onRelease(event);
        ConfigManager.save();
    }
}
