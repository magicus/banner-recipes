/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;
import se.icus.mag.bannerrecipes.util.Lookup;

public final class WeavingGuideOverlay implements ScreenExtension {
    private final LoomScreen screen;

    public WeavingGuideOverlay(LoomScreen screen) {
        this.screen = screen;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!screen.menu.getResultSlot().getItem().isEmpty()) return;

        BannerRecipe recipe = BannerRecipesMod.getManager().getActiveRecipe();
        if (recipe == null) return;

        int progress = BannerRecipesMod.getManager().getWeavingProgress();
        if (progress < 0) return;

        if (progress >= recipe.layers().size()) return;

        BannerRecipeLayer layer = recipe.layers().get(progress);
        int previewX = screen.leftPos + 141;
        int previewY = screen.topPos + 8;
        graphics.fakeItem(new ItemStack(Lookup.getItemFromDyeColor(layer.color())), previewX + 2, previewY + 2);

        Registry<BannerPattern> registry = BannerRecipesMod.getBannerPatternRegistry(Minecraft.getInstance());
        Optional<Holder.Reference<BannerPattern>> entry = registry.get(layer.pattern());
        if (entry.isEmpty()) return;

        TextureAtlasSprite sprite = graphics.getSprite(Sheets.getBannerSprite(entry.get()));
        float u0 = sprite.getU0();
        float u1 = u0 + (sprite.getU1() - sprite.getU0()) * 21.0F / 64.0F;
        float vSpan = sprite.getV1() - sprite.getV0();
        float v0 = sprite.getV0() + vSpan / 64.0F;
        float v1 = v0 + vSpan * 40.0F / 64.0F;

        graphics.pose().pushMatrix();
        graphics.pose().translate(previewX + 6, previewY + 22);
        graphics.fill(-1, -1, 8, 15, 0xFF9E9276);
        graphics.fill(0, 0, 7, 14, DyeColor.GRAY.getTextureDiffuseColor());
        graphics.blit(sprite.atlasLocation(), 0, 0, 7, 14, u0, u1, v0, v1);
        graphics.pose().popMatrix();
    }
}
