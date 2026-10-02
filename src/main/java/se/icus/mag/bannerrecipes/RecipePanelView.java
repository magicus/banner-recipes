/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes;

import java.util.List;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeCategory;
import se.icus.mag.bannerrecipes.storage.LoomRecipeCategories;

public class RecipePanelView {
    private static final int ITEMS_PER_PAGE = 20;

    private boolean panelOpen;
    private BannerRecipeCategory activeCategory = LoomRecipeCategories.ALL;
    private String searchText = "";
    private boolean filtering;
    private int totalPages;
    private int currentPage;

    List<BannerRecipe> allVisibleRecipes;

    public RecipePanelView(boolean panelOpen, boolean filtering) {
        this.filtering = filtering;
        this.panelOpen = panelOpen;

        updateVisibleRecipes();
    }

    public List<BannerRecipe> getRecipesForCurrentPage() {
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allVisibleRecipes.size());
        return allVisibleRecipes.subList(startIndex, endIndex);
    }

    public void updateVisibleRecipes() {
        BannerRecipeCategory category = getActiveCategory();
        boolean isFiltering = isFiltering();
        String searchText = getSearchText();

        allVisibleRecipes = BannerRecipesMod.getManager().calculateVisibleRecipes(category, isFiltering, searchText);

        totalPages = Math.ceilDiv(allVisibleRecipes.size(), ITEMS_PER_PAGE);
        currentPage = 0;
    }

    public boolean isPanelOpen() {
        return panelOpen;
    }

    public void setPanelOpen(boolean panelOpen) {
        this.panelOpen = panelOpen;
        BannerRecipesMod.getManager().updatePanelVisibility();
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
        updateVisibleRecipes();

        BannerRecipesMod.getManager().updateRecipesInPanel();
    }

    public boolean isFiltering() {
        return filtering;
    }

    public void setFiltering(boolean filtering) {
        this.filtering = filtering;
        updateVisibleRecipes();

        BannerRecipesMod.getManager().updateRecipesInPanel();
    }

    public BannerRecipeCategory getActiveCategory() {
        return activeCategory;
    }

    public void setActiveCategory(BannerRecipeCategory activeCategory) {
        this.activeCategory = activeCategory;
        updateVisibleRecipes();

        BannerRecipesMod.getManager().updateRecipesInPanel();
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void goToNextPage() {
        if (currentPage >= totalPages - 1) return;

        currentPage++;
        BannerRecipesMod.getManager().updateRecipesInPanel();
    }

    public void goToPreviousPage() {
        if (currentPage <= 0) return;

        currentPage--;
        BannerRecipesMod.getManager().updateRecipesInPanel();
    }
}
