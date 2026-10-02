/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.widgets.panel;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import se.icus.mag.bannerrecipes.BannerRecipesMod;

public class RecipePanelFilter {
    private static final Component SHOWING_ALL_TOOLTIP = Component.translatable("gui.recipebook.toggleRecipes.all");
    private static final Component SHOWING_WEAVABLE_TOOLTIP =
            Component.translatable("banner-recipes.tooltip.panel.showing_weavable");
    private static final WidgetSprites FILTER_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(BannerRecipesMod.MOD_ID, "loom_recipe_book/filter_enabled"),
            Identifier.fromNamespaceAndPath(BannerRecipesMod.MOD_ID, "loom_recipe_book/filter_disabled"),
            Identifier.fromNamespaceAndPath(BannerRecipesMod.MOD_ID, "loom_recipe_book/filter_enabled_highlighted"),
            Identifier.fromNamespaceAndPath(BannerRecipesMod.MOD_ID, "loom_recipe_book/filter_disabled_highlighted"));

    private CycleButton<Boolean> filterButton;

    public void init(int leftPos, int topPos) {
        filterButton = CycleButton.booleanBuilder(
                        SHOWING_WEAVABLE_TOOLTIP,
                        SHOWING_ALL_TOOLTIP,
                        BannerRecipesMod.getManager().getPanelView().isFiltering())
                .withTooltip(filtering ->
                        filtering ? Tooltip.create(SHOWING_WEAVABLE_TOOLTIP) : Tooltip.create(SHOWING_ALL_TOOLTIP))
                .withSprite((cycleButton, filtering) ->
                        FILTER_BUTTON_SPRITES.get(filtering, cycleButton.isHoveredOrFocused()))
                .displayState(CycleButton.DisplayState.HIDE)
                .create(leftPos + 110, topPos + 12, 26, 16, CommonComponents.EMPTY, (button, value) -> {
                    BannerRecipesMod.getManager().getPanelView().setFiltering(value);
                });
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        filterButton.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return filterButton.mouseClicked(event, doubleClick);
    }
}
