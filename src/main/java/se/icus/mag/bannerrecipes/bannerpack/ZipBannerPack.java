/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.bannerpack;

import java.io.IOException;
import java.nio.file.Path;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

public final class ZipBannerPack extends BannerPack {
    public ZipBannerPack(BannerPackMetadata metadata, Path path, Path bannersPath, Path categoriesPath, Path langPath)
            throws IOException {
        super(metadata, path);
        loadRecipesFromPath(bannersPath);
        loadCategoriesFromPath(categoriesPath);
        loadCategoryLangFiles(langPath);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public BannerRecipe addBannerRecipe(BannerRecipe recipe) {
        throw new IllegalStateException(
                "Cannot add a recipe to read-only pack " + getMetadata().id());
    }

    @Override
    public void updateBannerRecipe(BannerRecipe recipe) {
        throw new IllegalStateException(
                "Cannot update a recipe in read-only pack " + getMetadata().id());
    }

    @Override
    public void removeBannerRecipe(String recipeId) {
        throw new IllegalStateException(
                "Cannot remove a recipe from read-only pack " + getMetadata().id());
    }
}
