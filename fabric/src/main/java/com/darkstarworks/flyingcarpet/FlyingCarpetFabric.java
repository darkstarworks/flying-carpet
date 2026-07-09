package com.darkstarworks.flyingcarpet;

import com.darkstarworks.flyingcarpet.config.FlyingCarpetConfig;
import com.darkstarworks.flyingcarpet.entity.FlyingCarpetEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric entrypoint. Wires the loader-agnostic {@link ModContent} to Fabric:
 * provides the config directory, registers content, and registers the entity's
 * default attributes via the Fabric API.
 */
public class FlyingCarpetFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ModContent.CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
        FlyingCarpetConfig.load();

        ModContent.registerContent();

        FabricDefaultAttributeRegistry.register(
            ModContent.FLYING_CARPET, FlyingCarpetEntity.createAttributes().build());
    }
}
