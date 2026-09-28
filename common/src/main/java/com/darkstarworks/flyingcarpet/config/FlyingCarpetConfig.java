package com.darkstarworks.flyingcarpet.config;

import com.darkstarworks.flyingcarpet.ModContent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * JSON config at {@code config/flyingcarpet.json}, shared by the settings screen and
 * gameplay code. On a server, the server's copy decides how carpets behave.
 */
public class FlyingCarpetConfig {

    /** What happens to the carpet when the rider gets off with no ground close below. */
    public enum AirDismount {
        /** The carpet turns back into a plain cushion and drops. */
        DROP_CUSHION,
        /** The carpet drops as a Flying Carpet item, nothing is lost. */
        DROP_CARPET,
        /** The carpet stays floating where it was. */
        HOVER
    }

    /** What a carpet drops when it is broken by hitting it. */
    public enum BreakDrop {
        CUSHION,
        CARPET,
        NOTHING
    }

    // ---- Recipe ------------------------------------------------------------
    public boolean enforceColorMatch = true;
    public String recipeLeftSlot = "minecraft:breeze_rod";
    public String recipeCenterSlot = "minecraft:blaze_rod";
    public String recipeRightSlot = "minecraft:breeze_rod";

    // ---- Flight ------------------------------------------------------------
    public static final double SPEED_MIN = 0.5D;
    public static final double SPEED_MAX = 10.0D;

    /** Flight speed as a multiple of the Happy Ghast's speed. */
    public double flightSpeedMultiplier = 1.5D;
    public boolean showHud = true;

    // ---- Getting off -------------------------------------------------------
    /** Keep the carpet (parked) when getting off with solid ground within {@link #safeDistance}. */
    public boolean safeRevertEnabled = true;
    public double safeDistance = 2.0D;
    public AirDismount airDismount = AirDismount.DROP_CUSHION;
    public boolean confirmDismount = true;
    public double confirmSeconds = 3.0D;

    // ---- Breaking ----------------------------------------------------------
    public int hitsToBreak = 1;
    public BreakDrop breakDrop = BreakDrop.CUSHION;
    /** Sneak + right-click an empty carpet to pick it up as an item. */
    public boolean sneakPickup = true;

    // ------------------------------------------------------------------------

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
        return ModContent.CONFIG_DIR.resolve(FILE_NAME);
    }

    public static FlyingCarpetConfig load() {
        Path p = path();
        FlyingCarpetConfig cfg = null;
        if (Files.exists(p)) {
            try {
                cfg = GSON.fromJson(Files.readString(p), FlyingCarpetConfig.class);
            } catch (IOException | RuntimeException e) {
                // Corrupt or unreadable: fall back to defaults rather than crash.
            }
        }
        if (cfg == null) {
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

    public void clamp() {
        flightSpeedMultiplier = Math.clamp(flightSpeedMultiplier, SPEED_MIN, SPEED_MAX);
        safeDistance = Math.clamp(safeDistance, 0.0D, 32.0D);
        confirmSeconds = Math.clamp(confirmSeconds, 0.5D, 30.0D);
        hitsToBreak = Math.clamp(hitsToBreak, 1, 50);
        // Gson leaves unknown enum names as null.
        if (airDismount == null) airDismount = AirDismount.DROP_CUSHION;
        if (breakDrop == null) breakDrop = BreakDrop.CUSHION;
        // Blank means "no ingredient in this slot".
        if (recipeLeftSlot == null) recipeLeftSlot = "";
        if (recipeCenterSlot == null) recipeCenterSlot = "";
        if (recipeRightSlot == null) recipeRightSlot = "";
    }

    public int confirmTicks() {
        return (int) Math.round(confirmSeconds * 20.0D);
    }

    /** The three middle-row ingredient ids, left to right. */
    public List<String> rodSlots() {
        return List.of(recipeLeftSlot, recipeCenterSlot, recipeRightSlot);
    }
}
