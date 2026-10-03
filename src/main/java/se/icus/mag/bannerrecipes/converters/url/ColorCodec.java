/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url;

import net.minecraft.world.item.DyeColor;

public final class ColorCodec {
    public static int providerIndex(DyeColor color) {
        if (color == null) throw new IllegalArgumentException("Banner color cannot be null");
        return 15 - color.getId();
    }

    public static DyeColor fromProviderIndex(int index) {
        if (index < 0 || index > 15) throw new IllegalArgumentException("Invalid color index: " + index);
        return DyeColor.byId(15 - index);
    }

    public static DyeColor fromName(String name) {
        DyeColor color = DyeColor.byName(name, null);
        if (color == null) throw new IllegalArgumentException("Invalid dye color: " + name);
        return color;
    }
}
