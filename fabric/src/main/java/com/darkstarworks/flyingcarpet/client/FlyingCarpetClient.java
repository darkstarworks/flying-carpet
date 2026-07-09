package com.darkstarworks.flyingcarpet.client;

import com.darkstarworks.flyingcarpet.ModContent;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Client entrypoint: settings screen access via the {@code /flyingcarpet} and
 * {@code /fc} client commands and an (unbound-by-default) keybind. No third-party
 * config mods are involved.
 */
public class FlyingCarpetClient implements ClientModInitializer {

    private static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModContent.FLYING_CARPET, FlyingCarpetRenderer::new);

        // Unbound by default (players bind it in vanilla Controls if they want it).
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
        // Defer to after the command finishes so we replace the chat screen cleanly.
        mc.execute(() -> mc.setScreenAndShow(new FlyingCarpetConfigScreen(null)));
        return 1;
    }
}
