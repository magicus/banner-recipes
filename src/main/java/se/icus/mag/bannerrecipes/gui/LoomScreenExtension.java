/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import se.icus.mag.bannerrecipes.BannerRecipesManager;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.gui.widgets.LoomScreenLeftBar;

public class LoomScreenExtension extends DelegatingScreenExtension {
    private final BannerRecipesManager manager;
    private final LoomScreen screen;

    public LoomScreenExtension(LoomScreen screen) {
        BannerRecipesManager manager = BannerRecipesMod.getManager();
        LoomScreenLeftBar leftBar = new LoomScreenLeftBar(screen, manager);
        super(List.of(leftBar));

        this.manager = manager;
        this.screen = screen;
    }

    public void init() {
        super.init();
        manager.onLoomScreenOpened(screen.menu);
    }

    public void removed() {
        super.removed();
        manager.onLoomScreenClosed();
    }

    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        manager.tick();
    }
}
