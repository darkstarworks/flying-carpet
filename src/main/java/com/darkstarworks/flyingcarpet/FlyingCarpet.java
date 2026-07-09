package com.darkstarworks.flyingcarpet;

import com.darkstarworks.flyingcarpet.config.FlyingCarpetConfig;
import com.darkstarworks.flyingcarpet.entity.FlyingCarpetEntity;
import com.darkstarworks.flyingcarpet.item.FlyingCarpetItem;
import com.darkstarworks.flyingcarpet.recipe.FlyingCarpetRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.Map;

public class FlyingCarpet implements ModInitializer {

    public static final String MOD_ID = "flyingcarpet";

    public static Identifier id(final String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // ---- Entity type ------------------------------------------------------

    public static final ResourceKey<EntityType<?>> FLYING_CARPET_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, id("flying_carpet"));

    public static final EntityType<FlyingCarpetEntity> FLYING_CARPET = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        FLYING_CARPET_KEY,
        EntityType.Builder.of(FlyingCarpetEntity::new, MobCategory.MISC)
            .noLootTable()
            .sized(1.0F, 0.25F)           // matches vanilla Cushion footprint/height
            .clientTrackingRange(10)
            .build(FLYING_CARPET_KEY)
    );

    // ---- 16 colored items -------------------------------------------------

    public static final Map<DyeColor, Item> CARPET_ITEMS = new EnumMap<>(DyeColor.class);

    @Override
    public void onInitialize() {
        FlyingCarpetConfig.load();

        for (DyeColor color : DyeColor.values()) {
            String name = color.getName() + "_flying_carpet";
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(name));
            Item item = Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new FlyingCarpetItem(
                    new Item.Properties()
                        .setId(itemKey)
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true),
                    color
                )
            );
            CARPET_ITEMS.put(color, item);
        }

        FabricDefaultAttributeRegistry.register(FLYING_CARPET, FlyingCarpetEntity.createAttributes().build());

        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("flying_carpet"), FlyingCarpetRecipe.SERIALIZER);
    }
}
