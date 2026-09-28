package com.darkstarworks.flyingcarpet;

import com.darkstarworks.flyingcarpet.config.FlyingCarpetConfig;
import com.darkstarworks.flyingcarpet.entity.FlyingCarpetEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(ModContent.MOD_ID)
public class FlyingCarpetNeoForge {

    public FlyingCarpetNeoForge(final IEventBus modBus) {
        ModContent.CONFIG_DIR = FMLPaths.CONFIGDIR.get();
        FlyingCarpetConfig.load();

        modBus.addListener(FlyingCarpetNeoForge::onRegister);
        modBus.addListener(FlyingCarpetNeoForge::onAttributes);
        modBus.addListener(FlyingCarpetNeoForge::onCreativeTab);
    }

    private static void onRegister(final RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.ENTITY_TYPE)) {
            ModContent.registerEntities();
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            ModContent.registerItems();
        } else if (event.getRegistryKey().equals(Registries.RECIPE_SERIALIZER)) {
            ModContent.registerRecipeSerializers();
        }
    }

    private static void onAttributes(final EntityAttributeCreationEvent event) {
        event.put(ModContent.FLYING_CARPET, FlyingCarpetEntity.createAttributes().build());
    }

    private static void onCreativeTab(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            ModContent.CARPET_ITEMS.values().forEach(event::accept);
        }
    }
}
