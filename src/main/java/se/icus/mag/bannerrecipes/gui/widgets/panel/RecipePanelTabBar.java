/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.widgets.panel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeCategory;

public class RecipePanelTabBar {
    private static final WidgetSprites SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/tab"),
            Identifier.withDefaultNamespace("recipe_book/tab_selected"));

    private static final int SPRITE_WIDTH = 35;
    private static final int SPRITE_HEIGHT = 27;
    private static final int TAB_BUTTON_Y_OFFSET = 27;

    private int leftPos;
    private int topPos;
    private List<RecipePanelTabButton> tabButtons;
    private RecipePanelTabButton selectedTab;

    public void init(int leftPos, int topPos) {
        this.leftPos = leftPos;
        this.topPos = topPos;

        tabButtons = new ArrayList<>();

        int index = 0;
        for (BannerRecipeCategory category : BannerRecipesMod.getManager().getCategories()) {
            RecipePanelTabButton tabButton = new RecipePanelTabButton(index, category);
            tabButtons.add(tabButton);
            index++;
        }

        selectedTab = tabButtons.getFirst();
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        for (RecipePanelTabButton tabButton : tabButtons) {
            tabButton.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (RecipePanelTabButton tabButton : tabButtons) {
            if (tabButton.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return false;
    }

    private void onTabButtonPress(Button button) {
        if (!(button instanceof RecipePanelTabButton tabButton)) return;

        selectedTab = tabButton;
        BannerRecipesMod.getManager().getPanelView().setActiveCategory(tabButton.getCategory());
    }

    private class RecipePanelTabButton extends ImageButton {
        private final BannerRecipeCategory category;
        private final ItemStack iconItemStack;

        protected RecipePanelTabButton(int index, BannerRecipeCategory category) {
            super(
                    leftPos - 30,
                    index * TAB_BUTTON_Y_OFFSET + topPos + 3,
                    SPRITE_WIDTH,
                    SPRITE_HEIGHT,
                    SPRITES,
                    RecipePanelTabBar.this::onTabButtonPress);
            this.category = category;
            this.iconItemStack = BannerRecipesMod.getItemStack(category);
        }

        protected BannerRecipeCategory getCategory() {
            return category;
        }

        private boolean isSelected() {
            return this == selectedTab;
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            Identifier sprite = sprites.get(true, isSelected());
            int selectionOffset = isSelected() ? -2 : 0;

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX() + selectionOffset, getY(), width, height);
            graphics.fakeItem(iconItemStack, getX() + selectionOffset + 9, getY() + 5);
        }

        @Override
        protected void handleCursor(GuiGraphicsExtractor graphics) {
            if (!isSelected()) {
                super.handleCursor(graphics);
            }
        }
    }
}
