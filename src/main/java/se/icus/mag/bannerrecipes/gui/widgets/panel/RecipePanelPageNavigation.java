/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.widgets.panel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import se.icus.mag.bannerrecipes.BannerRecipesMod;

public class RecipePanelPageNavigation {
    private static final int TURN_PAGE_SPRITE_WIDTH = 12;
    private static final int TURN_PAGE_SPRITE_HEIGHT = 17;

    private static final WidgetSprites PAGE_FORWARD_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/page_forward"),
            Identifier.withDefaultNamespace("recipe_book/page_forward_highlighted"));
    private static final WidgetSprites PAGE_BACKWARD_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/page_backward"),
            Identifier.withDefaultNamespace("recipe_book/page_backward_highlighted"));
    private static final Component NEXT_PAGE_TEXT = Component.translatable("gui.recipebook.next_page");
    private static final Component PREVIOUS_PAGE_TEXT = Component.translatable("gui.recipebook.previous_page");

    private int leftPos;
    private int topPos;
    private Minecraft minecraft;

    private ImageButton forwardButton;
    private ImageButton backButton;
    private int totalPages;
    private int currentPage;

    public void init(int leftPos, int topPos) {
        this.leftPos = leftPos;
        this.topPos = topPos;

        this.minecraft = Minecraft.getInstance();

        forwardButton = new ImageButton(
                leftPos + 93,
                topPos + 137,
                TURN_PAGE_SPRITE_WIDTH,
                TURN_PAGE_SPRITE_HEIGHT,
                PAGE_FORWARD_SPRITES,
                this::goToNextPage,
                NEXT_PAGE_TEXT);
        forwardButton.setTooltip(Tooltip.create(NEXT_PAGE_TEXT));

        backButton = new ImageButton(
                leftPos + 38,
                topPos + 137,
                TURN_PAGE_SPRITE_WIDTH,
                TURN_PAGE_SPRITE_HEIGHT,
                PAGE_BACKWARD_SPRITES,
                this::goToPreviousPage,
                PREVIOUS_PAGE_TEXT);
        backButton.setTooltip(Tooltip.create(PREVIOUS_PAGE_TEXT));

        refreshContent();
    }

    public void refreshContent() {
        this.totalPages = BannerRecipesMod.getManager().getPanelView().getTotalPages();
        this.currentPage = BannerRecipesMod.getManager().getPanelView().getCurrentPage();

        forwardButton.visible = currentPage < totalPages - 1;
        backButton.visible = currentPage > 0;
    }

    private void goToPreviousPage(Button button) {
        BannerRecipesMod.getManager().getPanelView().goToPreviousPage();
    }

    private void goToNextPage(Button button) {
        BannerRecipesMod.getManager().getPanelView().goToNextPage();
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (this.totalPages > 1) {
            Component pageNumbers = Component.translatable("gui.recipebook.page", currentPage + 1, totalPages);
            int textWidth = minecraft.font.width(pageNumbers);
            graphics.text(minecraft.font, pageNumbers, leftPos - textWidth / 2 + 73, topPos + 141, -1);
        }

        forwardButton.extractRenderState(graphics, mouseX, mouseY, delta);
        backButton.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (forwardButton.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (backButton.mouseClicked(event, doubleClick)) {
            return true;
        }

        return false;
    }
}
