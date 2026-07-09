package com.darkstarworks.flyingcarpet.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Dependency-free JSON config. Lives at {@code config/flyingcarpet.json}.
 *
 * <p>Source of truth for both the settings {@code Screen} (which reads/writes this
 * object) and gameplay code. No third-party config mods are used — just Gson, which
 * is already bundled with Minecraft.</p>
 */
public class FlyingCarpetConfig {

    // ---- Recipe (server-economy customizable) -----------------------------
    /** If true, the three cushions in the top row must all be the same color. */
    public boolean enforceColorMatch = true;
    /** Item id used in the middle-left slot. Default: breeze rod. */
    public String recipeLeftSlot = "minecraft:breeze_rod";
    /** Item id used in the middle-center slot. Default: blaze rod. */
    public String recipeCenterSlot = "minecraft:blaze_rod";
    /** Item id used in the middle-right slot. Default: breeze rod. */
    public String recipeRightSlot = "minecraft:breeze_rod";

    // ---- Flight -----------------------------------------------------------
    /** Allowed range for {@link #flightSpeedMultiplier} (also drives the settings slider). */
    public static final double SPEED_MIN = 1.0D;
    public static final double SPEED_MAX = 5.0D;

    /**
     * Flight speed as a multiple of the Happy Ghast's speed. Default 1.5 = 50%
     * faster than a Happy Ghast. Clamped to [{@link #SPEED_MIN}, {@link #SPEED_MAX}].
     */
    public double flightSpeedMultiplier = 1.5D;

    // ---- Dismount ---------------------------------------------------------
    /**
     * If true, sneak-dismounting with solid ground within {@link #safeDistance}
     * blocks below reverts the entity to a placed Flying Carpet block (keeps the
     * enchanted carpet). Otherwise every dismount drops a plain cushion.
     */
    public boolean safeRevertEnabled = true;
    /** How many blocks below counts as "safe" for a revert. */
    public double safeDistance = 2.0D;
    /**
     * If true, sneaking to dismount requires a confirming second sneak (with an
     * action-bar prompt) so you don't fall off by accident. If false, a single
     * sneak dismounts immediately (vanilla behavior).
     */
    public boolean confirmDismount = true;
    /** Ticks the confirmation window stays open after the first sneak. */
    public int confirmWindowTicks = 60;

    // -----------------------------------------------------------------------

    private static final String FILE_NAME = "flyingcarpet.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static FlyingCarpetConfig INSTANCE;

    public static FlyingCarpetConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static FlyingCarpetConfig load() {
        Path p = path();
        FlyingCarpetConfig cfg;
        if (Files.exists(p)) {
            try {
                cfg = GSON.fromJson(Files.readString(p), FlyingCarpetConfig.class);
                if (cfg == null) cfg = new FlyingCarpetConfig();
            } catch (IOException | RuntimeException e) {
                // Corrupt or unreadable — fall back to defaults rather than crash.
                cfg = new FlyingCarpetConfig();
            }
        } else {
            cfg = new FlyingCarpetConfig();
        }
        cfg.clamp();
        INSTANCE = cfg;
        cfg.save();
        return cfg;
    }

    public void save() {
        clamp();
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(this));
        } catch (IOException e) {
            // Non-fatal: keep running with in-memory values.
        }
    }

    /** Keep values inside ranges that won't break gameplay. */
    public void clamp() {
        if (flightSpeedMultiplier < SPEED_MIN) flightSpeedMultiplier = SPEED_MIN;
        if (flightSpeedMultiplier > SPEED_MAX) flightSpeedMultiplier = SPEED_MAX;
        if (safeDistance < 0.0D) safeDistance = 0.0D;
        if (safeDistance > 32.0D) safeDistance = 32.0D;
        // Blank is allowed and means "no ingredient in this slot" — a server owner
        // can make the recipe cheaper (down to just 3 cushions). Only guard against
        // null so the JSON round-trips cleanly.
        if (recipeLeftSlot == null) recipeLeftSlot = "";
        if (recipeCenterSlot == null) recipeCenterSlot = "";
        if (recipeRightSlot == null) recipeRightSlot = "";
    }

    /** The three middle-row rod ingredient ids, left to right. */
    public List<String> rodSlots() {
        return List.of(recipeLeftSlot, recipeCenterSlot, recipeRightSlot);
    }
}
