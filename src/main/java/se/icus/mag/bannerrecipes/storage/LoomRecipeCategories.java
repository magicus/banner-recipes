/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.storage;

import java.util.List;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeCategory;

public final class LoomRecipeCategories {
    public static final BannerRecipeCategory ALL = new BannerRecipeCategory("all", "All", "minecraft:compass");

    public static final BannerRecipeCategory BASIC = new BannerRecipeCategory("basic", "Basic", "minecraft:stone");
    public static final BannerRecipeCategory FLAGS = new BannerRecipeCategory(
            "flags",
            "Flags",
            "minecraft:blue_banner[banner_patterns=[{\"pattern\":\"minecraft:straight_cross\",\"color\":\"yellow\"}]]");
    public static final BannerRecipeCategory DECORATIVE =
            new BannerRecipeCategory("decorative", "Decorative", "minecraft:creeper_banner_pattern");

    public static final List<BannerRecipeCategory> TABS = List.of(ALL, BASIC, FLAGS, DECORATIVE);

    private LoomRecipeCategories() {}
}
