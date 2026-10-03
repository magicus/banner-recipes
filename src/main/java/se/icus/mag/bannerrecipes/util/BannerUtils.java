/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class BannerUtils {
    public static boolean isBlankBanner(ItemStack stack) {
        BannerPatternLayers patterns = stack.get(DataComponents.BANNER_PATTERNS);
        return patterns == null || patterns.layers().isEmpty();
    }
}
