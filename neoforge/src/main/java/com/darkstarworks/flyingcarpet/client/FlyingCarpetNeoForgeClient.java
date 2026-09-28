package com.darkstarworks.flyingcarpet.client;

import com.darkstarworks.flyingcarpet.ModContent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/** Renderer, HUD, the /flyingcarpet and /fc commands, an unbound settings key, and the mods-list config button. */
@Mod(value = ModContent.MOD_ID, dist = Dist.CLIENT)
public class FlyingCarpetNeoForgeClient {

    private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
        "key.flyingcarpet.open_config",
        InputConstants.UNKNOWN.getValue(),
        KeyMapping.Category.MISC
    );

    public FlyingCarpetNeoForgeClient(final IEventBus modBus, final ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new FlyingCarpetConfigScreen(parent));

        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
            event.registerEntityRenderer(ModContent.FLYING_CARPET, FlyingCarpetRenderer::new));
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(OPEN_CONFIG_KEY));
        modBus.addListener((RegisterGuiLayersEvent event) ->
            event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, ModContent.id("flight_info"), FlyingCarpetHud::extract));

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_CONFIG_KEY.consumeClick()) {
                mc.setScreenAndShow(new FlyingCarpetConfigScreen(mc.gui.screen()));
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> {
            event.getDispatcher().register(Commands.literal("flyingcarpet").executes(ctx -> openConfig()));
            event.getDispatcher().register(Commands.literal("fc").executes(ctx -> openConfig()));
        });
    }

    private static int openConfig() {
        Minecraft mc = Minecraft.getInstance();
        // Deferred so the chat screen closes first.
        mc.execute(() -> mc.setScreenAndShow(new FlyingCarpetConfigScreen(null)));
        return 1;
    }
}
