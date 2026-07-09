# NeoForge module (dormant)

This module is **scaffolded but not active**, and it does **not build yet**.

## Why it's dormant

NeoForge has **not published a Minecraft 26.3 build**. As of this writing their
maven (`https://maven.neoforged.net/releases`) tops out at the **26.1 alphas** and
**26.2 betas** — there is no `26.3.x` artifact to compile against. You cannot build
a NeoForge mod for a Minecraft version NeoForge itself doesn't support yet.

## What's already done (loader-agnostic, in `common/`)

The hard part is shared and works on any loader:

- `ModContent.registerContent()` — entity type, 16 colored items, recipe serializer (vanilla registries)
- `FlyingCarpetEntity` — flight, steering, dismount logic, `createAttributes()`
- `FlyingCarpetItem`, `FlyingCarpetRecipe` — placement + config-driven recipe
- `FlyingCarpetRenderer`, `FlyingCarpetConfigScreen` — reuse vanilla cushion model / vanilla widgets

## What this module still needs (when NeoForge 26.3 exists)

1. Set `neoforge_version` in `gradle.properties` to the published build.
2. Uncomment `include 'neoforge'` in `settings.gradle`.
3. Confirm the ModDevGradle plugin version in `neoforge/build.gradle`.
4. Flesh out `FlyingCarpetNeoForge` against the real NeoForge API (the file
   contains the exact event-bus shape as commented pseudocode):
   - `EntityAttributeCreationEvent` → attributes
   - `EntityRenderersEvent.RegisterRenderers` → renderer
   - `RegisterKeyMappingsEvent` → keybind
   - `RegisterClientCommandsEvent` → `/fc` command
5. Port `PlayerMixin` (sneak-again-to-confirm dismount) — MixinExtras works on
   NeoForge; add a mixin config referenced from the jar.
6. Copy or share the `assets/` + `data/` from `fabric/src/main/resources`.

Everything in `common/` should carry over unchanged.
