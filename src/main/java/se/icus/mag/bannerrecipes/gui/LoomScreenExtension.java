/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui;

import net.minecraft.client.gui.screens.inventory.LoomScreen;
import se.icus.mag.bannerrecipes.BannerRecipesManager;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.gui.widgets.LoomScreenSidebar;

public class LoomScreenExtension extends DelegatingScreenExtension {
    private final LoomScreen screen;
    private final BannerRecipesManager manager;

    public LoomScreenExtension(LoomScreen screen) {
        this.screen = screen;
        this.manager = BannerRecipesMod.getManager();
    }

    @Override
    public void init() {
        manager.onLoomScreenOpened(screen.menu);

        // Always show the sidebar
        addWidget(new LoomScreenSidebar(screen));
        super.init();
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
