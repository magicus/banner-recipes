/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.widgets;

import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.util.BannerUtils;

/** An item slot showing a banner recipe, with its name as tooltip. Nothing is drawn if there is no recipe. */
public class RecipePreviewSlot extends AbstractWidget {
    public static final int SIZE = 18;

    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    private final Font font;
    private final Supplier<BannerRecipe> recipe;

    public RecipePreviewSlot(Font font, Supplier<BannerRecipe> recipe) {
        super(0, 0, SIZE, SIZE, Component.empty());
        this.font = font;
        this.recipe = recipe;
        this.active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        BannerRecipe current = recipe.get();
        if (current == null) return;

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, getX(), getY(), SIZE, SIZE);
        graphics.item(BannerRecipesMod.getItemStack(current), getX() + 1, getY() + 1);
        if (isHovered()) {
            graphics.setTooltipForNextFrame(font, BannerUtils.displayName(current), mouseX, mouseY, null);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
