package com.darkstarworks.flyingcarpet.recipe;

import com.darkstarworks.flyingcarpet.ModContent;
import com.darkstarworks.flyingcarpet.config.FlyingCarpetConfig;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shapeless recipe: three cushions plus the configured extra ingredients, in any arrangement. */
public class FlyingCarpetRecipe extends CustomRecipe {

    /** Stateless, so one shared instance; StreamCodec.unit refuses to encode any other. */
    public static final FlyingCarpetRecipe INSTANCE = new FlyingCarpetRecipe();
    public static final MapCodec<FlyingCarpetRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, FlyingCarpetRecipe> STREAM_CODEC =
        StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<FlyingCarpetRecipe> SERIALIZER =
        new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /** Reverse lookup cushion-item -> color, built lazily from the vanilla cushion collection. */
    private static Map<Item, DyeColor> cushionColors;

    private static DyeColor cushionColor(final Item item) {
        if (cushionColors == null) {
            Map<Item, DyeColor> map = new HashMap<>();
            for (DyeColor color : DyeColor.values()) {
                map.put(Items.CUSHION.pick(color), color);
            }
            cushionColors = map;
        }
        return cushionColors.get(item);
    }

    /** The configured extra ingredients; blank, malformed or unknown ids are skipped so a typo can't disable the recipe. */
    public static List<Item> extraIngredients() {
        List<Item> items = new ArrayList<>();
        for (String idStr : FlyingCarpetConfig.get().rodSlots()) {
            if (idStr == null || idStr.isBlank()) {
                continue;
            }
            Identifier id = Identifier.tryParse(idStr.trim());
            if (id == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item != Items.AIR) {
                items.add(item);
            }
        }
        return items;
    }

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        FlyingCarpetConfig cfg = FlyingCarpetConfig.get();
        List<Item> neededRods = extraIngredients();

        DyeColor color = null;
        int cushions = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            DyeColor c = cushionColor(stack.getItem());
            if (c != null) {
                cushions++;
                if (color == null) {
                    color = c;
                } else if (cfg.enforceColorMatch && color != c) {
                    return false;
                }
            } else if (!neededRods.remove(stack.getItem())) {
                return false;
            }
        }

        return cushions == 3 && neededRods.isEmpty();
    }

    @Override
    public ItemStack assemble(final CraftingInput input) {
        DyeColor color = DyeColor.WHITE;
        for (int i = 0; i < input.size(); i++) {
            DyeColor c = cushionColor(input.getItem(i).getItem());
            if (c != null) {
                color = c;
                break;
            }
        }
        Item carpet = ModContent.CARPET_ITEMS.get(color);
        // Glint is baked into the carpet item's default components at registration.
        return carpet != null ? new ItemStack(carpet) : ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<FlyingCarpetRecipe> getSerializer() {
        return SERIALIZER;
    }
}
