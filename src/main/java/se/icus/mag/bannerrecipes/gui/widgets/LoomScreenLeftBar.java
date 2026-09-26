/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import se.icus.mag.bannerrecipes.BannerRecipesManager;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.gui.ScreenExtension;

public class LoomScreenLeftBar implements ScreenExtension {
    private static final Identifier SIDEBAR_LOCATION =
            Identifier.fromNamespaceAndPath(BannerRecipesMod.MOD_ID, "textures/gui/sidebar_gui.png");
    private static final int SIDEBAR_WIDTH = 28;
    private static final int SIDEBAR_HEIGHT = 175;
    private static final int SIDEBAR_OVERLAP = 6;

    private static final int BG_LEFT_PADDING = 22;

    private static final int RECIPE_BUTTON_WIDTH = 20;
    private static final int RECIPE_BUTTON_HEIGHT = 18;
    private static final int RECIPE_BUTTON_X_OFFSET = -16;
    private static final int RECIPE_BUTTON_Y_OFFSET = 5;

    private final LoomScreen screen;
    private final BannerRecipesManager manager;

    private ImageButton recipeButton;

    public LoomScreenLeftBar(LoomScreen screen, BannerRecipesManager manager) {
        this.screen = screen;
        this.manager = manager;
    }

    public void init() {
        // Shift original screen
        screen.leftPos = getImageLeftPos() + BG_LEFT_PADDING;

        recipeButton = new ImageButton(
                screen.leftPos + RECIPE_BUTTON_X_OFFSET,
                screen.topPos + RECIPE_BUTTON_Y_OFFSET,
                RECIPE_BUTTON_WIDTH,
                RECIPE_BUTTON_HEIGHT,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                button -> {
                    manager.togglePanelOpen();
                });

        screen.addRenderableWidget(recipeButton);
    }

    private int getImageLeftPos() {
        int extendedImageWidth = screen.imageWidth + BG_LEFT_PADDING;
        return (screen.width - extendedImageWidth) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.blit(
                RenderPipelines.GUI_TEXTURED,
                SIDEBAR_LOCATION,
                screen.leftPos - (SIDEBAR_WIDTH - SIDEBAR_OVERLAP),
                screen.topPos,
                0.0F,
                0.0F,
                SIDEBAR_WIDTH,
                SIDEBAR_HEIGHT,
                SIDEBAR_WIDTH,
                SIDEBAR_HEIGHT);
    }
}
