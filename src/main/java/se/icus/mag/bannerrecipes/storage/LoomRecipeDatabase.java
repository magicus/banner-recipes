/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeCategory;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

public final class LoomRecipeDatabase {
    private static final Map<BannerRecipeCategory, List<BannerRecipe>> RECIPES_BY_CATEGORY = new LinkedHashMap<>();

    static {
        add(
                LoomRecipeCategories.BASIC,
                new BannerRecipe(
                        "skull",
                        "Skull banner",
                        "demo",
                        null,
                        "decorative",
                        "black",
                        List.of(new BannerRecipeLayer(
                                Identifier.fromNamespaceAndPath("minecraft", "skull"), DyeColor.WHITE))));

        add(
                LoomRecipeCategories.FLAGS,
                new BannerRecipe(
                        "swedish_flag",
                        "Swedish flag",
                        "demo",
                        null,
                        "flags",
                        "blue",
                        List.of(
                                new BannerRecipeLayer(
                                        Identifier.fromNamespaceAndPath("minecraft", "stripe_center"), DyeColor.YELLOW),
                                new BannerRecipeLayer(
                                        Identifier.fromNamespaceAndPath("minecraft", "stripe_middle"),
                                        DyeColor.YELLOW))));

        for (int i = 0; i < 25; i++) {
            add(
                    LoomRecipeCategories.DECORATIVE,
                    new BannerRecipe(
                            "creeper",
                            "Creeper banner",
                            "demo",
                            null,
                            "decorative",
                            "green",
                            List.of(new BannerRecipeLayer(
                                    Identifier.fromNamespaceAndPath("minecraft", "creeper"), DyeColor.BLACK))));
        }
    }

    private static void add(BannerRecipeCategory category, BannerRecipe recipe) {
        List<BannerRecipe> recipes = RECIPES_BY_CATEGORY.computeIfAbsent(category, k -> new ArrayList<>());
        recipes.add(recipe);
    }

    public static List<BannerRecipe> recipesForCategory(BannerRecipeCategory category) {
        if (category == LoomRecipeCategories.ALL) {
            return List.copyOf(
                    RECIPES_BY_CATEGORY.values().stream().flatMap(List::stream).toList());
        }

        return RECIPES_BY_CATEGORY.getOrDefault(category, List.of());
    }
}
