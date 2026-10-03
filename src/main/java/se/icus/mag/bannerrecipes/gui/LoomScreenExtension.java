/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui;

import net.minecraft.client.gui.screens.inventory.LoomScreen;
import se.icus.mag.bannerrecipes.BannerRecipesManager;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.gui.widgets.LoomScreenSidebar;
import se.icus.mag.bannerrecipes.gui.widgets.panel.RecipePanel;

public class LoomScreenExtension extends DelegatingScreenExtension {
    private final LoomScreen screen;
    private final BannerRecipesManager manager;
    private RecipePanel recipePanel;
    private boolean panelVisible;
    private LoomScreenSidebar sidebar;
    private WeavingGuideOverlay weavingGuide;

    public LoomScreenExtension(LoomScreen screen) {
        this.screen = screen;
        this.manager = BannerRecipesMod.getManager();
    }

    @Override
    public void init() {
        manager.onLoomScreenOpened(screen.menu);

        // Always show the sidebar
        this.sidebar = new LoomScreenSidebar(screen);
        addWidget(sidebar);
        this.weavingGuide = new WeavingGuideOverlay(screen);
        addWidget(weavingGuide);
        super.init();

        RecipePanel recipePanel = new RecipePanel();
        recipePanel.init(screen.width, screen.height);
        this.recipePanel = recipePanel;

        panelVisible = false;
        updatePanelVisibility();
    }

    public void updateActiveRecipe() {
        sidebar.updateActiveRecipe();
    }

    public void updateRecipesInPanel() {
        recipePanel.refreshContent();
    }

    public void updatePanelVisibility() {
        if (manager.getPanelView().isPanelOpen() && !this.panelVisible) {
            screen.addRenderableWidget(this.recipePanel);
            addWidget(this.recipePanel);
            panelVisible = true;
        }
        if (!manager.getPanelView().isPanelOpen() && this.panelVisible) {
            screen.removeWidget(this.recipePanel);
            removeWidget(this.recipePanel);
            panelVisible = false;
        }
    }

    @Override
    public void removed() {
        manager.onLoomScreenClosed();

        super.removed();
    }

    @Override
    public void tick() {
        super.tick();

        manager.tick();
    }
}
