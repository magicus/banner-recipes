/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.HolderLookup;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

/**
 * Converts between Minecraft {@code /give} commands and {@link BannerRecipe}.
 * When parsing, the leading slash is optional. The target selector is skipped and the remaining item description is handled by {@link BannerItemConverter}.
 */
public class CommandConverter {
    public static BannerRecipe fromCommand(String input, HolderLookup.Provider registries)
            throws CommandSyntaxException {
        StringReader reader = new StringReader(input.strip());

        if (reader.canRead() && reader.peek() == '/') {
            reader.skip();
        }

        String command = readToken(reader);
        if (!command.equals("give")) {
            throw new IllegalArgumentException("Not a give command: " + input);
        }

        reader.skipWhitespace();
        skipTarget(reader);
        reader.skipWhitespace();

        if (!reader.canRead()) {
            throw new IllegalArgumentException("Missing item in give command: " + input);
        }

        return BannerItemConverter.fromBannerItem(reader.getRemaining(), registries);
    }

    public static String toCommand(BannerRecipe recipe) {
        String bannerString = BannerItemConverter.toBannerItem(recipe);
        return "/give @p " + bannerString;
    }

    private static String readToken(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    // A target is a name or a selector like @p or @a[name="a b",limit=1], which may contain spaces.
    private static void skipTarget(StringReader reader) {
        int depth = 0;
        char quote = 0;

        while (reader.canRead()) {
            char c = reader.peek();

            if (quote != 0) {
                if (c == '\\' && reader.canRead(2)) {
                    reader.skip();
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
            } else if (Character.isWhitespace(c) && depth <= 0) {
                return;
            }
            reader.skip();
        }
    }
}
