/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;
import se.icus.mag.bannerrecipes.util.Lookup;

/**
 * Parses Minecraft banner item descriptions from commands into {@link BannerRecipe}.
 *
 * <table>
 * <caption>Supported command formats</caption>
 * <tr><th>Format</th>   <th>Versions</th>     <th>Example</th></tr>
 * <tr><td>Legacy</td>   <td>1.8–1.12.2</td>   <td>{@code banner 1 5 {BlockEntityTag:{Patterns:[...]}}}</td></tr>
 * <tr><td>NBT</td>      <td>1.13–1.20.4</td>  <td>{@code blue_banner{BlockEntityTag:{Patterns:[...]}}}</td></tr>
 * <tr><td>Component</td><td>1.20.5–1.21.4</td><td>{@code blue_banner[banner_patterns=[...],custom_name='{"text":"Name"}']}</td></tr>
 * <tr><td>Modern</td>   <td>1.21.5+</td>      <td>{@code blue_banner[banner_patterns=[...],custom_name={text:"Name"}]}</td></tr>
 * </table>
 *
 * <p>Legacy and NBT formats are parsed using Minecraft's {@code TagParser},
 * with manual conversion of pattern IDs and dye colors.
 * Component and Modern formats use {@code ItemParser}, with additional
 * JSON text decoding for the older Component format.
 * Banners without patterns are rejected.
 */
public class BannerItemConverter {
    public static BannerRecipe fromBannerItem(String input, HolderLookup.Provider registries)
            throws CommandSyntaxException {
        StringReader reader = new StringReader(input.strip());
        int start = reader.getCursor();

        while (reader.canRead()) {
            char c = reader.peek();
            if (!Character.isLetterOrDigit(c) && c != '_' && c != ':' && c != '/') {
                break;
            }
            reader.skip();
        }

        String itemId = reader.getString().substring(start, reader.getCursor());
        String itemName = itemId.substring(itemId.indexOf(':') + 1);

        if (itemName.equals("banner")) {
            return parseOldBanner(reader, true, null);
        }

        if (!itemName.endsWith("_banner")) {
            throw new IllegalArgumentException("Not a banner: " + itemId);
        }

        String color = itemName.substring(0, itemName.length() - 7);

        if (reader.canRead() && reader.peek() == '{') {
            return parseOldBanner(reader, false, color);
        }

        return parseComponentBanner(input, color, registries);
    }

    public static String toBannerItem(BannerRecipe recipe) {
        StringBuilder patterns = new StringBuilder();
        for (BannerRecipeLayer layer : recipe.layers()) {
            if (!patterns.isEmpty()) {
                patterns.append(',');
            }
            patterns.append("{pattern:")
                    .append(quote(layer.pattern().toString()))
                    .append(",color:")
                    .append(quote(layer.color().getName()))
                    .append('}');
        }

        StringBuilder components =
                new StringBuilder("banner_patterns=[").append(patterns).append(']');
        if (recipe.description() != null && !recipe.description().isBlank()) {
            components
                    .append(",custom_name={text:")
                    .append(quote(recipe.description()))
                    .append('}');
        }
        if (recipe.author() != null && !recipe.author().isBlank()) {
            components.append(",lore=[{text:").append(quote(recipe.author())).append("}]");
        }

        String bannerString = recipe.bannerColor() + "_banner[" + components + "]";
        return bannerString;
    }

