/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

public class BannerUtils {
    public static boolean isBlankBanner(ItemStack stack) {
        BannerPatternLayers patterns = stack.get(DataComponents.BANNER_PATTERNS);
        return patterns == null || patterns.layers().isEmpty();
    }

    public static Component displayName(BannerRecipe recipe) {
        String description = recipe.description();
        return description == null || description.isBlank()
                ? Component.translatable("banner-recipes.banner.unnamed")
                : Component.literal(description);
    }
}
