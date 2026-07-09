package com.darkstarworks.flyingcarpet.recipe;

import com.darkstarworks.flyingcarpet.FlyingCarpet;
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

/**
 * Shapeless, config-driven Flying Carpet recipe.
 *
 * <p>Requires exactly <b>3 cushions</b> plus the three configured "rod" ingredients
 * (default breeze/blaze/breeze). If {@code enforceColorMatch} is on, the three
 * cushions must share a color, and the resulting carpet takes that color. The
 * arrangement in the grid does not matter — only the set of ingredients — which
 * matches the "I don't care how they got the cushions" intent and avoids brittle
 * positional assumptions.</p>
 */
public class FlyingCarpetRecipe extends CustomRecipe {

    public static final MapCodec<FlyingCarpetRecipe> MAP_CODEC = MapCodec.unit(FlyingCarpetRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, FlyingCarpetRecipe> STREAM_CODEC =
        StreamCodec.unit(new FlyingCarpetRecipe());
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

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        FlyingCarpetConfig cfg = FlyingCarpetConfig.get();

        // Mutable multiset of the required rod ingredients. Blank slots mean "no
        // ingredient"; unparseable/unknown ids are skipped rather than bricking the
        // recipe. With all three blank, 3 matching cushions alone craft a carpet.
        List<Item> neededRods = new ArrayList<>();
        for (String idStr : cfg.rodSlots()) {
            if (idStr == null || idStr.isBlank()) {
                continue;
            }
            Identifier id = Identifier.tryParse(idStr.trim());
            if (id == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == Items.AIR) {
                continue; // unknown id -> ignore this slot
            }
            neededRods.add(item);
        }

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
                // A non-cushion item that isn't one of the required rods -> no match.
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
        Item carpet = FlyingCarpet.CARPET_ITEMS.get(color);
        // Glint is baked into the carpet item's default components at registration.
        return carpet != null ? new ItemStack(carpet) : ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<FlyingCarpetRecipe> getSerializer() {
        return SERIALIZER;
    }
}
