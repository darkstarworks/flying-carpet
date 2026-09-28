package com.darkstarworks.flyingcarpet.client;

import com.darkstarworks.flyingcarpet.config.FlyingCarpetConfig;
import com.darkstarworks.flyingcarpet.entity.FlyingCarpetEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Small top-left panel shown while riding: speed, height and what getting off would do. */
public final class FlyingCarpetHud {

    private FlyingCarpetHud() {}

    private static final int PAD = 4;
    private static final int LINE_H = 10;

    public static void extract(final GuiGraphicsExtractor graphics, final DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !FlyingCarpetConfig.get().showHud
            || !(mc.player.getVehicle() instanceof FlyingCarpetEntity carpet)) {
            return;
        }

        double dx = carpet.getX() - carpet.xo;
        double dy = carpet.getY() - carpet.yo;
        double dz = carpet.getZ() - carpet.zo;
        double blocksPerSecond = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0D;

        Component[] lines = {
            Component.translatable("hud.flyingcarpet.speed", String.format("%.1f", blocksPerSecond)),
            Component.translatable("hud.flyingcarpet.height", (int) Math.floor(carpet.getY())),
            outcomeLine(carpet.getDismountOutcome())
        };

        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, mc.font.width(line));
        }
        graphics.fill(PAD - 2, PAD - 2, PAD + width + 2, PAD + lines.length * LINE_H, 0x80000000);
        for (int i = 0; i < lines.length; i++) {
            graphics.text(mc.font, lines[i], PAD, PAD + i * LINE_H, 0xFFFFFFFF, true);
        }
    }

    private static Component outcomeLine(final FlyingCarpetEntity.DismountOutcome outcome) {
        return switch (outcome) {
            case KEEP -> Component.translatable("hud.flyingcarpet.outcome.keep").withStyle(ChatFormatting.GREEN);
            case HOVER -> Component.translatable("hud.flyingcarpet.outcome.hover").withStyle(ChatFormatting.AQUA);
            case DROP_CARPET -> Component.translatable("hud.flyingcarpet.outcome.drop_carpet").withStyle(ChatFormatting.YELLOW);
            case DROP_CUSHION -> Component.translatable("hud.flyingcarpet.outcome.drop_cushion").withStyle(ChatFormatting.RED);
        };
    }
}
