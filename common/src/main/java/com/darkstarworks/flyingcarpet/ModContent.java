package com.darkstarworks.flyingcarpet;

import com.darkstarworks.flyingcarpet.entity.FlyingCarpetEntity;
import com.darkstarworks.flyingcarpet.item.FlyingCarpetItem;
import com.darkstarworks.flyingcarpet.recipe.FlyingCarpetRecipe;
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

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Loader-agnostic content, registered straight into the vanilla registries.
 * Fabric calls every register method at startup; NeoForge calls each one from the
 * matching registry event.
 */
public final class ModContent {

    private ModContent() {}

    public static final String MOD_ID = "flyingcarpet";

    public static Identifier id(final String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Config directory, supplied by the active loader before {@code FlyingCarpetConfig.load()}. */
    public static Path CONFIG_DIR;

    public static final ResourceKey<EntityType<?>> FLYING_CARPET_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, id("flying_carpet"));

    public static EntityType<FlyingCarpetEntity> FLYING_CARPET;

    public static final Map<DyeColor, Item> CARPET_ITEMS = new EnumMap<>(DyeColor.class);

    public static void registerAll() {
        registerEntities();
        registerItems();
        registerRecipeSerializers();
    }

    public static void registerEntities() {
        FLYING_CARPET = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            FLYING_CARPET_KEY,
            EntityType.Builder.of(FlyingCarpetEntity::new, MobCategory.MISC)
                .noLootTable()
                .sized(1.0F, 0.25F)
                .clientTrackingRange(10)
                .build(FLYING_CARPET_KEY)
        );
    }

    public static void registerItems() {
        for (DyeColor color : DyeColor.values()) {
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(color.getName() + "_flying_carpet"));
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
    }

    public static void registerRecipeSerializers() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("flying_carpet"), FlyingCarpetRecipe.SERIALIZER);
    }
}
