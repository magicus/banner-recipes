/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

public record BannerRecipeLayer(Identifier pattern, DyeColor color) {
    public BannerRecipeLayer {
        if (pattern == null) throw new IllegalArgumentException("Pattern identifier cannot be null");
        if (color == null) throw new IllegalArgumentException("Color cannot be null");
    }

    public static BannerRecipeLayer of(String pattern, String color) {
        Identifier parsed = Identifier.tryParse(pattern);
        if (parsed == null) throw new IllegalArgumentException("Invalid pattern identifier: " + pattern);

        DyeColor parsedColor = DyeColor.byName(color, null);
        if (parsedColor == null) throw new IllegalArgumentException("Invalid color: " + color);

        return new BannerRecipeLayer(parsed, parsedColor);
    }
}
