/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.screens;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.icus.mag.bannerrecipes.gui.screens.packdownload.BannerPackDownloadManagementScreen;
import se.icus.mag.bannerrecipes.gui.screens.packselection.BannerPackSelectionScreen;

public final class ManageBannerRecipesScreen extends Screen {
    private final Screen previousScreen;

    public ManageBannerRecipesScreen(Screen previousScreen) {
        super(Component.translatable("banner-recipes.screen.manage.title"));
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        StringWidget title = new StringWidget(this.getTitle(), this.font);
        title.setPosition((this.width - title.getWidth()) / 2, this.height / 2 - 38);
        this.addRenderableWidget(title);

        int buttonWidth = 190;
        int buttonX = (this.width - buttonWidth) / 2;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("banner-recipes.screen.manage.select_packs"),
                        button -> this.minecraft.gui.setScreen(new BannerPackSelectionScreen(this)))
                .bounds(buttonX, this.height / 2 - 10, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("banner-recipes.screen.manage.download_packs"),
                        button -> this.minecraft.gui.setScreen(new BannerPackDownloadManagementScreen(this)))
                .bounds(buttonX, this.height / 2 + 14, buttonWidth, 20)
                .build());
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.previousScreen);
    }
}
