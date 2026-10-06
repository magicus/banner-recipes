/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import com.google.gson.Gson;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
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
    private static final String DEFAULT_DESCRIPTION = "Unnamed banner";
    private static final String DEFAULT_CATEGORY = "misc";
    private static final int MAX_INPUT_LENGTH = 1_048_576;
    private static final int MAX_NESTING = 128;
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

    /** errorPosition is a zero-based UTF-16 offset into the original input. */
    public record CommandParseResult(BannerRecipe recipe, String errorMessage, Integer errorPosition) {}

    private static final class CommandEnvelope {
        private static void read(Cursor cursor) {
            boolean slash = cursor.take('/');
            int commandStart = cursor.position;
            String command = cursor.peekWord();
            if (!command.equals("give") && !command.equals("minecraft:give")) {
                if (slash) throw cursor.failure("Expected /give", commandStart);
                return;
            }
            cursor.position += command.length();
            cursor.requireWhitespace();
            // A selector may contain quoted spaces, nested SNBT and commas.
            // Validate its framing; target resolution belongs to Minecraft.
            int targetStart = cursor.position;
            if (cursor.take('@')) {
                if (cursor.atEnd() || "paresn".indexOf(cursor.current()) < 0) {
                    throw cursor.failure("Invalid target selector", targetStart);
                }
                cursor.position++;
                if (cursor.is('[')) cursor.readBalanced();
            } else {
                String target = cursor.readWord();
                if (!target.matches("[A-Za-z0-9_]{1,16}|[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) {
                    throw cursor.failure("Invalid player name or UUID", targetStart);
                }
            }
            cursor.requireWhitespace();
        }
    }

    private static final class ItemParser {
        private static BannerRecipe read(Cursor cursor) {
            if (cursor.is('{')) {
                // Compatibility with the serialized item form emitted by the
                // previous converter. This is not a fourth historical give style.
                return ComponentsFormat.readSerializedItem(cursor);
            }
            int itemPosition = cursor.position;
            String item = normalizeId(cursor.readId(), itemPosition);
            if (item.equals("minecraft:banner")) return DamageFormat.read(cursor);
            DyeColor base = bannerColor(item, itemPosition);
            // A plain colored banner is shared by both newer formats.
            if (cursor.is('[')) return ComponentsFormat.read(cursor, base);
            return NbtFormat.read(cursor, base);
        }
    }

    /** 1.8-1.12: count and damage precede NBT; dye IDs run black -> white. */
    private static final class DamageFormat {
        private static BannerRecipe read(Cursor cursor) {
            int damage = 0;
            int damagePosition = cursor.position;
            CompoundTag itemTag = new CompoundTag();
            boolean separated = cursor.skipWhitespace();
            if (!cursor.atEnd() && !cursor.is('{')) {
                if (!separated) throw cursor.failure("Expected whitespace");
                cursor.readInteger(1, Integer.MAX_VALUE, "count");
                separated = cursor.skipWhitespace();
                if (!cursor.atEnd() && !cursor.is('{')) {
                    if (!separated) throw cursor.failure("Expected whitespace");
                    damagePosition = cursor.position;
                    damage = cursor.readInteger(0, 15, "banner damage");
                    separated = cursor.skipWhitespace();
                }
            }
            int tagPosition = cursor.position;
            if (cursor.is('{')) {
                if (!separated) throw cursor.failure("Expected whitespace before legacy NBT");
                itemTag = Snbt.compound(cursor.readBalanced());
            }
            cursor.requireEnd();
            CompoundTag block = optionalCompound(itemTag, "BlockEntityTag", tagPosition);
            // Accept historical generator output carrying Base in BlockEntityTag.
            // Base uses the same reversed dye numbering as damage and Color.
            if (block.contains("Base")) {
                damage = integer(block.get("Base"), "Base", tagPosition);
                damagePosition = tagPosition;
            }
            DyeColor base = numericColor(damage, true, damagePosition);
            return recipe(base, legacyLayers(block, true, tagPosition), DEFAULT_DESCRIPTION);
        }
    }

    /** 1.13-1.20.4: colored item ID, attached NBT; dye IDs run white -> black. */
    private static final class NbtFormat {
        private static BannerRecipe read(Cursor cursor, DyeColor base) {
            int tagPosition = cursor.position;
            CompoundTag block = new CompoundTag();
            if (cursor.is('{')) {
                CompoundTag item = Snbt.compound(cursor.readBalanced());
                block = optionalCompound(item, "BlockEntityTag", tagPosition);
            }
            cursor.readOptionalCount();
            cursor.requireEnd();
            return recipe(base, legacyLayers(block, false, tagPosition), DEFAULT_DESCRIPTION);
        }
    }

    /** 1.20.5+: component assignments; patterns and colors are named strings. */
    private static final class ComponentsFormat {
        private static BannerRecipe read(Cursor cursor, DyeColor base) {
            List<BannerRecipeLayer> layers = List.of();
            String description = DEFAULT_DESCRIPTION;
            Set<String> seen = new HashSet<>();
            cursor.expect('[');
            cursor.skipWhitespace();
            if (!cursor.take(']')) {
                do {
                    cursor.skipWhitespace();
                    int keyPosition = cursor.position;
                    boolean removed = cursor.take('!');
                    String key = normalizeId(cursor.readId(), keyPosition);
                    if (!seen.add(key)) throw cursor.failure("Duplicate component: " + key, keyPosition);
                    cursor.skipWhitespace();
                    if (!removed) {
                        cursor.expect('=');
                        cursor.skipWhitespace();
                        Slice value = cursor.readComponentValue();
                        Tag tag = Snbt.value(value);
                        if (key.equals("minecraft:banner_patterns")) {
                            layers = modernLayers(tag, value.start());
                        } else if (key.equals("minecraft:custom_name")) {
                            description = plainName(tag);
                        }
                    }
                    cursor.skipWhitespace();
                    if (cursor.take(']')) break;
                    cursor.expect(',');
                    cursor.skipWhitespace();
                    if (cursor.is(']')) throw cursor.failure("Expected component after comma");
                } while (true);
            }
            cursor.readOptionalCount();
            cursor.requireEnd();
            return recipe(base, layers, description);
        }

        private static BannerRecipe readSerializedItem(Cursor cursor) {
            int position = cursor.position;
            CompoundTag item = Snbt.compound(cursor.readBalanced());
            DyeColor base = bannerColor(normalizeId(string(item.get("id"), "id", position), position), position);
            CompoundTag components = optionalCompound(item, "components", position);
            Tag patterns = component(components, "banner_patterns", position);
            Tag name = component(components, "custom_name", position);
            if (item.contains("count")) {
                int count = integer(item.get("count"), "count", position);
                if (count < 1) throw new ParseFailure("Count must be positive", position);
            }
            cursor.requireEnd();
            return recipe(base, patterns == null ? List.of() : modernLayers(patterns, position), plainName(name));
        }

        private static Tag component(CompoundTag components, String name, int position) {
            if (components.contains(name) && components.contains("minecraft:" + name)) {
                throw new ParseFailure("Duplicate component: " + name, position);
            }
            return components.contains(name) ? components.get(name) : components.get("minecraft:" + name);
        }
    }

    private static List<BannerRecipeLayer> legacyLayers(CompoundTag block, boolean reversed, int position) {
        if (!block.contains("Patterns")) return List.of();
        ListTag patterns = list(block.get("Patterns"), "Patterns", position);
        List<BannerRecipeLayer> layers = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) {
            String context = "Patterns[" + i + "]";
            CompoundTag layer = compound(patterns.get(i), context, position);
            String code = string(layer.get("Pattern"), context + ".Pattern", position);
            String pattern = LegacyPatterns.id(code, position);
            DyeColor color =
                    numericColor(integer(layer.get("Color"), context + ".Color", position), reversed, position);
            layers.add(layer(pattern, color, position));
        }
        return List.copyOf(layers);
    }

    private static List<BannerRecipeLayer> modernLayers(Tag value, int position) {
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

    private static BannerRecipeLayer layer(String pattern, DyeColor color, int position) {
        try {
            return BannerRecipeLayer.of(pattern, color.getName());
        } catch (IllegalArgumentException exception) {
            throw new ParseFailure("Invalid banner layer: " + exception.getMessage(), position);
        }
    }

    private static BannerRecipe recipe(DyeColor base, List<BannerRecipeLayer> layers, String description) {
        return new BannerRecipe(null, description, null, null, DEFAULT_CATEGORY, base.getName(), layers);
    }

    private static DyeColor bannerColor(String item, int position) {
        if (!item.startsWith("minecraft:") || !item.endsWith("_banner")) {
            throw new ParseFailure("Expected a vanilla banner item", position);
        }
        String name = item.substring("minecraft:".length(), item.length() - "_banner".length());
        DyeColor color = DyeColor.byName(name, null);
        if (color == null) throw new ParseFailure("Invalid banner color: " + name, position);
        return color;
    }

    private static String normalizeId(String id, int position) {
        String normalized = id.contains(":") ? id : "minecraft:" + id;
        if (!RESOURCE_ID.matcher(normalized).matches()) {
            throw new ParseFailure("Invalid resource ID: " + id, position);
        }
        return normalized;
    }

    private static DyeColor numericColor(int value, boolean reversed, int position) {
        if (value < 0 || value > 15) throw new ParseFailure("Dye ID must be between 0 and 15", position);
        return DyeColor.byId(reversed ? 15 - value : value);
    }

    private static CompoundTag optionalCompound(CompoundTag parent, String key, int position) {
        return parent.contains(key) ? compound(parent.get(key), key, position) : new CompoundTag();
    }

    private static CompoundTag compound(Tag value, String field, int position) {
        if (value instanceof CompoundTag compound) return compound;
        throw new ParseFailure(field + " must be a compound", position);
    }

    private static ListTag list(Tag value, String field, int position) {
        if (value instanceof ListTag list) return list;
        throw new ParseFailure(field + " must be a list", position);
    }

    private static String string(Tag value, String field, int position) {
        if (value instanceof StringTag string) return string.value();
        throw new ParseFailure(field + " must be a string", position);
    }

    private static int integer(Tag value, String field, int position) {
        if (value instanceof IntTag integer) return integer.value();
        if (value instanceof ByteTag integer) return integer.value();
        if (value instanceof ShortTag integer) return integer.value();
        throw new ParseFailure(field + " must be an integer", position);
    }

    private static String plainName(Tag value) {
        // BannerRecipe stores plain text, not a styled/translatable component.
        // Keep simple text names; leave richer components to Minecraft.
        String name = null;
        if (value instanceof StringTag string) name = string.value();
        if (value instanceof CompoundTag compound && compound.get("text") instanceof StringTag text) {
            name = text.value();
        }
        return name == null || name.isBlank() ? DEFAULT_DESCRIPTION : name;
    }

    /** Complete legacy short-code table; unknown codes must not silently disappear. */
    private static final class LegacyPatterns {
        private static String id(String code, int position) {
            String path =
                    switch (code) {
                        case "b" -> "base";
                        case "bl" -> "square_bottom_left";
                        case "br" -> "square_bottom_right";
                        case "tl" -> "square_top_left";
                        case "tr" -> "square_top_right";
                        case "bs" -> "stripe_bottom";
                        case "ts" -> "stripe_top";
                        case "ls" -> "stripe_left";
                        case "rs" -> "stripe_right";
                        case "cs" -> "stripe_center";
                        case "ms" -> "stripe_middle";
                        case "drs" -> "stripe_downright";
                        case "dls" -> "stripe_downleft";
                        case "ss" -> "small_stripes";
                        case "cr" -> "cross";
                        case "sc" -> "straight_cross";
                        case "bt" -> "triangle_bottom";
                        case "tt" -> "triangle_top";
                        case "bts" -> "triangles_bottom";
                        case "tts" -> "triangles_top";
                        case "ld" -> "diagonal_left";
                        case "rd" -> "diagonal_up_right";
                        case "lud" -> "diagonal_up_left";
                        case "rud" -> "diagonal_right";
                        case "mc" -> "circle";
                        case "mr" -> "rhombus";
                        case "vh" -> "half_vertical";
                        case "hh" -> "half_horizontal";
                        case "vhr" -> "half_vertical_right";
                        case "hhb" -> "half_horizontal_bottom";
                        case "bo" -> "border";
                        case "cbo" -> "curly_border";
                        case "gra" -> "gradient";
                        case "gru" -> "gradient_up";
                        case "bri" -> "bricks";
                        case "glb" -> "globe";
                        case "cre" -> "creeper";
                        case "sku" -> "skull";
                        case "flo" -> "flower";
                        case "moj" -> "mojang";
                        case "pig" -> "piglin";
                        default -> throw new ParseFailure("Unknown legacy banner pattern: " + code, position);
                    };
            return "minecraft:" + path;
        }
    }

    private record Slice(String text, int start) {}

    private static final class Snbt {
        private static CompoundTag compound(Slice slice) {
            try {
                return TagParser.parseCompoundFully(slice.text());
            } catch (CommandSyntaxException exception) {
                throw syntaxError(exception, slice.start(), 0, slice.text().length());
            }
        }

        private static Tag value(Slice slice) {
            String prefix = "{value:";
            try {
                return TagParser.parseCompoundFully(prefix + slice.text() + "}").get("value");
            } catch (CommandSyntaxException exception) {
                throw syntaxError(
                        exception, slice.start(), prefix.length(), slice.text().length());
            }
        }

        private static ParseFailure syntaxError(CommandSyntaxException exception, int start, int prefix, int length) {
            int local = exception.getCursor() < 0 ? 0 : exception.getCursor() - prefix;
            return new ParseFailure(
                    exception.getRawMessage().getString(), start + Math.max(0, Math.min(local, length)));
        }
    }

    /** A bounded cursor for delimiters and offsets, not a replacement SNBT parser. */
    private static final class Cursor {
        private final String input;
        private int position;

        private Cursor(String input) {
            this.input = input;
        }

        private boolean atEnd() {
            return position == input.length();
        }

        private char current() {
            return input.charAt(position);
        }

        private boolean is(char value) {
            return !atEnd() && current() == value;
        }

        private boolean take(char value) {
            if (!is(value)) return false;
            position++;
            return true;
        }

        private void expect(char value) {
            if (!take(value)) throw failure("Expected '" + value + "'");
        }

        private boolean skipWhitespace() {
            int start = position;
            while (!atEnd() && Character.isWhitespace(current())) position++;
            return position != start;
        }

        private void requireWhitespace() {
            if (!skipWhitespace()) throw failure("Expected whitespace");
        }

        private void requireEnd() {
            skipWhitespace();
            if (!atEnd()) throw failure("Unexpected trailing input");
        }

        private String peekWord() {
            int end = position;
            while (end < input.length() && !Character.isWhitespace(input.charAt(end))) end++;
            return input.substring(position, end);
        }

        private String readWord() {
            String word = peekWord();
            if (word.isEmpty()) throw failure("Expected a word");
            position += word.length();
            return word;
        }

        private String readId() {
            int start = position;
            while (!atEnd()
                    && (current() >= 'a' && current() <= 'z'
                            || current() >= '0' && current() <= '9'
                            || "_./:-".indexOf(current()) >= 0)) position++;
            if (position == start) throw failure("Expected resource ID");
            return input.substring(start, position);
        }

        private int readInteger(int minimum, int maximum, String label) {
            int start = position;
            String token = readWord();
            try {
                if (!token.matches("[0-9]+")) throw new NumberFormatException();
                int value = Integer.parseInt(token);
                if (value < minimum || value > maximum) throw new NumberFormatException();
                return value;
            } catch (NumberFormatException exception) {
                throw failure("Invalid " + label + ": " + token, start);
            }
        }

        private void readOptionalCount() {
            boolean separated = skipWhitespace();
            if (atEnd()) return;
            if (!separated) throw failure("Expected whitespace before count");
            readInteger(1, Integer.MAX_VALUE, "count");
        }

        private Slice readBalanced() {
            int start = position;
            if (!is('{') && !is('[') && !is('(')) throw failure("Expected opening delimiter");
            ArrayDeque<Character> closers = new ArrayDeque<>();
            do {
                char character = current();
                if (character == '\'' || character == '"') {
                    readQuoted();
                    continue;
                }
                position++;
                switch (character) {
                    case '{' -> closers.push('}');
                    case '[' -> closers.push(']');
                    case '(' -> closers.push(')');
                    case '}', ']', ')' -> {
                        if (closers.isEmpty() || closers.pop() != character) {
                            throw failure("Mismatched closing delimiter", position - 1);
                        }
                    }
                    default -> {}
                }
                if (closers.size() > MAX_NESTING) throw failure("Nesting is too deep");
                if (closers.isEmpty()) return new Slice(input.substring(start, position), start);
            } while (!atEnd());
            throw failure("Unclosed delimiter", position);
        }

        private void readQuoted() {
            int start = position;
            char quote = current();
            position++;
            while (!atEnd()) {
                char character = current();
                position++;
                if (character == quote) return;
                if (character == '\\') {
                    if (atEnd()) throw failure("Unfinished escape sequence");
                    position++;
                }
            }
            throw failure("Unclosed string", start);
        }

        private Slice readComponentValue() {
            int start = position;
            while (!atEnd() && !is(',') && !is(']')) {
                if (is('{') || is('[') || is('(')) readBalanced();
                else if (is('"') || is('\'')) readQuoted();
                else if (is('}') || is(')')) throw failure("Unexpected closing delimiter");
                else position++;
            }
            int end = position;
            while (end > start && Character.isWhitespace(input.charAt(end - 1))) end--;
            if (end == start) throw failure("Expected component value", start);
            return new Slice(input.substring(start, end), start);
        }

        private ParseFailure failure(String message) {
            return failure(message, position);
        }

        private ParseFailure failure(String message, int offset) {
            return new ParseFailure(message, offset);
        }
    }

    private static final class ParseFailure extends IllegalArgumentException {
        private final int position;

        private ParseFailure(String message, int position) {
            super(message);
            this.position = position;
        }
    }
}
