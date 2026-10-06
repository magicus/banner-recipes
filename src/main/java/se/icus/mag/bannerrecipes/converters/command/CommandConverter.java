/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

/**
 * Imports Java Edition banner item descriptions in three formats:
 *
 * <pre>
 * 1.8:    banner [count [damage]] [{BlockEntityTag:{Patterns:[...]}}]
 * 1.13:   blue_banner[{BlockEntityTag:{Patterns:[...]}}] [count]
 * 1.20.5: blue_banner[banner_patterns=[...]] [count]
 * </pre>
 *
 * CommandEnvelope handles the optional give command. ItemParser selects exactly
 * one format; the selected parser must consume the entire description.
 * Cursor handles framing only. Minecraft's TagParser handles SNBT syntax.
 * Unrelated NBT/components are syntax-checked but not retained in BannerRecipe.
 * Pattern registry existence is deliberately not checked here: datapacks may
 * supply patterns, and importing a recipe must not require a running world.
 */
public final class CommandConverter {
    static final String DEFAULT_DESCRIPTION = "Unnamed banner";
    private static final String DEFAULT_CATEGORY = "misc";
    private static final int MAX_INPUT_LENGTH = 1_048_576;
    static final int MAX_NESTING = 128;
    private static final Gson GSON = new Gson();
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private CommandConverter() {}

    /** Returns null on invalid input, for compatibility with the original API. */
    public static BannerRecipe fromCommand(String command) {
        return parseCommandDetailed(command).recipe();
    }

    public static CommandParseResult parseCommandDetailed(String input) {
        return parse(input, true);
    }

    /** Parses only an item description, without interpreting a give prefix. */
    public static CommandParseResult parseItemDescriptionDetailed(String input) {
        return parse(input, false);
    }

    public static BannerRecipe fromItemDescription(String input) {
        return parseItemDescriptionDetailed(input).recipe();
    }

    public static String toCommand(BannerRecipe recipe) {
        return "/give @p " + toItemDescription(recipe);
    }

    /** Emits component syntax, with explicit namespaces and quoted pattern IDs. */
    public static String toItemDescription(BannerRecipe recipe) {
        if (recipe == null) throw new IllegalArgumentException("Recipe is required");
        DyeColor base = DyeColor.byName(recipe.bannerColor(), null);
        if (base == null) throw new IllegalArgumentException("Invalid banner color");

        List<String> components = new ArrayList<>();
        if (!recipe.layers().isEmpty()) {
            List<String> layers = new ArrayList<>();
            for (BannerRecipeLayer layer : recipe.layers()) {
                String pattern = normalizeId(layer.pattern().toString(), 0);
                layers.add("{pattern:" + GSON.toJson(pattern) + ",color:"
                        + GSON.toJson(layer.color().getName()) + "}");
            }
            components.add("minecraft:banner_patterns=[" + String.join(",", layers) + "]");
        }
        if (recipe.description() != null
                && !recipe.description().isBlank()
                && !DEFAULT_DESCRIPTION.equals(recipe.description())) {
            // Plain text component syntax used by current Minecraft versions.
            components.add("minecraft:custom_name={text:" + GSON.toJson(recipe.description()) + "}");
        }
        String item = "minecraft:" + base.getName() + "_banner";
        return components.isEmpty() ? item : item + "[" + String.join(",", components) + "]";
    }

    private static CommandParseResult parse(String input, boolean allowCommand) {
        if (input == null) return new CommandParseResult(null, "Input is required", null);
        if (input.length() > MAX_INPUT_LENGTH) {
            return new CommandParseResult(null, "Input is too long", MAX_INPUT_LENGTH);
        }
        Cursor cursor = new Cursor(input);
        try {
            cursor.skipWhitespace();
            if (allowCommand) CommandEnvelope.read(cursor);
            BannerRecipe recipe = ItemParser.read(cursor);
            cursor.requireEnd();
            return new CommandParseResult(recipe, null, null);
        } catch (ParseFailure failure) {
            return new CommandParseResult(null, failure.getMessage(), failure.position);
        }
    }

