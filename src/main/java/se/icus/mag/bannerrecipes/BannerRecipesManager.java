/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes;

import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.world.inventory.LoomMenu;
import se.icus.mag.bannerrecipes.gui.LoomScreenExtension;
import se.icus.mag.bannerrecipes.gui.ScreenExtension;

public class BannerRecipesManager {
    private LoomScreenExtension loomExtension;
    private boolean panelOpen;

    public ScreenExtension getExtension() {
        return loomExtension;
    }

    public void createExtension(LoomScreen s) {
        this.loomExtension = new LoomScreenExtension(s);
    }

    public void removeExtension() {
        this.loomExtension = null;
    }

    public void togglePanelOpen() {
        panelOpen = !panelOpen;
    }

    public void onLoomScreenOpened(LoomMenu menu) {}

    public boolean isPanelOpen() {
        return panelOpen;
    }

    public void onLoomScreenClosed() {}

    public void tick() {}
}
