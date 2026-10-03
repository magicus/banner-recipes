/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.recipe;

import java.util.List;
import net.minecraft.world.item.DyeColor;

public record BannerRecipe(
        String id,
        String description,
        String author,
        String url,
        String category,
        String bannerColor,
        List<BannerRecipeLayer> layers) {
    public static final String DEFAULT_DESCRIPTION = "Unnamed banner";
    public static final String DEFAULT_CATEGORY = "misc";

    public BannerRecipe {
        description = blankToNull(description);
        if (description == null) description = DEFAULT_DESCRIPTION;
        author = blankToNull(author);
        url = blankToNull(url);
        category = blankToNull(category);
        if (category == null) category = DEFAULT_CATEGORY;
        bannerColor = blankToNull(bannerColor);
        if (bannerColor == null) bannerColor = "white";
        if (DyeColor.byName(bannerColor, null) == null) {
            throw new IllegalArgumentException("Invalid banner color: " + bannerColor);
        }
        layers = List.copyOf(layers == null ? List.of() : layers);
    }

    public BannerRecipe withId(String newId) {
        return new BannerRecipe(newId, description, author, url, category, bannerColor, layers);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
