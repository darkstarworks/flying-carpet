package com.darkstarworks.flyingcarpet.compat.jei;

import com.darkstarworks.flyingcarpet.ModContent;
import com.darkstarworks.flyingcarpet.recipe.FlyingCarpetRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shows one shapeless crafting recipe per carpet color in JEI, since JEI can't display the custom recipe itself.
 * Built from the local config, so a server with different recipe settings may show slightly off ingredients.
 */
@JeiPlugin
public class FlyingCarpetJeiPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return ModContent.id("jei_plugin");
    }

    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        List<Ingredient> extras = FlyingCarpetRecipe.extraIngredients().stream().map(Ingredient::of).toList();
        List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();

        for (Map.Entry<DyeColor, Item> entry : ModContent.CARPET_ITEMS.entrySet()) {
            Ingredient cushion = Ingredient.of(Items.CUSHION.pick(entry.getKey()));
            List<Ingredient> ingredients = new ArrayList<>(List.of(cushion, cushion, cushion));
            ingredients.addAll(extras);

            ShapelessRecipe recipe = new ShapelessRecipe(
                new Recipe.CommonInfo(false),
                new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
                new ItemStackTemplate(entry.getValue()),
                ingredients
            );
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE,
                ModContent.id("jei/" + entry.getKey().getName() + "_flying_carpet"));
            recipes.add(new RecipeHolder<>(key, recipe));
        }

        registration.addRecipes(RecipeTypes.CRAFTING, recipes);
    }
}
