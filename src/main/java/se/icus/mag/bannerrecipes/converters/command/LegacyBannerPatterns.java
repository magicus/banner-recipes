/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import java.util.Map;

final class LegacyBannerPatterns {
    private static final Map<String, String> PATTERN_IDS = Map.ofEntries(
            Map.entry("bs", "minecraft:stripe_bottom"),
            Map.entry("ts", "minecraft:stripe_top"),
            Map.entry("ls", "minecraft:stripe_left"),
            Map.entry("rs", "minecraft:stripe_right"),
            Map.entry("cs", "minecraft:stripe_center"),
            Map.entry("ms", "minecraft:stripe_middle"),
            Map.entry("drs", "minecraft:stripe_downright"),
            Map.entry("dls", "minecraft:stripe_downleft"),
            Map.entry("ss", "minecraft:small_stripes"),
            Map.entry("cr", "minecraft:cross"),
            Map.entry("sc", "minecraft:straight_cross"),
            Map.entry("ld", "minecraft:diagonal_left"),
            Map.entry("rud", "minecraft:diagonal_right"),
            Map.entry("lud", "minecraft:diagonal_up_left"),
            Map.entry("rd", "minecraft:diagonal_up_right"),
            Map.entry("vh", "minecraft:half_vertical"),
            Map.entry("vhr", "minecraft:half_vertical_right"),
            Map.entry("hh", "minecraft:half_horizontal"),
            Map.entry("hhb", "minecraft:half_horizontal_bottom"),
            Map.entry("bl", "minecraft:square_bottom_left"),
            Map.entry("br", "minecraft:square_bottom_right"),
            Map.entry("tl", "minecraft:square_top_left"),
            Map.entry("tr", "minecraft:square_top_right"),
            Map.entry("bt", "minecraft:triangle_bottom"),
            Map.entry("tt", "minecraft:triangle_top"),
            Map.entry("bts", "minecraft:triangles_bottom"),
            Map.entry("tts", "minecraft:triangles_top"),
            Map.entry("mc", "minecraft:circle"),
            Map.entry("mr", "minecraft:rhombus"),
            Map.entry("bo", "minecraft:border"),
            Map.entry("cbo", "minecraft:curly_border"),
            Map.entry("bri", "minecraft:bricks"),
            Map.entry("gra", "minecraft:gradient"),
            Map.entry("gru", "minecraft:gradient_up"),
            Map.entry("cre", "minecraft:creeper"),
            Map.entry("sku", "minecraft:skull"),
            Map.entry("flo", "minecraft:flower"),
            Map.entry("moj", "minecraft:mojang"),
            Map.entry("glb", "minecraft:globe"),
            Map.entry("pig", "minecraft:piglin"),
            Map.entry("flow", "minecraft:flow"),
            Map.entry("guster", "minecraft:guster"));

    private LegacyBannerPatterns() {}

    static String getPatternId(String patternCode) {
        return PATTERN_IDS.getOrDefault(patternCode, "minecraft:" + patternCode);
    }
}
