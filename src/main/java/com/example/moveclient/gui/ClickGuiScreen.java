package com.example.moveclient.gui;

import com.example.moveclient.config.ConfigManager;
import com.example.moveclient.gui.widget.SettingNumberField;
import com.example.moveclient.gui.widget.SettingSlider;
import com.example.moveclient.module.Module;
import com.example.moveclient.module.ModuleCategory;
import com.example.moveclient.module.ModuleManager;
import com.example.moveclient.module.Setting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-game ClickGUI: a left-hand, multi-column list of every module grouped by category (click to
 * toggle, "Cfg" to inspect settings, hover for a description) and a right-hand panel showing the
 * selected module's settings (a slider plus a typeable number field per value). The whole panel
 * can be dragged by its header, and content scrolls with the mouse wheel once it exceeds a fixed
 * viewport height. The game keeps running behind it so movement modules can be tuned live.
 *
 * Scrolling is implemented by toggling {@code AbstractWidget#visible} on rows that fall outside
 * the viewport, not by {@code GuiGraphicsExtractor#enableScissor}: a bad scissor call previously
 * crashed this client ("Scissor size must be >0") in a way that couldn't be caught, so that API
 * is deliberately avoided here.
 */
public class ClickGuiScreen extends Screen {

    private static final int MAX_ROWS_PER_COLUMN = 7;
    private static final int ROW_HEIGHT = 20;
    private static final int TOGGLE_WIDTH = 104;
    private static final int CFG_WIDTH = 20;
    private static final int GAP = 3;
    private static final int COLUMN_SPACING = 14;
    private static final int COLUMN_WIDTH = TOGGLE_WIDTH + GAP + CFG_WIDTH + COLUMN_SPACING;
    private static final int HEADER_HEIGHT = 18;
    private static final int PADDING = 8;
    private static final int CATEGORY_GAP = 8;
    private static final int SLIDER_WIDTH = 128;
    private static final int NUMBER_FIELD_WIDTH = 54;
    private static final int SETTINGS_PANEL_WIDTH = SLIDER_WIDTH + GAP + NUMBER_FIELD_WIDTH;
    private static final int CLOSE_BUTTON_SIZE = 14;
    private static final int FOOTER_HEIGHT = 14;
    private static final int MAX_CONTENT_HEIGHT = 170;
    private static final int SCROLL_STEP = ROW_HEIGHT;

    private static final int COLOR_BACKGROUND = 0xC8101014;
    private static final int COLOR_HEADER = 0xF0202535;
    private static final int COLOR_BORDER = 0xFF3C4A5A;
    private static final int COLOR_DIVIDER = 0xFF303844;

    // Top-left of the whole panel; shifts when the header is dragged.
    private int originX = 16;
    private int originY = 16;
    private boolean draggingPanel;

    // Fixed relative layout, computed once in init() and reused whenever the panel is dragged
    // or the settings panel is rebuilt, so everything stays anchored to (originX, originY).
    private int panelWidth;
    private int panelHeight;
    private int rightXOffset;
    private int settingsTopOffset;
    private int contentTopOffset;

    // Vertical scroll state, shared by the module list and the settings panel.
    private int scrollOffset;
    private int maxScroll;
    private int leftNaturalHeight;
    private int rightNaturalHeight;

    private Module selectedModule;
    private final List<AbstractWidget> settingWidgets = new ArrayList<>();
    private Button closeButton;
    // Non-null while waiting for the next key/mouse press to become that module's keybind; the
    // capture happens in keyPressed()/mouseClicked() below rather than through a normal widget.
    private Module bindingTarget;
    private Button keybindButton;
    // Hover state is read from the button itself (AbstractWidget#isHovered) rather than via the
    // native Tooltip widget: tooltip rendering does its own deferred hover-box clipping, which is
    // exactly the kind of code path that crashed with "Scissor size must be >0" on this client, so
    // descriptions are drawn as a plain status line instead.
    private final Map<Module, Button> moduleButtons = new LinkedHashMap<>();

    public ClickGuiScreen() {
        super(Component.literal("Movement Test Client"));
    }

    @Override
    protected void init() {
        moduleButtons.clear();
        scrollOffset = 0;
        int leftAreaWidth = 0;

        contentTopOffset = HEADER_HEIGHT + PADDING;
        int categoryYOffset = contentTopOffset;

        for (ModuleCategory category : ModuleCategory.values()) {
            List<Module> inCategory = modulesIn(category);
            if (inCategory.isEmpty()) {
                continue;
            }

            int columns = (inCategory.size() + MAX_ROWS_PER_COLUMN - 1) / MAX_ROWS_PER_COLUMN;
            int rowsInTallestColumn = Math.min(inCategory.size(), MAX_ROWS_PER_COLUMN);
            leftAreaWidth = Math.max(leftAreaWidth, columns * COLUMN_WIDTH);

            int headerY = categoryYOffset;
            for (int i = 0; i < inCategory.size(); i++) {
                int column = i / MAX_ROWS_PER_COLUMN;
                int row = i % MAX_ROWS_PER_COLUMN;
                int x = originX + PADDING + column * COLUMN_WIDTH;
                int y = originY + headerY + 12 + row * ROW_HEIGHT;
                addModuleRow(inCategory.get(i), x, y);
            }

            categoryYOffset += 12 + rowsInTallestColumn * ROW_HEIGHT + CATEGORY_GAP;
        }
        leftNaturalHeight = categoryYOffset - CATEGORY_GAP - contentTopOffset;

        rightXOffset = PADDING + leftAreaWidth + PADDING;
        settingsTopOffset = contentTopOffset + 14;
        rightNaturalHeight = 0;

        panelWidth = rightXOffset + SETTINGS_PANEL_WIDTH + PADDING;
        panelHeight = contentTopOffset + Math.min(Math.max(leftNaturalHeight, 20), MAX_CONTENT_HEIGHT) + PADDING + FOOTER_HEIGHT;

        closeButton = Button.builder(Component.literal("X"), button -> onClose())
                .bounds(originX + panelWidth - PADDING - CLOSE_BUTTON_SIZE, originY + (HEADER_HEIGHT - CLOSE_BUTTON_SIZE) / 2,
                        CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);

        if (selectedModule != null) {
            rebuildSettingsPanel();
        }

        recomputeScrollBounds();
    }

    private static List<Module> modulesIn(ModuleCategory category) {
        List<Module> result = new ArrayList<>();
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return result;
    }

    private void addModuleRow(Module module, int x, int y) {
        Button toggleButton = Button.builder(rowLabel(module), button -> {
            module.toggle();
            button.setMessage(rowLabel(module));
            ConfigManager.save();
        }).bounds(x, y - scrollOffset, TOGGLE_WIDTH, ROW_HEIGHT - 2).build();
        addRenderableWidget(toggleButton);
        moduleButtons.put(module, toggleButton);

        // Always offered, even for modules with no tunable settings: it's also where the keybind
        // picker lives now.
        Button cfgButton = Button.builder(Component.literal("Cfg"), button -> {
            selectedModule = module;
            bindingTarget = null;
            rebuildSettingsPanel();
        }).bounds(x + TOGGLE_WIDTH + GAP, y - scrollOffset, CFG_WIDTH, ROW_HEIGHT - 2).build();
        addRenderableWidget(cfgButton);
    }

    private static Component rowLabel(Module module) {
        ChatFormatting color = module.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED;
        String prefix = module.isEnabled() ? "[ON] " : "[OFF] ";
        return Component.literal(prefix + module.getName()).withStyle(color);
    }

    private void rebuildSettingsPanel() {
        for (AbstractWidget widget : settingWidgets) {
            removeWidget(widget);
        }
        settingWidgets.clear();

        keybindButton = null;
        if (selectedModule == null) {
            rightNaturalHeight = 0;
            recomputeScrollBounds();
            return;
        }

        int x = originX + rightXOffset;
        int y = originY + settingsTopOffset - scrollOffset;

        Module boundModule = selectedModule;
        keybindButton = Button.builder(keybindLabel(boundModule), button -> {
            bindingTarget = boundModule;
            button.setMessage(Component.literal("Press a key... (Esc = unbind)"));
        }).bounds(x, y, SETTINGS_PANEL_WIDTH, ROW_HEIGHT - 2).build();
        addRenderableWidget(keybindButton);
        settingWidgets.add(keybindButton);
        y += ROW_HEIGHT;

        for (Setting<?> setting : selectedModule.getSettings()) {
            if (setting instanceof Setting.DoubleSetting doubleSetting) {
                SettingSlider slider = new SettingSlider(x, y, SLIDER_WIDTH, ROW_HEIGHT - 2, doubleSetting);
                SettingNumberField field = new SettingNumberField(font, x + SLIDER_WIDTH + GAP, y, NUMBER_FIELD_WIDTH, ROW_HEIGHT - 2, doubleSetting);
                slider.setOnValueSynced(field::syncFromSetting);
                field.setOnValueSynced(slider::syncFromSetting);
                addRenderableWidget(slider);
                addRenderableWidget(field);
                settingWidgets.add(slider);
                settingWidgets.add(field);
            } else if (setting instanceof Setting.BooleanSetting booleanSetting) {
                Button toggle = Button.builder(boolLabel(booleanSetting), button -> {
                    booleanSetting.toggle();
                    button.setMessage(boolLabel(booleanSetting));
                    ConfigManager.save();
                }).bounds(x, y, SETTINGS_PANEL_WIDTH, ROW_HEIGHT - 2).build();
                addRenderableWidget(toggle);
                settingWidgets.add(toggle);
            } else {
                continue;
            }
            y += ROW_HEIGHT;
        }

        rightNaturalHeight = (1 + selectedModule.getSettings().size()) * ROW_HEIGHT;
        recomputeScrollBounds();
    }

    private static Component boolLabel(Setting.BooleanSetting setting) {
        ChatFormatting color = setting.get() ? ChatFormatting.GREEN : ChatFormatting.RED;
        return Component.literal(setting.getName() + ": " + (setting.get() ? "ON" : "OFF")).withStyle(color);
    }

    private static Component keybindLabel(Module module) {
        KeyMapping mapping = module.getKeyMapping();
        String keyName = mapping == null ? "unbound" : mapping.getTranslatedKeyMessage().getString();
        return Component.literal("Key: " + keyName);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(originX, originY, originX + panelWidth, originY + panelHeight, COLOR_BACKGROUND);
        graphics.fill(originX, originY, originX + panelWidth, originY + HEADER_HEIGHT, COLOR_HEADER);
        graphics.outline(originX, originY, panelWidth, panelHeight, COLOR_BORDER);
        int contentBottom = originY + panelHeight - FOOTER_HEIGHT;
        graphics.verticalLine(originX + rightXOffset - PADDING / 2, originY + HEADER_HEIGHT, contentBottom, COLOR_DIVIDER);

        graphics.text(font, "Movement Test Client (drag me)", originX + PADDING, originY + 5, 0xFFFFFF, true);

        int contentTop = originY + HEADER_HEIGHT;
        int categoryYOffset = contentTopOffset - scrollOffset;
        for (ModuleCategory category : ModuleCategory.values()) {
            List<Module> inCategory = modulesIn(category);
            if (inCategory.isEmpty()) {
                continue;
            }
            int rowsInTallestColumn = Math.min(inCategory.size(), MAX_ROWS_PER_COLUMN);
            int labelY = originY + categoryYOffset + 1;
            if (labelY >= contentTop && labelY <= contentBottom) {
                graphics.text(font, category.name(), originX + PADDING, labelY, 0x8FAFC8, true);
            }
            categoryYOffset += 12 + rowsInTallestColumn * ROW_HEIGHT + CATEGORY_GAP;
        }

        int settingsHeaderY = originY + contentTopOffset - scrollOffset + 1;
        if (settingsHeaderY >= contentTop && settingsHeaderY <= contentBottom) {
            if (selectedModule != null) {
                graphics.text(font, selectedModule.getName() + " settings", originX + rightXOffset, settingsHeaderY, 0x8FAFC8, true);
            } else {
                graphics.text(font, "Click \"Cfg\" on a module", originX + rightXOffset, settingsHeaderY, 0x808080, true);
            }
        }
        if (selectedModule != null && selectedModule.getSettings().isEmpty()) {
            graphics.text(font, "(no settings)", originX + rightXOffset, originY + settingsTopOffset - scrollOffset, 0x808080, true);
        }

        if (maxScroll > 0) {
            graphics.text(font, "scroll for more", originX + panelWidth - PADDING - 62, originY + 5, 0xA0A8B0, true);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        String hoveredDescription = hoveredModuleDescription();
        if (hoveredDescription != null) {
            graphics.text(font, hoveredDescription, originX + PADDING, originY + panelHeight - FOOTER_HEIGHT + 2, 0xC0C8D0, true);
        }
    }

    private String hoveredModuleDescription() {
        for (Map.Entry<Module, Button> entry : moduleButtons.entrySet()) {
            if (entry.getValue().visible && entry.getValue().isHovered()) {
                return entry.getKey().getDescription();
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (bindingTarget != null) {
            applyBinding(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            return true;
        }
        if (isInHeaderDragArea(event.x(), event.y())) {
            draggingPanel = true;
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (bindingTarget != null) {
            applyBinding(event.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(event));
            return true;
        }
        return super.keyPressed(event);
    }

    private void applyBinding(InputConstants.Key key) {
        KeyMapping mapping = bindingTarget.getKeyMapping();
        if (mapping != null) {
            mapping.setKey(key);
            KeyMapping.resetMapping();
            Minecraft.getInstance().options.save();
        }
        if (keybindButton != null && selectedModule == bindingTarget) {
            keybindButton.setMessage(keybindLabel(bindingTarget));
        }
        bindingTarget = null;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingPanel) {
            // Clamp so the header (and every widget dragged along with it) can never end up
            // partially off-screen, however far the mouse moves.
            int clampedX = Math.max(0, Math.min(originX + (int) dragX, width - panelWidth));
            int clampedY = Math.max(0, Math.min(originY + (int) dragY, height - HEADER_HEIGHT));
            int dx = clampedX - originX;
            int dy = clampedY - originY;
            if (dx != 0 || dy != 0) {
                originX += dx;
                originY += dy;
                for (var child : children()) {
                    if (child instanceof AbstractWidget widget) {
                        widget.setX(widget.getX() + dx);
                        widget.setY(widget.getY() + dy);
                    }
                }
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingPanel) {
            draggingPanel = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean overPanel = mouseX >= originX && mouseX <= originX + panelWidth
                && mouseY >= originY + HEADER_HEIGHT && mouseY <= originY + panelHeight - FOOTER_HEIGHT;
        if (maxScroll > 0 && overPanel) {
            applyScrollDelta((int) Math.round(-scrollY * SCROLL_STEP));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void applyScrollDelta(int pixels) {
        int newScrollOffset = Math.max(0, Math.min(scrollOffset + pixels, maxScroll));
        int actualDelta = newScrollOffset - scrollOffset;
        if (actualDelta != 0) {
            shiftContentWidgets(-actualDelta);
            scrollOffset = newScrollOffset;
            updateContentVisibility();
        }
    }

    /** Recomputes how far the content can scroll, and snaps the current scroll back in bounds if it shrank. */
    private void recomputeScrollBounds() {
        int naturalHeight = Math.max(leftNaturalHeight, rightNaturalHeight);
        int newMaxScroll = Math.max(0, naturalHeight - (panelHeight - contentTopOffset - PADDING - FOOTER_HEIGHT));
        if (scrollOffset > newMaxScroll) {
            shiftContentWidgets(scrollOffset - newMaxScroll);
            scrollOffset = newMaxScroll;
        }
        maxScroll = newMaxScroll;
        updateContentVisibility();
    }

    /** Shifts every scrollable content widget (everything except the always-visible close button) vertically. */
    private void shiftContentWidgets(int dy) {
        if (dy == 0) {
            return;
        }
        for (var child : children()) {
            if (child instanceof AbstractWidget widget && widget != closeButton) {
                widget.setY(widget.getY() + dy);
            }
        }
    }

    /** Hides (and thereby disables clicking on) any content widget currently scrolled outside the viewport. */
    private void updateContentVisibility() {
        int contentTop = originY + HEADER_HEIGHT;
        int contentBottom = originY + panelHeight - FOOTER_HEIGHT;
        for (var child : children()) {
            if (child instanceof AbstractWidget widget && widget != closeButton) {
                int top = widget.getY();
                int bottom = top + widget.getHeight();
                widget.visible = bottom > contentTop && top < contentBottom;
            }
        }
    }

    private boolean isInHeaderDragArea(double x, double y) {
        if (y < originY || y > originY + HEADER_HEIGHT) {
            return false;
        }
        double closeButtonLeft = originX + panelWidth - PADDING - CLOSE_BUTTON_SIZE - 4;
        return x >= originX && x < closeButtonLeft;
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
