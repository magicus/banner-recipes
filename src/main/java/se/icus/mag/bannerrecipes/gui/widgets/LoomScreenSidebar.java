/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.RecipePanelView;
import se.icus.mag.bannerrecipes.gui.ScreenExtension;

public class LoomScreenSidebar implements ScreenExtension {
    private static final Identifier SIDEBAR_LOCATION =
            Identifier.fromNamespaceAndPath(BannerRecipesMod.MOD_ID, "textures/gui/sidebar_gui.png");
    private static final int SIDEBAR_WIDTH = 28;
    private static final int SIDEBAR_HEIGHT = 175;
    private static final int SIDEBAR_OVERLAP = 6;

    private static final int BG_LEFT_PADDING = 22;

    private static final int WEAVE_BUTTON_Y_OFFSET = 69;
    private static final int EDIT_BUTTON_Y_OFFSET = 89;
    private static final int SWAP_COLORS_BUTTON_Y_OFFSET = 109;
    private static final int MANAGE_BUTTON_Y_OFFSET = 140;

    private static final WidgetSprites WEAVE_BUTTON_SPRITES =
            new WidgetSprites(Identifier.fromNamespaceAndPath(BannerRecipesMod.MOD_ID, "recipe_weave"));

    private static final WidgetSprites EDIT_BUTTON_SPRITES =
            new WidgetSprites(Identifier.fromNamespaceAndPath(BannerRecipesMod.MOD_ID, "recipe_edit"));

    private static final WidgetSprites SWAP_COLORS_BUTTON_SPRITES =
            new WidgetSprites(Identifier.fromNamespaceAndPath(BannerRecipesMod.MOD_ID, "recipe_swap_colors"));

    private static final WidgetSprites MANAGE_BUTTON_SPRITES =
            new WidgetSprites(Identifier.fromNamespaceAndPath(BannerRecipesMod.MOD_ID, "recipe_manage"));

    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_X_OFFSET = -16;

    private static final int RECIPE_BUTTON_Y_OFFSET = 5;

    private final LoomScreen screen;

    public LoomScreenSidebar(LoomScreen screen) {
        this.screen = screen;
        // Tell parent screen it has gotten wider
        screen.imageWidth += BG_LEFT_PADDING;
    }

    @Override
    public void init() {
        screen.leftPos = calculateLeftPos();
        ImageButton recipeBookButton = new ImageButton(
                screen.leftPos + BUTTON_X_OFFSET,
                screen.topPos + RECIPE_BUTTON_Y_OFFSET,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                this::onTogglePanel);
        screen.addRenderableWidget(recipeBookButton);

        ImageButton weaveButton = new ImageButton(
                screen.leftPos + BUTTON_X_OFFSET,
                screen.topPos + WEAVE_BUTTON_Y_OFFSET,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                LoomScreenSidebar.WEAVE_BUTTON_SPRITES,
                this::onWeaveButtonPressed);
        screen.addRenderableWidget(weaveButton);

        ImageButton editButton = new ImageButton(
                screen.leftPos + BUTTON_X_OFFSET,
                screen.topPos + EDIT_BUTTON_Y_OFFSET,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                LoomScreenSidebar.EDIT_BUTTON_SPRITES,
                this::onEditButtonPressed);
        screen.addRenderableWidget(editButton);

        ImageButton swapColorsButton = new ImageButton(
                screen.leftPos + BUTTON_X_OFFSET,
                screen.topPos + SWAP_COLORS_BUTTON_Y_OFFSET,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                LoomScreenSidebar.SWAP_COLORS_BUTTON_SPRITES,
                this::onSwapColorsButtonPressed);
        screen.addRenderableWidget(swapColorsButton);

        ImageButton manageButton = new ImageButton(
                screen.leftPos + BUTTON_X_OFFSET,
                screen.topPos + MANAGE_BUTTON_Y_OFFSET,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                LoomScreenSidebar.MANAGE_BUTTON_SPRITES,
                this::onManageButtonPressed);
        screen.addRenderableWidget(manageButton);
    }

    private void onWeaveButtonPressed(Button button) {}

    private void onEditButtonPressed(Button button) {}

    private void onSwapColorsButtonPressed(Button button) {}

    private void onManageButtonPressed(Button button) {}

    private int calculateLeftPos() {
        return getBaseLeftPos() + (BannerRecipesMod.getManager().getPanelView().isPanelOpen() ? 77 : 0);
    }

    private int getBaseLeftPos() {
        return (screen.width - screen.imageWidth) / 2 + BG_LEFT_PADDING + 11;
    }

    private void onTogglePanel(Button button) {
        RecipePanelView view = BannerRecipesMod.getManager().getPanelView();
        view.setPanelOpen(!view.isPanelOpen());

        // Update our button position if leftPos has changed
        screen.leftPos = calculateLeftPos();
        button.setX(screen.leftPos + BUTTON_X_OFFSET);
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
