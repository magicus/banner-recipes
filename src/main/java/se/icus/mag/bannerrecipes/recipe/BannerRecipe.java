/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.recipe;

import java.util.List;

public record BannerRecipe(
        String id,
        String description,
        String author,
        String url,
        String category,
        String bannerColor,
        List<BannerRecipeLayer> layers) {}
