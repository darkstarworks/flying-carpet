package com.darkstarworks.flyingcarpet;

import com.darkstarworks.flyingcarpet.config.FlyingCarpetConfig;
import com.darkstarworks.flyingcarpet.entity.FlyingCarpetEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTabs;

public class FlyingCarpetFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ModContent.CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
        FlyingCarpetConfig.load();

        ModContent.registerAll();

        FabricDefaultAttributeRegistry.register(
            ModContent.FLYING_CARPET, FlyingCarpetEntity.createAttributes().build());

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(output -> ModContent.CARPET_ITEMS.values().forEach(output::accept));
    }
}
