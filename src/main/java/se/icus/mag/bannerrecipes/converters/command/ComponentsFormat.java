/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

/**
 * 1.20.5+: component assignments; patterns and colors are named strings.
 */
final class ComponentsFormat {
    static BannerRecipe read(Cursor cursor, DyeColor base) {
        List<BannerRecipeLayer> layers = List.of();
        String description = CommandConverter.DEFAULT_DESCRIPTION;
        Set<String> seen = new HashSet<>();
        cursor.expect('[');
        cursor.skipWhitespace();
        if (!cursor.take(']')) {
            do {
                cursor.skipWhitespace();
                int keyPosition = cursor.position;
                boolean removed = cursor.take('!');
                String key = CommandConverter.normalizeId(cursor.readId(), keyPosition);
                if (!seen.add(key)) {
                    throw cursor.failure("Duplicate component: " + key, keyPosition);
                }
                cursor.skipWhitespace();
                if (!removed) {
                    cursor.expect('=');
                    cursor.skipWhitespace();
                    Slice value = cursor.readComponentValue();
                    Tag tag = Snbt.value(value);
                    if (key.equals("minecraft:banner_patterns")) {
                        layers = CommandConverter.modernLayers(tag, value.start());
                    } else if (key.equals("minecraft:custom_name")) {
                        description = CommandConverter.plainName(tag);
                    }
                }
                cursor.skipWhitespace();
                if (cursor.take(']')) {
                    break;
                }
                cursor.expect(',');
                cursor.skipWhitespace();
                if (cursor.is(']')) {
                    throw cursor.failure("Expected component after comma");
                }
            } while (true);
        }
        cursor.readOptionalCount();
        cursor.requireEnd();
        return CommandConverter.recipe(base, layers, description);
    }

    static BannerRecipe readSerializedItem(Cursor cursor) {
        int position = cursor.position;
        CompoundTag item = Snbt.compound(cursor.readBalanced());
        DyeColor base = CommandConverter.bannerColor(
                CommandConverter.normalizeId(CommandConverter.string(item.get("id"), "id", position), position),
                position);
        CompoundTag components = CommandConverter.optionalCompound(item, "components", position);
        Tag patterns = component(components, "banner_patterns", position);
        Tag name = component(components, "custom_name", position);
        if (item.contains("count")) {
            int count = CommandConverter.integer(item.get("count"), "count", position);
            if (count < 1) {
                throw new ParseFailure("Count must be positive", position);
            }
        }
        cursor.requireEnd();
        return CommandConverter.recipe(
                base,
                patterns == null ? List.of() : CommandConverter.modernLayers(patterns, position),
                CommandConverter.plainName(name));
    }

    static Tag component(CompoundTag components, String name, int position) {
        if (components.contains(name) && components.contains("minecraft:" + name)) {
            throw new ParseFailure("Duplicate component: " + name, position);
        }
        return components.contains(name) ? components.get(name) : components.get("minecraft:" + name);
    }
}
