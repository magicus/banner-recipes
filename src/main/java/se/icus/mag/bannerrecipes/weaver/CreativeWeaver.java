/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

public class CreativeWeaver implements Weaver {
    @Override
    public void weave(BannerRecipe recipe) {
        ItemStack wovenBanner = BannerRecipesMod.getItemStack(recipe);

        if (recipe.description() != null && !recipe.description().isBlank()) {
            wovenBanner.set(DataComponents.CUSTOM_NAME, Component.literal(recipe.description()));
        }

        Minecraft.getInstance().player.getInventory().add(wovenBanner);
    }

    @Override
    public boolean isCurrentlyWeaving() {
        return false;
    }

    @Override
    public void tick() {}

    @Override
    public boolean canWeave(BannerRecipe banner) {
        return true;
    }

    @Override
    public List<String> getMissingMaterialDescriptions(BannerRecipe banner) {
        return List.of();
    }
}
