/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import java.util.Map;

/**
 * Complete legacy short-code table; unknown codes must not silently disappear.
 */
final class LegacyBannerPatterns {
    private static final Map<String, String> PATTERN_IDS = Map.ofEntries(
            Map.entry("b", "base"),
            Map.entry("bl", "square_bottom_left"),
            Map.entry("bo", "border"),
            Map.entry("br", "square_bottom_right"),
            Map.entry("bri", "bricks"),
            Map.entry("bs", "stripe_bottom"),
            Map.entry("bt", "triangle_bottom"),
            Map.entry("bts", "triangles_bottom"),
            Map.entry("cbo", "curly_border"),
            Map.entry("cr", "cross"),
            Map.entry("cre", "creeper"),
            Map.entry("cs", "stripe_center"),
            Map.entry("dls", "stripe_downleft"),
            Map.entry("drs", "stripe_downright"),
            Map.entry("flo", "flower"),
            Map.entry("flow", "flow"),
            Map.entry("glb", "globe"),
            Map.entry("gra", "gradient"),
            Map.entry("gru", "gradient_up"),
            Map.entry("guster", "guster"),
            Map.entry("hh", "half_horizontal"),
            Map.entry("hhb", "half_horizontal_bottom"),
            Map.entry("ld", "diagonal_left"),
            Map.entry("ls", "stripe_left"),
            Map.entry("lud", "diagonal_up_left"),
            Map.entry("mc", "circle"),
            Map.entry("moj", "mojang"),
            Map.entry("mr", "rhombus"),
            Map.entry("ms", "stripe_middle"),
            Map.entry("pig", "piglin"),
            Map.entry("rd", "diagonal_up_right"),
            Map.entry("rs", "stripe_right"),
            Map.entry("rud", "diagonal_right"),
            Map.entry("sc", "straight_cross"),
            Map.entry("sku", "skull"),
            Map.entry("ss", "small_stripes"),
            Map.entry("tl", "square_top_left"),
            Map.entry("tr", "square_top_right"),
            Map.entry("ts", "stripe_top"),
            Map.entry("tt", "triangle_top"),
            Map.entry("tts", "triangles_top"),
            Map.entry("vh", "half_vertical"),
            Map.entry("vhr", "half_vertical_right"));

    static String id(String code, int position) {
        String baseId = PATTERN_IDS.get(code);
        if (baseId == null) {
            // If the pattern code is not found in the map, return a default value
            throw new ParseFailure("Unknown legacy banner pattern: " + code, position);
        }

        return "minecraft:" + baseId;
    }
}
