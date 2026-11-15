/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum OnlineServiceType {
    PLANET_MINECRAFT,
    SKIN_MC,
    MINECRAFT_TOOLS,
    NEED_COOLER_SHOES;

    public Component displayName() {
        return Component.translatable("banner-recipes.banner.convert.export_format." + name().toLowerCase(Locale.ROOT));
    }
}
