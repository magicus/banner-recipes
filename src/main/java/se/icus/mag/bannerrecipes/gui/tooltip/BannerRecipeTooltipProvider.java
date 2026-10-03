/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.tooltip;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

public class BannerRecipeTooltipProvider {
    private final ItemStack itemStack;

    public BannerRecipeTooltipProvider(BannerRecipe recipe) {
        this.itemStack = BannerRecipesMod.getItemStack(recipe);
    }

    public void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Minecraft minecraft) {
        Identifier tooltipStyle = itemStack.get(DataComponents.TOOLTIP_STYLE);

        graphics.setComponentTooltipForNextFrame(
                minecraft.font,
                new ArrayList<>(Screen.getTooltipFromItem(minecraft, itemStack)),
                mouseX,
                mouseY,
                tooltipStyle);
    }
}
