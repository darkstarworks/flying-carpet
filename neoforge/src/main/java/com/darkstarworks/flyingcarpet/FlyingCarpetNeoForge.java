package com.darkstarworks.flyingcarpet;

// ============================================================================
// DORMANT NeoForge entrypoint — SCAFFOLD ONLY, NOT YET COMPILED OR VERIFIED.
//
// NeoForge has no Minecraft 26.3 build yet, so this cannot be built or tested.
// It is written from the standard NeoForge event-bus shape to show exactly where
// the shared common/ code plugs in. Every NeoForge API reference below must be
// re-checked against the real 26.3 NeoForge API when it ships — imports are left
// commented so the module does not appear falsely "ready".
//
// The loader-agnostic work is already done in common/:
//   - ModContent.registerContent()          -> entity type, 16 items, recipe serializer
//   - FlyingCarpetEntity.createAttributes()  -> entity attributes
//   - FlyingCarpetRenderer                   -> entity renderer (vanilla cushion model)
//   - FlyingCarpetConfigScreen               -> settings screen (vanilla widgets)
//   - PlayerMixin logic                      -> lives in fabric/; port the mixin here too
//
// What this entrypoint must do once NeoForge 26.3 exists (pseudocode):
//
//   @Mod(ModContent.MOD_ID)
//   public class FlyingCarpetNeoForge {
//       public FlyingCarpetNeoForge(IEventBus modBus, ModContainer container) {
//           ModContent.CONFIG_DIR = FMLPaths.CONFIGDIR.get();
//           FlyingCarpetConfig.load();
//           ModContent.registerContent();                       // vanilla registries — loader-agnostic
//           modBus.addListener(this::onAttributes);
//           if (FMLEnvironment.dist == Dist.CLIENT) {
//               modBus.addListener(FlyingCarpetNeoForgeClient::onRegisterRenderers);
//               modBus.addListener(FlyingCarpetNeoForgeClient::onRegisterKeyMappings);
//           }
//       }
//       private void onAttributes(EntityAttributeCreationEvent e) {
//           e.put(ModContent.FLYING_CARPET, FlyingCarpetEntity.createAttributes().build());
//       }
//   }
//
//   Client (RegisterRenderers / RegisterKeyMappings events) and the /fc command
//   (RegisterClientCommandsEvent) mirror the Fabric client class.
//
//   Also port PlayerMixin (the sneak-again-to-confirm dismount) — MixinExtras works
//   on NeoForge too; it needs a neoforge mixin config entry in the mods.toml jar.
// ============================================================================

/** Placeholder so the package exists; real implementation added when NeoForge 26.3 ships. */
public final class FlyingCarpetNeoForge {
    private FlyingCarpetNeoForge() {}
}