    static List<BannerRecipeLayer> legacyLayers(CompoundTag block, boolean reversed, int position) {
        if (!block.contains("Patterns")) return List.of();
        ListTag patterns = list(block.get("Patterns"), "Patterns", position);
        List<BannerRecipeLayer> layers = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) {
            String context = "Patterns[" + i + "]";
            CompoundTag layer = compound(patterns.get(i), context, position);
            String code = string(layer.get("Pattern"), context + ".Pattern", position);
            String pattern = LegacyBannerPatterns.id(code, position);
            DyeColor color =
                    numericColor(integer(layer.get("Color"), context + ".Color", position), reversed, position);
            layers.add(layer(pattern, color, position));
        }
        return List.copyOf(layers);
    }

    static List<BannerRecipeLayer> modernLayers(Tag value, int position) {
        ListTag patterns = list(value, "banner_patterns", position);
        List<BannerRecipeLayer> layers = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) {
            String context = "banner_patterns[" + i + "]";
            CompoundTag entry = compound(patterns.get(i), context, position);
            String pattern = normalizeId(string(entry.get("pattern"), context + ".pattern", position), position);
            String colorName = string(entry.get("color"), context + ".color", position);
            DyeColor color = DyeColor.byName(colorName, null);
            if (color == null) throw new ParseFailure("Invalid dye color: " + colorName, position);
            layers.add(layer(pattern, color, position));
        }
        return List.copyOf(layers);
    }

    static BannerRecipeLayer layer(String pattern, DyeColor color, int position) {
        try {
            return BannerRecipeLayer.of(pattern, color.getName());
        } catch (IllegalArgumentException exception) {
            throw new ParseFailure("Invalid banner layer: " + exception.getMessage(), position);
        }
    }

    static BannerRecipe recipe(DyeColor base, List<BannerRecipeLayer> layers, String description) {
        return new BannerRecipe(null, description, null, null, DEFAULT_CATEGORY, base.getName(), layers);
    }

    static DyeColor bannerColor(String item, int position) {
        if (!item.startsWith("minecraft:") || !item.endsWith("_banner")) {
            throw new ParseFailure("Expected a vanilla banner item", position);
        }
        String name = item.substring("minecraft:".length(), item.length() - "_banner".length());
        DyeColor color = DyeColor.byName(name, null);
        if (color == null) throw new ParseFailure("Invalid banner color: " + name, position);
        return color;
    }

    static String normalizeId(String id, int position) {
        String normalized = id.contains(":") ? id : "minecraft:" + id;
        if (!RESOURCE_ID.matcher(normalized).matches()) {
            throw new ParseFailure("Invalid resource ID: " + id, position);
        }
        return normalized;
    }

    static DyeColor numericColor(int value, boolean reversed, int position) {
        if (value < 0 || value > 15) throw new ParseFailure("Dye ID must be between 0 and 15", position);
        return DyeColor.byId(reversed ? 15 - value : value);
    }

    static CompoundTag optionalCompound(CompoundTag parent, String key, int position) {
        return parent.contains(key) ? compound(parent.get(key), key, position) : new CompoundTag();
    }

    static CompoundTag compound(Tag value, String field, int position) {
        if (value instanceof CompoundTag compound) return compound;
        throw new ParseFailure(field + " must be a compound", position);
    }

    static ListTag list(Tag value, String field, int position) {
        if (value instanceof ListTag list) return list;
        throw new ParseFailure(field + " must be a list", position);
    }

    static String string(Tag value, String field, int position) {
        if (value instanceof StringTag string) return string.value();
        throw new ParseFailure(field + " must be a string", position);
    }

    static int integer(Tag value, String field, int position) {
        if (value instanceof IntTag integer) return integer.value();
        if (value instanceof ByteTag integer) return integer.value();
        if (value instanceof ShortTag integer) return integer.value();
        throw new ParseFailure(field + " must be an integer", position);
    }

    static String plainName(Tag value) {
        // BannerRecipe stores plain text, not a styled/translatable component.
        // Keep simple text names; leave richer components to Minecraft.
        String name = null;
        if (value instanceof StringTag string) name = string.value();
        if (value instanceof CompoundTag compound && compound.get("text") instanceof StringTag text) {
            name = text.value();
        }
        return name == null || name.isBlank() ? DEFAULT_DESCRIPTION : name;
    }
}
