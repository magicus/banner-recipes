/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.util;

import net.minecraft.network.chat.Component;

public class TextUtil {
    public static Component colored(Component text, int color) {
        return text.copy().withStyle(style -> style.withColor(color).withoutShadow());
    }
}
