package com.darkstarworks.flyingcarpet.client;

import com.darkstarworks.flyingcarpet.config.FlyingCarpetConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Dependency-free settings screen built entirely from self-rendering vanilla
 * widgets (no manual text drawing, which changed substantially in 26.x). Reads
 * and writes the same {@link FlyingCarpetConfig} JSON the gameplay code uses.
 *
 * <p>Left column: disabled "label" buttons. Right column: an EditBox (numbers/ids)
 * or a toggle Button (booleans). "Done" saves and returns to the previous screen.</p>
 */
public class FlyingCarpetConfigScreen extends Screen {

    private final @Nullable Screen parent;
    private final FlyingCarpetConfig cfg = FlyingCarpetConfig.get();

    private EditBox safeDistBox;
    private EditBox leftRodBox;
    private EditBox centerRodBox;
    private EditBox rightRodBox;
    private boolean safeRevert;
    private boolean colorMatch;
    private boolean confirmDismount;

    public FlyingCarpetConfigScreen(final @Nullable Screen parent) {
        super(Component.literal("Flying Carpet Settings"));
        this.parent = parent;
        this.safeRevert = cfg.safeRevertEnabled;
        this.colorMatch = cfg.enforceColorMatch;
        this.confirmDismount = cfg.confirmDismount;
    }

    private static final int LABEL_W = 150;
    private static final int FIELD_W = 140;
    private static final int ROW_H = 24;

    @Override
    protected void init() {
        int labelX = this.width / 2 - (LABEL_W + FIELD_W + 6) / 2;
        int fieldX = labelX + LABEL_W + 6;
        int fullW = LABEL_W + FIELD_W + 6;
        int y = 40;

        // Flight speed: a slider spanning the full row, mapping 0..1 -> [SPEED_MIN, SPEED_MAX].
        addRenderableWidget(new SpeedSlider(labelX, y, fullW));
        y += ROW_H;

        addLabel(labelX, y, "Safe distance (blocks)");
        this.safeDistBox = addField(fieldX, y, Double.toString(cfg.safeDistance));
        y += ROW_H;

        addLabel(labelX, y, "Safe dismount protection");
        Button safeBtn = Button.builder(toggleLabel(safeRevert), b -> {
            this.safeRevert = !this.safeRevert;
            b.setMessage(toggleLabel(this.safeRevert));
        }).bounds(fieldX, y, FIELD_W, 20).build();
        addRenderableWidget(safeBtn);
        y += ROW_H;

        addLabel(labelX, y, "Confirm dismount (sneak twice)");
        Button confirmBtn = Button.builder(toggleLabel(confirmDismount), b -> {
            this.confirmDismount = !this.confirmDismount;
            b.setMessage(toggleLabel(this.confirmDismount));
        }).bounds(fieldX, y, FIELD_W, 20).build();
        addRenderableWidget(confirmBtn);
        y += ROW_H;

        addLabel(labelX, y, "Enforce cushion color match");
        Button matchBtn = Button.builder(toggleLabel(colorMatch), b -> {
            this.colorMatch = !this.colorMatch;
            b.setMessage(toggleLabel(this.colorMatch));
        }).bounds(fieldX, y, FIELD_W, 20).build();
        addRenderableWidget(matchBtn);
        y += ROW_H;

        addLabel(labelX, y, "Recipe slot: left rod");
        this.leftRodBox = addField(fieldX, y, cfg.recipeLeftSlot);
        y += ROW_H;

        addLabel(labelX, y, "Recipe slot: center rod");
        this.centerRodBox = addField(fieldX, y, cfg.recipeCenterSlot);
        y += ROW_H;

        addLabel(labelX, y, "Recipe slot: right rod");
        this.rightRodBox = addField(fieldX, y, cfg.recipeRightSlot);
        y += ROW_H + 8;

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
            saveAndClose();
        }).bounds(this.width / 2 - 100, y, 200, 20).build());
    }

    private void addLabel(final int x, final int y, final String text) {
        Button label = Button.builder(Component.literal(text), b -> {}).bounds(x, y, LABEL_W, 20).build();
        label.active = false; // renders as a static, greyed label
        addRenderableWidget(label);
    }

    private EditBox addField(final int x, final int y, final String value) {
        EditBox box = new EditBox(this.font, x, y, FIELD_W, 20, Component.literal(""));
        box.setMaxLength(256);
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    private static Component toggleLabel(final boolean on) {
        return Component.literal(on ? "ON" : "OFF");
    }

    private static double parseDouble(final String s, final double fallback) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void saveAndClose() {
        // flightSpeedMultiplier is written live by the slider's applyValue().
        cfg.safeDistance = parseDouble(safeDistBox.getValue(), cfg.safeDistance);
        cfg.safeRevertEnabled = this.safeRevert;
        cfg.enforceColorMatch = this.colorMatch;
        cfg.confirmDismount = this.confirmDismount;
        cfg.recipeLeftSlot = leftRodBox.getValue().trim();
        cfg.recipeCenterSlot = centerRodBox.getValue().trim();
        cfg.recipeRightSlot = rightRodBox.getValue().trim();
        cfg.save();
        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    /**
     * Flight-speed slider. The widget's internal value is normalized 0..1 and maps
     * linearly onto [{@link FlyingCarpetConfig#SPEED_MIN}, {@link FlyingCarpetConfig#SPEED_MAX}].
     * {@code applyValue()} writes the live config each drag, so speed changes are felt immediately.
     */
    private final class SpeedSlider extends AbstractSliderButton {
        private SpeedSlider(final int x, final int y, final int width) {
            super(x, y, width, 20, Component.empty(),
                (FlyingCarpetConfig.get().flightSpeedMultiplier - FlyingCarpetConfig.SPEED_MIN)
                    / (FlyingCarpetConfig.SPEED_MAX - FlyingCarpetConfig.SPEED_MIN));
            updateMessage();
        }

        private double speed() {
            return FlyingCarpetConfig.SPEED_MIN
                + this.value * (FlyingCarpetConfig.SPEED_MAX - FlyingCarpetConfig.SPEED_MIN);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(String.format("Flight speed: %.2fx Happy Ghast", speed())));
        }

        @Override
        protected void applyValue() {
            cfg.flightSpeedMultiplier = Mth.clamp(speed(), FlyingCarpetConfig.SPEED_MIN, FlyingCarpetConfig.SPEED_MAX);
        }
    }
}
