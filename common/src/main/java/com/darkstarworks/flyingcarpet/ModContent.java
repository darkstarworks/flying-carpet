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
 * Loader-agnostic content and registration.
 *
 * <p>Everything here uses only vanilla registries/APIs, so it compiles and runs
 * identically under any loader. Each loader's entrypoint provides {@link #CONFIG_DIR}
 * and calls {@link #registerContent()}; loader-specific glue (attributes, entity
 * renderer, keybind, commands) lives in the per-loader modules.</p>
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

    /** Registers the entity type, 16 colored glint items, and the recipe serializer. */
    public static void registerContent() {
        FLYING_CARPET = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            FLYING_CARPET_KEY,
            EntityType.Builder.of(FlyingCarpetEntity::new, MobCategory.MISC)
                .noLootTable()
                .sized(1.0F, 0.25F)
                .clientTrackingRange(10)
                .build(FLYING_CARPET_KEY)
        );

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

        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("flying_carpet"), FlyingCarpetRecipe.SERIALIZER);
    }
}
