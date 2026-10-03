/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes;

import java.util.List;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.world.inventory.LoomMenu;
import se.icus.mag.bannerrecipes.gui.LoomScreenExtension;
import se.icus.mag.bannerrecipes.gui.ScreenExtension;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeCategory;
import se.icus.mag.bannerrecipes.storage.LoomRecipeCategories;
import se.icus.mag.bannerrecipes.storage.LoomRecipeDatabase;
import se.icus.mag.bannerrecipes.weaver.Weaver;
import se.icus.mag.bannerrecipes.weaver.WeavingGuide;

public class BannerRecipesManager {
    private LoomScreenExtension loomExtension;
    private RecipePanelView panelView;
    private boolean persistedStateOpen = false;
    private boolean persistedStateFiltering = false;
    private BannerRecipe activeRecipe;
    private Weaver weaver;
    private WeavingGuide weavingGuide;

    public RecipePanelView getPanelView() {
        return panelView;
    }

    public List<BannerRecipeCategory> getCategories() {
        return LoomRecipeCategories.TABS;
    }

    public List<BannerRecipe> calculateVisibleRecipes(
            BannerRecipeCategory category, boolean isFiltering, String searchText) {
        return LoomRecipeDatabase.recipesForCategory(category).stream()
                .filter(recipe -> searchText.isEmpty() || BannerRecipesMod.matchesSearch(recipe, searchText))
                .filter(recipe -> !isFiltering || BannerRecipesMod.isCraftable(recipe))
                .toList();
    }

    public ScreenExtension getExtension() {
        return loomExtension;
    }

    public void createExtension(LoomScreen s) {
        this.loomExtension = new LoomScreenExtension(s);
    }

    public void removeExtension() {
        this.loomExtension = null;
    }

    public void onLoomScreenOpened(LoomMenu menu) {
        panelView = new RecipePanelView(persistedStateOpen, persistedStateFiltering);
        this.activeRecipe = null;
        this.weaver = Weaver.getWeaver(menu);
        this.weavingGuide = new WeavingGuide(menu);
    }

    public void onLoomScreenClosed() {
        persistedStateOpen = panelView.isPanelOpen();
        persistedStateFiltering = panelView.isFiltering();
        this.activeRecipe = null;
        this.weaver = null;
        this.weavingGuide = null;
    }

    public void tick() {
        if (weaver != null) {
            weaver.tick();
        }
        if (weavingGuide != null) {
            weavingGuide.tick();
        }
    }

    public void recipeSelected(BannerRecipe recipe, boolean autoWeave) {
        this.activeRecipe = recipe;
        this.loomExtension.updateActiveRecipe();
        this.weavingGuide.updateActiveRecipe(recipe);
    }

    public BannerRecipe getActiveRecipe() {
        return activeRecipe;
    }

    public void weaveActiveRecipe() {
        if (activeRecipe != null && weaver != null) {
            weaver.weave(activeRecipe);
        }
    }

    public void updatePanelVisibility() {
        loomExtension.updatePanelVisibility();
    }

    public void updateRecipesInPanel() {
        loomExtension.updateRecipesInPanel();
    }

    public int getWeavingProgress() {
        if (weavingGuide != null) {
            return weavingGuide.currentProgress();
        }
        return -1;
    }
}
