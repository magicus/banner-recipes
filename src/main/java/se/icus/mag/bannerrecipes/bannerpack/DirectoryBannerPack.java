/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.bannerpack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

public final class DirectoryBannerPack extends BannerPack {
    public DirectoryBannerPack(
            BannerPackMetadata metadata, Path path, Path bannersPath, Path categoriesPath, Path langPath)
            throws IOException {
        super(metadata, path);
        loadRecipesFromPath(bannersPath);
        loadCategoriesFromPath(categoriesPath);
        loadCategoryLangFiles(langPath);
    }

    public DirectoryBannerPack(BannerPackMetadata metadata, Path path) {
        super(metadata, path);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public BannerRecipe addBannerRecipe(BannerRecipe recipe) throws IOException {
        BannerRecipe normalized = normalizeRecipeForPack(recipe);
        writeRecipe(recipeFile(getPath(), normalized.id()), normalized);
        includeRecipe(normalized);
        return normalized;
    }

    @Override
    public void updateBannerRecipe(BannerRecipe recipe) throws IOException {
        if (recipe.id() == null || recipe.id().isBlank()) {
            throw new IllegalArgumentException("recipe id is required for update");
        }
        addBannerRecipe(recipe);
    }

    @Override
    public void removeBannerRecipe(String recipeId) throws IOException {
        Files.deleteIfExists(recipeFile(getPath(), recipeId));
        excludeRecipe(recipeId);
    }

    public BannerRecipe moveRecipeTo(BannerPack target, String recipeId) throws IOException {
        if (target.isReadOnly()) throw new IllegalArgumentException("cannot move to read-only pack");
        BannerRecipe recipe = getDesign(recipeId);
        if (recipe == null) throw new IllegalArgumentException("recipe not found: " + recipeId);
        BannerRecipe moved = target.addBannerRecipe(recipe.withId(null));
        removeBannerRecipe(recipeId);
        return moved;
    }
}
