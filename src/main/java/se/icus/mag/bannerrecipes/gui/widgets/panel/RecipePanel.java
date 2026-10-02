/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.widgets.panel;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import se.icus.mag.bannerrecipes.gui.ScreenExtension;
import se.icus.mag.bannerrecipes.gui.widgets.BasicWidget;

public class RecipePanel extends BasicWidget implements ScreenExtension {
    private static final Identifier PANEL_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/recipe_book.png");
    private static final int PANEL_BACKGROUND_WIDTH = 256;
    private static final int PANEL_BACKGROUND_HEIGHT = 256;

    public static final int IMAGE_WIDTH = 147;
    public static final int IMAGE_HEIGHT = 166;
    public static final int OFFSET_X_POSITION = 86;

    public int width;
    public int height;
    private int leftPos;
    private int topPos;

    private final RecipePanelTabBar tabBar;
    private final RecipePanelSearch search;
    private final RecipePanelFilter filter;
    private final RecipePanelPage page;
    private final RecipePanelPageNavigation pageNavigation;

    public RecipePanel() {
        this.tabBar = new RecipePanelTabBar();
        this.search = new RecipePanelSearch();
        this.filter = new RecipePanelFilter();
        this.page = new RecipePanelPage();
        this.pageNavigation = new RecipePanelPageNavigation();
    }

    public void init(int width, int height) {
        this.width = width;
        this.height = height;
        this.leftPos = getLeftPos();
        this.topPos = getTopPos();

        tabBar.init(leftPos, topPos);
        search.init(leftPos, topPos);
        filter.init(leftPos, topPos);
        page.init(leftPos, topPos);
        pageNavigation.init(leftPos, topPos);

        refreshContent();
    }

    public void refreshContent() {
        page.refreshContent();
        pageNavigation.refreshContent();
    }

    private int getTopPos() {
        return (height - IMAGE_HEIGHT) / 2;
    }

    private int getLeftPos() {
        return (width - IMAGE_WIDTH) / 2 - OFFSET_X_POSITION;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                PANEL_BACKGROUND,
                leftPos,
                topPos,
                1.0F,
                1.0F,
                IMAGE_WIDTH,
                IMAGE_HEIGHT,
                PANEL_BACKGROUND_WIDTH,
                PANEL_BACKGROUND_HEIGHT);

        tabBar.extractRenderState(graphics, mouseX, mouseY, delta);
        search.extractRenderState(graphics, mouseX, mouseY, delta);
        filter.extractRenderState(graphics, mouseX, mouseY, delta);
        page.extractRenderState(graphics, mouseX, mouseY, delta);
        pageNavigation.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        page.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (search.keyPressed(event)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return search.keyReleased();
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return search.charTyped(event);
    }

    @Override
    public boolean preeditUpdated(PreeditEvent event) {
        return search.preeditUpdated(event);
    }

    @Override
    public boolean isInputCaptured() {
        return capturesInput();
    }

    @Override
    public boolean capturesInput() {
        return search.capturesInput();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (tabBar.mouseClicked(event, doubleClick)) return true;
        if (search.mouseClicked(event, doubleClick)) return true;
        if (filter.mouseClicked(event, doubleClick)) return true;
        if (page.mouseClicked(event, doubleClick)) return true;
        if (pageNavigation.mouseClicked(event, doubleClick)) return true;

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return search.mouseDragged(event, dx, dy);
    }
}
