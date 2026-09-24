package com.example.moveclient.module;

/**
 * A single configurable value exposed by a {@link Module}. The ClickGUI renders
 * one widget per setting, and {@code ConfigManager} persists the current value
 * under the owning module's JSON object. Callers that need persistence trigger it
 * explicitly (e.g. on button press / slider release) rather than on every mutation,
 * so dragging a slider doesn't hit disk every frame.
 */
public abstract class Setting<T> {

    private final String name;
    protected T value;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T get() {
        return value;
    }

    public static final class BooleanSetting extends Setting<Boolean> {
        public BooleanSetting(String name, boolean defaultValue) {
            super(name, defaultValue);
        }

        public void toggle() {
            value = !value;
        }

        public void set(boolean newValue) {
            value = newValue;
        }
    }

    public static final class DoubleSetting extends Setting<Double> {
        private final double min;
        private final double max;
        private final double step;

        public DoubleSetting(String name, double defaultValue, double min, double max, double step) {
            super(name, defaultValue);
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public double getStep() {
            return step;
        }

        public void set(double newValue) {
            double clamped = Math.max(min, Math.min(max, newValue));
            value = Math.round(clamped / step) * step;
        }
    }
}
