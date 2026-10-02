/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.util;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class Lookup {
    public static Item getBannerFromDyeColor(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.BANNER.white();
            case ORANGE -> Items.BANNER.orange();
            case MAGENTA -> Items.BANNER.magenta();
            case LIGHT_BLUE -> Items.BANNER.lightBlue();
            case YELLOW -> Items.BANNER.yellow();
            case LIME -> Items.BANNER.lime();
            case PINK -> Items.BANNER.pink();
            case GRAY -> Items.BANNER.gray();
            case LIGHT_GRAY -> Items.BANNER.lightGray();
            case CYAN -> Items.BANNER.cyan();
            case PURPLE -> Items.BANNER.purple();
            case BLUE -> Items.BANNER.blue();
            case BROWN -> Items.BANNER.brown();
            case GREEN -> Items.BANNER.green();
            case RED -> Items.BANNER.red();
            case BLACK -> Items.BANNER.black();
        };
    }

    public static Item getItemFromDyeColor(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.DYE.white();
            case ORANGE -> Items.DYE.orange();
            case MAGENTA -> Items.DYE.magenta();
            case LIGHT_BLUE -> Items.DYE.lightBlue();
            case YELLOW -> Items.DYE.yellow();
            case LIME -> Items.DYE.lime();
            case PINK -> Items.DYE.pink();
            case GRAY -> Items.DYE.gray();
            case LIGHT_GRAY -> Items.DYE.lightGray();
            case CYAN -> Items.DYE.cyan();
            case PURPLE -> Items.DYE.purple();
            case BLUE -> Items.DYE.blue();
            case BROWN -> Items.DYE.brown();
            case GREEN -> Items.DYE.green();
            case RED -> Items.DYE.red();
            case BLACK -> Items.DYE.black();
        };
    }

    public static Item getItemFromPattern(Identifier pattern) {
        if (!Identifier.DEFAULT_NAMESPACE.equals(pattern.getNamespace())) return null;

        return switch (pattern.getPath()) {
            case "globe" -> Items.GLOBE_BANNER_PATTERN;
            case "creeper" -> Items.CREEPER_BANNER_PATTERN;
            case "skull" -> Items.SKULL_BANNER_PATTERN;
            case "flower" -> Items.FLOWER_BANNER_PATTERN;
            case "mojang" -> Items.MOJANG_BANNER_PATTERN;
            case "piglin" -> Items.PIGLIN_BANNER_PATTERN;
            case "flow" -> Items.FLOW_BANNER_PATTERN;
            case "guster" -> Items.GUSTER_BANNER_PATTERN;
            case "bricks" -> Items.FIELD_MASONED_BANNER_PATTERN;
            case "curly_border" -> Items.BORDURE_INDENTED_BANNER_PATTERN;
            default -> null;
        };
    }
}
