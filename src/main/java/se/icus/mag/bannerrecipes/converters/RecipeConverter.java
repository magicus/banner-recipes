/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.converters.command.BannerItemConverter;
import se.icus.mag.bannerrecipes.converters.command.CommandConverter;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceException;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceType;
import se.icus.mag.bannerrecipes.converters.url.UrlConverter;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

/**
 * Converts between {@link BannerRecipe}s and text.
 *
 * <p>Input text is interpreted as a URL if it starts with {@code http://} or {@code https://}, as a {@code /give}
 * command if it starts with {@code give} or {@code /give}, and otherwise as a banner item description.
 */
public final class RecipeConverter {
    private static final Pattern URL = Pattern.compile("^https?://.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern COMMAND = Pattern.compile("^/?give(\\s.*)?$", Pattern.DOTALL);

    private RecipeConverter() {}

    /**
     * The outcome of {@link #parse(String)}. Both {@code recipe} and {@code error} are null if there was no input.
     *
     * @param recipe the parsed recipe, or null if the input could not be parsed
     * @param error why the input could not be parsed, or null if it could
     */
    public record ParseResult(BannerRecipe recipe, Component error) {}

    /** Parses text using the registries of the running game, reporting failures in the result instead of throwing. */
    public static ParseResult parse(String text) {
        if (text == null || text.isBlank()) {
            return new ParseResult(null, null);
        }

        try {
            BannerRecipe recipe = fromString(text, BannerRecipesMod.getRegistryAccess(Minecraft.getInstance()));
            return new ParseResult(recipe, null);
        } catch (CommandSyntaxException e) {
            if (e.getCursor() >= 0) {
                return new ParseResult(
                        null, Component.translatable("banner-recipes.banner.convert.error_at_pos", e.getCursor()));
            }
            return new ParseResult(null, messageOrDefault(e.getRawMessage().getString()));
        } catch (OnlineServiceException | RuntimeException e) {
            return new ParseResult(null, messageOrDefault(e.getMessage()));
        }
    }

    /** Parses text into a recipe, throwing if the text is blank or cannot be parsed. */
    public static BannerRecipe fromString(String input, HolderLookup.Provider registries)
            throws OnlineServiceException, CommandSyntaxException {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input cannot be empty");
        }

        String text = input.strip();

        if (URL.matcher(text).matches()) {
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

    /**
     * Returns the recipe as a {@code /give} command if {@code service} is null, otherwise as a URL to the service.
     * Returns null if there is no recipe or it cannot be expressed.
     */
    public static String export(BannerRecipe recipe, OnlineServiceType service) {
        if (recipe == null) {
            return null;
        }
        if (service == null) {
            return CommandConverter.toCommand(recipe);
        }

        try {
            return UrlConverter.toUrl(recipe, service);
        } catch (OnlineServiceException | RuntimeException e) {
            return null;
        }
    }

    private static Component messageOrDefault(String message) {
        return message == null || message.isBlank()
                ? Component.translatable("banner-recipes.banner.convert.invalid_syntax")
                : Component.literal(message);
    }
}
