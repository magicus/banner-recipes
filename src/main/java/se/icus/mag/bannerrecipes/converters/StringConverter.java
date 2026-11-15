/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.converters.command.BannerItemConverter;
import se.icus.mag.bannerrecipes.converters.command.CommandConverter;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceException;
import se.icus.mag.bannerrecipes.converters.url.UrlConverter;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

/**
 * Parses a free-form string into a {@link BannerRecipe}. The string is interpreted as a URL if it starts
 * with {@code http://} or {@code https://}, as a {@code /give} command if it starts with {@code give} or
 * {@code /give}, and otherwise as a banner item description.
 */
public class StringConverter {
    private static final Pattern URL = Pattern.compile("^https?://.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern COMMAND = Pattern.compile("^/?give(\\s.*)?$", Pattern.DOTALL);

    public static BannerRecipe fromString(String input) throws OnlineServiceException, CommandSyntaxException {
        return fromString(input, BannerRecipesMod.getRegistryAccess(Minecraft.getInstance()));
    }

    public static BannerRecipe fromString(String input, HolderLookup.Provider registries)
            throws OnlineServiceException, CommandSyntaxException {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input cannot be empty");
        }

        String text = input.strip();

        if (URL.matcher(text.strip()).matches()) {
            return UrlConverter.fromUrl(text);
        }

        try {
            if (COMMAND.matcher(text).matches()) {
                return CommandConverter.fromCommand(text, registries);
            }
            return BannerItemConverter.fromBannerItem(text, registries);
        } catch (CommandSyntaxException e) {
            throw ParseErrors.rebase(e, ParseErrors.leadingWhitespace(input), input);
        }
    }
}
