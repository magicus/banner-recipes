/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

class CommandConverterTest {
    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createWorldLookup();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(pending -> pending.apply());
    }

    private static BannerRecipe recipe(String description, String author) {
        return new BannerRecipe(
                null,
                description,
                author,
                null,
                null,
                "light_blue",
                List.of(
                        BannerRecipeLayer.of("minecraft:cross", "white"),
                        BannerRecipeLayer.of("minecraft:border", "red")));
    }

    @Test
    void generatesGiveCommand() {
        assertEquals(
                "/give @p light_blue_banner[banner_patterns=[{pattern:\"minecraft:cross\",color:\"white\"},"
                        + "{pattern:\"minecraft:border\",color:\"red\"}],custom_name={text:\"My Banner\"},"
                        + "lore=[{text:\"Me\"}]]",
                CommandConverter.toCommand(recipe("My Banner", "Me")));
    }

    @Test
    void omitsMissingMetadata() {
        assertEquals(
                "/give @p light_blue_banner[banner_patterns=[{pattern:\"minecraft:cross\",color:\"white\"},"
                        + "{pattern:\"minecraft:border\",color:\"red\"}]]",
                CommandConverter.toCommand(recipe(null, " ")));
    }

    @Test
    void roundTrips() throws Exception {
        BannerRecipe original = recipe("Say \"hi\" \\o/", "Me");

        BannerRecipe parsed = CommandConverter.fromCommand(CommandConverter.toCommand(original), registries);

        assertEquals(original.bannerColor(), parsed.bannerColor());
        assertEquals(original.description(), parsed.description());
        assertEquals(original.author(), parsed.author());
        assertEquals(original.layers(), parsed.layers());
    }
}