    private static BannerRecipe parseOldBanner(StringReader reader, boolean legacy, String color)
            throws CommandSyntaxException {
        int damage = 0;

        if (legacy) {
            reader.skipWhitespace();
            if (reader.canRead() && Character.isDigit(reader.peek())) {
                reader.readInt(); // count
                reader.skipWhitespace();

                if (reader.canRead() && Character.isDigit(reader.peek())) {
                    damage = reader.readInt();
                }
            }
            color = dyeColor(15 - damage).getName();
        }

        reader.skipWhitespace();

        CompoundTag nbt = reader.canRead() && reader.peek() == '{'
                ? TagParser.parseCompoundAsArgument(reader)
                : new CompoundTag();

        CompoundTag blockEntity = nbt.getCompoundOrEmpty("BlockEntityTag");
        CompoundTag display = nbt.getCompoundOrEmpty("display");

        if (blockEntity.getInt("Base").isPresent()) {
            color = dyeColor(15 - blockEntity.getIntOr("Base", 0)).getName();
        }

        List<BannerRecipeLayer> layers = new ArrayList<>();

        for (Tag tag : blockEntity.getListOrEmpty("Patterns")) {
            if (!(tag instanceof CompoundTag pattern)) {
                continue;
            }

            String code = pattern.getStringOr("Pattern", "");
            String name = Lookup.convertLegacyPattern(code);

            if (name == null) {
                throw new IllegalArgumentException("Unknown pattern: " + code);
            }

            int colorId = pattern.getIntOr("Color", 0);
            layers.add(new BannerRecipeLayer(Identifier.fromNamespaceAndPath("minecraft", name), dyeColor(colorId)));
        }

        String description = decodeText(display.getStringOr("Name", ""));
        String author = null;

        ListTag lore = display.getListOrEmpty("Lore");
        if (!lore.isEmpty() && lore.get(0) instanceof StringTag line) {
            author = decodeText(line.value());
        }

        return recipe(description, author, color, layers);
    }

    private static BannerRecipe parseComponentBanner(String input, String color, HolderLookup.Provider registries)
            throws CommandSyntaxException {
        ItemInput result = new ItemParser(registries).parse(new StringReader(input));
        DataComponentPatch components = result.components();
        DataComponentMap defaults = result.item().value().components();
        List<BannerRecipeLayer> layers = new ArrayList<>();

        BannerPatternLayers patterns = components.get(defaults, DataComponents.BANNER_PATTERNS);
        if (patterns != null) {
            for (BannerPatternLayers.Layer layer : patterns.layers()) {
                String id =
                        layer.pattern().unwrapKey().orElseThrow().identifier().toString();

                layers.add(new BannerRecipeLayer(Identifier.parse(id), layer.color()));
            }
        }

        Component name = components.get(defaults, DataComponents.CUSTOM_NAME);
        String description = name != null ? decodeText(name.getString()) : null;

        ItemLore lore = components.get(defaults, DataComponents.LORE);
        String author = lore != null && !lore.lines().isEmpty()
                ? decodeText(lore.lines().getFirst().getString())
                : null;

        return recipe(description, author, color, layers);
    }

    private static BannerRecipe recipe(
            String description, String author, String color, List<BannerRecipeLayer> layers) {
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("Banner must have at least one pattern");
        }

        return new BannerRecipe(null, description, author, null, null, color, List.copyOf(layers));
    }

    private static DyeColor dyeColor(int id) {
        if (id < 0 || id > 15) {
            throw new IllegalArgumentException("Invalid dye color: " + id);
        }
        return DyeColor.byId(id);
    }

    private static String decodeText(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // Legacy plain text and modern decoded components
        if (!value.startsWith("{") && !value.startsWith("[")) {
            return value;
        }

        // JSON text components from 1.13–1.21.4
        try {
            JsonElement json = JsonParser.parseString(value);
            return flattenJsonText(json);
        } catch (JsonParseException e) {
            return value;
        }
    }

    private static String flattenJsonText(JsonElement json) {
        if (json.isJsonPrimitive()) {
            return json.getAsString();
        }
        if (json.isJsonArray()) {
            StringBuilder result = new StringBuilder();
            for (JsonElement element : json.getAsJsonArray()) {
                result.append(flattenJsonText(element));
            }
            return result.toString();
        }
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            StringBuilder result = new StringBuilder();

            if (obj.has("text")) {
                result.append(obj.get("text").getAsString());
            }
            if (obj.has("extra")) {
                result.append(flattenJsonText(obj.get("extra")));
            }
            return result.toString();
        }
        return "";
    }

    private static String quote(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
