package com.darkstarworks.flyingcarpet.client;

import com.darkstarworks.flyingcarpet.config.FlyingCarpetConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Settings screen built from vanilla widgets, split into pages. Changes save when the screen closes. */
public class FlyingCarpetConfigScreen extends Screen {

    private enum Page { FLYING, GETTING_OFF, BREAKING, RECIPE }

    private static final int LABEL_W = 170;
    private static final int FIELD_W = 130;
    private static final int GAP = 6;
    private static final int ROW_H = 24;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFA0A0A0;

    private final @Nullable Screen parent;
    private final FlyingCarpetConfig cfg = FlyingCarpetConfig.get();
    private final List<Label> labels = new ArrayList<>();
    private Page page = Page.FLYING;

    /** Drawn right-aligned so long translations grow leftwards instead of under the control. */
    private record Label(Component text, int rightX, int y) {}

    public FlyingCarpetConfigScreen(final @Nullable Screen parent) {
        super(Component.translatable("screen.flyingcarpet.title"));
        this.parent = parent;
    }

    private static Component tr(final String key) {
        return Component.translatable("screen.flyingcarpet." + key);
    }

    @Override
    protected void init() {
        this.labels.clear();
        int fullW = LABEL_W + GAP + FIELD_W;
        int left = this.width / 2 - fullW / 2;

        Page[] pages = Page.values();
        int tabW = (fullW - GAP * (pages.length - 1)) / pages.length;
        for (int i = 0; i < pages.length; i++) {
            Page p = pages[i];
            Button tab = Button.builder(tr("page." + p.name().toLowerCase()), b -> {
                this.page = p;
                this.rebuildWidgets();
            }).bounds(left + i * (tabW + GAP), 32, tabW, 20).build();
            tab.active = p != this.page;
            addRenderableWidget(tab);
        }

        Rows rows = new Rows(left, 64);
        switch (this.page) {
            case FLYING -> {
                addRenderableWidget(new SpeedSlider(left, rows.y, fullW));
                rows.y += ROW_H;
                rows.toggle("show_hud", () -> cfg.showHud, v -> cfg.showHud = v);
            }
            case GETTING_OFF -> {
                rows.toggle("confirm_dismount", () -> cfg.confirmDismount, v -> cfg.confirmDismount = v);
                rows.text("confirm_seconds", Double.toString(cfg.confirmSeconds),
                    s -> cfg.confirmSeconds = parseDouble(s, cfg.confirmSeconds));
                rows.toggle("safe_revert", () -> cfg.safeRevertEnabled, v -> cfg.safeRevertEnabled = v);
                rows.text("safe_distance", Double.toString(cfg.safeDistance),
                    s -> cfg.safeDistance = parseDouble(s, cfg.safeDistance));
                rows.cycle("air_dismount", FlyingCarpetConfig.AirDismount.values(),
                    () -> cfg.airDismount, v -> cfg.airDismount = v);
            }
            case BREAKING -> {
                rows.text("hits_to_break", Integer.toString(cfg.hitsToBreak),
                    s -> cfg.hitsToBreak = (int) parseDouble(s, cfg.hitsToBreak));
                rows.cycle("break_drop", FlyingCarpetConfig.BreakDrop.values(),
                    () -> cfg.breakDrop, v -> cfg.breakDrop = v);
                rows.toggle("sneak_pickup", () -> cfg.sneakPickup, v -> cfg.sneakPickup = v);
            }
            case RECIPE -> {
                rows.toggle("color_match", () -> cfg.enforceColorMatch, v -> cfg.enforceColorMatch = v);
                rows.text("recipe_left", cfg.recipeLeftSlot, s -> cfg.recipeLeftSlot = s.trim());
                rows.text("recipe_center", cfg.recipeCenterSlot, s -> cfg.recipeCenterSlot = s.trim());
                rows.text("recipe_right", cfg.recipeRightSlot, s -> cfg.recipeRightSlot = s.trim());
            }
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
            .bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 12, WHITE);
        for (Label label : this.labels) {
            graphics.text(this.font, label.text(), label.rightX() - this.font.width(label.text()), label.y() + 6, WHITE);
        }
        if (this.minecraft != null && this.minecraft.level != null && this.minecraft.getSingleplayerServer() == null) {
            graphics.centeredText(this.font, tr("server_note"), this.width / 2, this.height - 44, GRAY);
        }
    }

    /** Lays out "label + control" rows top to bottom. */
    private final class Rows {
        private final int fieldX;
        private int y;

        private Rows(final int left, final int top) {
            this.fieldX = left + LABEL_W + GAP;
            this.y = top;
        }

        private Tooltip help(final String key) {
            return Tooltip.create(tr(key + ".help"));
        }

        private void label(final String key) {
            labels.add(new Label(tr(key), this.fieldX - GAP, this.y));
        }

        void toggle(final String key, final Supplier<Boolean> get, final Consumer<Boolean> set) {
            label(key);
            addRenderableWidget(Button.builder(onOff(get.get()), b -> {
                set.accept(!get.get());
                b.setMessage(onOff(get.get()));
            }).bounds(this.fieldX, this.y, FIELD_W, 20).tooltip(help(key)).build());
            this.y += ROW_H;
        }

        <E extends Enum<E>> void cycle(final String key, final E[] values, final Supplier<E> get, final Consumer<E> set) {
            label(key);
            addRenderableWidget(Button.builder(option(key, get.get()), b -> {
                set.accept(values[(get.get().ordinal() + 1) % values.length]);
                b.setMessage(option(key, get.get()));
            }).bounds(this.fieldX, this.y, FIELD_W, 20).tooltip(help(key)).build());
            this.y += ROW_H;
        }

        void text(final String key, final String value, final Consumer<String> set) {
            label(key);
            EditBox box = new EditBox(font, this.fieldX, this.y, FIELD_W, 20, tr(key));
            box.setMaxLength(256);
            box.setValue(value);
            box.setResponder(set);
            box.setTooltip(help(key));
            addRenderableWidget(box);
            this.y += ROW_H;
        }
    }

    private static Component onOff(final boolean on) {
        return Component.translatable(on ? "options.on" : "options.off");
    }

    private static Component option(final String key, final Enum<?> value) {
        return tr(key + "." + value.name().toLowerCase());
    }

    private static double parseDouble(final String s, final double fallback) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void onClose() {
        cfg.save();
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    /** Maps the slider's 0..1 position onto the allowed speed range. */
    private final class SpeedSlider extends AbstractSliderButton {
        private SpeedSlider(final int x, final int y, final int width) {
            super(x, y, width, 20, Component.empty(),
                (cfg.flightSpeedMultiplier - FlyingCarpetConfig.SPEED_MIN)
                    / (FlyingCarpetConfig.SPEED_MAX - FlyingCarpetConfig.SPEED_MIN));
            setTooltip(Tooltip.create(tr("speed.help")));
            updateMessage();
        }

        private double speed() {
            double raw = FlyingCarpetConfig.SPEED_MIN
                + this.value * (FlyingCarpetConfig.SPEED_MAX - FlyingCarpetConfig.SPEED_MIN);
            return Math.round(raw * 10.0D) / 10.0D;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.flyingcarpet.speed", String.format("%.1f", speed())));
        }

        @Override
        protected void applyValue() {
            cfg.flightSpeedMultiplier = Mth.clamp(speed(), FlyingCarpetConfig.SPEED_MIN, FlyingCarpetConfig.SPEED_MAX);
        }
    }
}
