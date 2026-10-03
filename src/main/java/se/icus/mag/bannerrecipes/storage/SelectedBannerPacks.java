/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.storage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeCategory;

public final class SelectedBannerPacks {
    private static final SelectedBannerPacks INSTANCE =
            new SelectedBannerPacks(FabricLoader.getInstance().getGameDir().resolve("bannerpacks"));

    private final Path packsRoot;
    private final Path configDir;
    private AvailableBannerPacks availablePacks;
    private SelectedPacksConfig selectedPacksConfig;
    private List<BannerRecipe> recipes = List.of();
    private List<BannerRecipeCategory> categories = List.of(BannerRecipeCategory.ALL);
    private Map<String, Map<String, String>> categoryTranslations = Map.of();

    private SelectedBannerPacks(Path packsRoot) {
        this(packsRoot, FabricLoader.getInstance().getConfigDir().resolve("banner-recipes"));
    }

    SelectedBannerPacks(Path packsRoot, Path configDir) {
        this.packsRoot = packsRoot;
        this.configDir = configDir;
    }

    public static SelectedBannerPacks getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        availablePacks = new AvailableBannerPacks(packsRoot);
        availablePacks.load();
        if (selectedPacksConfig == null) selectedPacksConfig = new SelectedPacksConfig(configDir);
        selectedPacksConfig.removeUnavailableSelections(
                new HashSet<>(availablePacks.getAvailablePacks().keySet()));
        rebuildSelectedRecipeList();
        rebuildCategoryMetadata();
    }

    public synchronized List<BannerRecipe> getRecipes() {
        ensureLoaded();
        return recipes;
    }

    public synchronized List<BannerRecipeCategory> getCategories() {
        ensureLoaded();
        return categories;
    }

    public synchronized Map<String, Map<String, String>> getCategoryTranslations() {
        ensureLoaded();
        return categoryTranslations;
    }

    public synchronized AvailableBannerPacks getAvailablePacks() {
        ensureLoaded();
        return availablePacks;
    }

    public synchronized List<String> getSelectedPackIds() {
        ensureLoaded();
        return selectedPacksConfig.getSelectedPacks();
    }

    public synchronized void selectPack(String packId) {
        ensureLoaded();
        if (availablePacks.getAvailablePack(packId) == null) {
            throw new IllegalArgumentException("pack not available: " + packId);
        }
        selectedPacksConfig.selectPack(packId);
        rebuildSelectedRecipeList();
        rebuildCategoryMetadata();
    }

    public synchronized void deselectPack(String packId) {
        ensureLoaded();
        selectedPacksConfig.deselectPack(packId);
        rebuildSelectedRecipeList();
        rebuildCategoryMetadata();
    }

    public synchronized List<BannerRecipe> recipesForCategory(BannerRecipeCategory category) {
        ensureLoaded();
        if (BannerRecipeCategory.ALL.id().equals(category.id())) return recipes;
        return recipes.stream()
                .filter(recipe -> category.id().equals(recipe.category()))
                .toList();
    }

    public synchronized BannerRecipe addLocalRecipe(BannerRecipe recipe) {
        BannerRecipe added = getAvailablePacks().addBannerRecipe(AvailableBannerPacks.LOCAL_PACK_ID, recipe);
        load();
        return added;
    }

    private void rebuildCategoryMetadata() {
        Map<String, BannerRecipeCategory> merged = new LinkedHashMap<>();
        for (String packId : selectedPacksConfig.getSelectedPacks()) {
            var pack = availablePacks.getAvailablePack(packId);
            if (pack == null) continue;
            for (BannerRecipeCategory category : pack.getCategories()) {
                merged.merge(category.id(), category, BannerRecipeCategory::mergedWith);
            }
        }
        for (BannerRecipe recipe : recipes) {
            if (!merged.containsKey(recipe.category())) {
                merged.put(recipe.category(), BannerRecipeCategory.fallback(recipe.category()));
            }
        }

        List<BannerRecipeCategory> result = new ArrayList<>();
        result.add(BannerRecipeCategory.ALL);
        result.addAll(merged.values());
        categories = List.copyOf(result);

        Map<String, Map<String, String>> translations = new LinkedHashMap<>();
        for (String packId : selectedPacksConfig.getSelectedPacks()) {
            var pack = availablePacks.getAvailablePack(packId);
            if (pack == null) continue;
            pack.getCategoryTranslations()
                    .forEach((locale, values) -> translations
                            .computeIfAbsent(locale, ignored -> new LinkedHashMap<>())
                            .putAll(values));
        }
        Map<String, Map<String, String>> immutableTranslations = new LinkedHashMap<>();
        translations.forEach((locale, values) -> immutableTranslations.put(locale, Map.copyOf(values)));
        categoryTranslations = Map.copyOf(immutableTranslations);
    }

    private void rebuildSelectedRecipeList() {
        List<BannerRecipe> selectedRecipes = new ArrayList<>();
        for (String packId : selectedPacksConfig.getSelectedPacks()) {
            var pack = availablePacks.getAvailablePack(packId);
            if (pack != null) selectedRecipes.addAll(pack.getDesigns());
        }
        recipes = List.copyOf(selectedRecipes);
    }

    private void ensureLoaded() {
        if (availablePacks == null) load();
    }
}
