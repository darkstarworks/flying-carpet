package com.darkstarworks.flyingcarpet.client;

import com.darkstarworks.flyingcarpet.ModContent;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;

/** Renderer, HUD, the /flyingcarpet and /fc commands, and an unbound settings key. */
public class FlyingCarpetClient implements ClientModInitializer {

    private static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        EntityRenderers.register(ModContent.FLYING_CARPET, FlyingCarpetRenderer::new);

        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, ModContent.id("flight_info"), FlyingCarpetHud::extract);

        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.flyingcarpet.open_config",
            InputConstants.UNKNOWN.getValue(),
            KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                client.setScreenAndShow(new FlyingCarpetConfigScreen(client.gui.screen()));
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            dispatcher.register(ClientCommands.literal("flyingcarpet").executes(ctx -> openConfig()));
            dispatcher.register(ClientCommands.literal("fc").executes(ctx -> openConfig()));
        });
    }

    private static int openConfig() {
        Minecraft mc = Minecraft.getInstance();
        // Deferred so the chat screen closes first.
        mc.execute(() -> mc.setScreenAndShow(new FlyingCarpetConfigScreen(null)));
        return 1;
    }
}
