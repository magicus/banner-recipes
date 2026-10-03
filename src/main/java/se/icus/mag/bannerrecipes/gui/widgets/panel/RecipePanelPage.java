/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.widgets.panel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.RecipePanelView;
import se.icus.mag.bannerrecipes.gui.tooltip.BannerRecipeTooltipProvider;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

public class RecipePanelPage {
    private static final int BACKGROUND_SIZE = 25;

    private static final Identifier SLOT_CRAFTABLE_SPRITE =
            Identifier.withDefaultNamespace("recipe_book/slot_craftable");
    private static final Identifier SLOT_UNCRAFTABLE_SPRITE =
            Identifier.withDefaultNamespace("recipe_book/slot_uncraftable");

    private int leftPos;
    private int topPos;

    private List<RecipePanelPageButton> recipeButtons;

    public void init(int leftPos, int topPos) {
        this.leftPos = leftPos;
        this.topPos = topPos;

        refreshContent();
    }

    public void refreshContent() {
        buildRecipeButtons();
    }

    private void buildRecipeButtons() {
        RecipePanelView panelView = BannerRecipesMod.getManager().getPanelView();
        List<BannerRecipe> recipes = panelView.getRecipesForCurrentPage();

        recipeButtons = new ArrayList<>();
        int index = 0;
        for (BannerRecipe recipe : recipes) {
            int row = index / 5;
            int column = index % 5;

            RecipePanelPageButton button = new RecipePanelPageButton(row, column, recipe);
            recipeButtons.add(button);
            index++;
        }
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        for (RecipePanelPageButton recipeButton : recipeButtons) {
            recipeButton.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    public void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (RecipePanelPageButton recipeButton : recipeButtons) {
            if (recipeButton.isHoveredOrFocused()) {
                recipeButton.extractTooltip(graphics, mouseX, mouseY);
            }
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (RecipePanelPageButton button : recipeButtons) {
            if (button.mouseClicked(event, doubleClick)) {
                return true;
            }
        }

        return false;
    }

    private class RecipePanelPageButton extends AbstractWidget {
        private final Minecraft minecraft;
        private final BannerRecipe recipe;
        private final ItemStack itemStack;
        private final BannerRecipeTooltipProvider bannerRecipeTooltipProvider;

        public RecipePanelPageButton(int row, int column, BannerRecipe recipe) {
            int buttonLeftPos = leftPos + 11 + BACKGROUND_SIZE * column;
            int buttonTopPos = topPos + 31 + BACKGROUND_SIZE * row;

            super(buttonLeftPos, buttonTopPos, BACKGROUND_SIZE, BACKGROUND_SIZE, CommonComponents.EMPTY);
            this.recipe = recipe;
            this.itemStack = BannerRecipesMod.getItemStack(recipe);
            this.bannerRecipeTooltipProvider = new BannerRecipeTooltipProvider(recipe);
            this.minecraft = Minecraft.getInstance();
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            Identifier sprite = BannerRecipesMod.isCraftable(recipe) ? SLOT_CRAFTABLE_SPRITE : SLOT_UNCRAFTABLE_SPRITE;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), width, height);

            graphics.fakeItem(itemStack, getX() + 4, getY() + 4);
        }

        public void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            bannerRecipeTooltipProvider.extractTooltip(graphics, mouseX, mouseY, minecraft);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (super.mouseClicked(event, doubleClick)) {
                if (event.button() == 1) {
                    BannerRecipesMod.getManager().recipeSelected(recipe, event.hasShiftDown());
                }
            }
            return false;
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {}
    }
}
