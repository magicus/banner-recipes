/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.recipe;

public record BannerRecipeCategory(String id, String description, String iconItemId) {
    public static final BannerRecipeCategory ALL = new BannerRecipeCategory("all", "All", "minecraft:compass");

    public static BannerRecipeCategory fallback(String id) {
        return new BannerRecipeCategory(id, id, "minecraft:lava_bucket");
    }

    public BannerRecipeCategory mergedWith(BannerRecipeCategory override) {
        String mergedDescription =
                override.description() == null || override.description().isBlank()
                        ? description
                        : override.description();
        String mergedIcon =
                override.iconItemId() == null || override.iconItemId().isBlank() ? iconItemId : override.iconItemId();
        return new BannerRecipeCategory(id, mergedDescription, mergedIcon);
    }
}
