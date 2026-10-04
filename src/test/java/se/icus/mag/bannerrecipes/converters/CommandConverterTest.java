/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.icus.mag.bannerrecipes.converters.command.CommandConverter;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

class CommandConverterTest {
    @Test
    @DisplayName("STYLE_1_20_5 imports the prototype item-object component format")
    void style1_20_5ImportsPrototypeItemObjectComponentCommand() {
        String command = "/give @p {id:light_blue_banner,components:{banner_patterns:[{pattern:cross,color:white},"
                + "{pattern:minecraft:curly_border,color:black},"
                + "{pattern:triangle_top,color:yellow}],custom_name:\"Letter C\"},count:16}";

        BannerRecipe recipe = CommandConverter.fromCommand(command);

        assertNotNull(recipe);
        assertEquals("Letter C", recipe.description());
        assertEquals("light_blue", recipe.bannerColor());
        assertEquals(
                List.of(
                        BannerRecipeLayer.of("minecraft:cross", "white"),
                        BannerRecipeLayer.of("minecraft:curly_border", "black"),
                        BannerRecipeLayer.of("minecraft:triangle_top", "yellow")),
                recipe.layers());
    }

    @Test
    @DisplayName("STYLE_1_20_5 round-trips a six-layer banner")
    void style1_20_5RoundTripsSixLayerBannerCommand() {
        String command = "/give @p {id:blue_banner,components:{banner_patterns:["
                + "{pattern:small_stripes,color:black},"
                + "{pattern:straight_cross,color:black},"
                + "{pattern:flower,color:light_blue},"
                + "{pattern:circle,color:white},"
                + "{pattern:curly_border,color:black},"
                + "{pattern:skull,color:black}],custom_name:\"Six layers\"}}";

        BannerRecipe expected = new BannerRecipe(
                null,
                "Six layers",
                null,
                null,
                "misc",
                "blue",
                List.of(
                        BannerRecipeLayer.of("minecraft:small_stripes", DyeColor.BLACK.getName()),
                        BannerRecipeLayer.of("minecraft:straight_cross", DyeColor.BLACK.getName()),
                        BannerRecipeLayer.of("minecraft:flower", DyeColor.LIGHT_BLUE.getName()),
                        BannerRecipeLayer.of("minecraft:circle", DyeColor.WHITE.getName()),
                        BannerRecipeLayer.of("minecraft:curly_border", DyeColor.BLACK.getName()),
                        BannerRecipeLayer.of("minecraft:skull", DyeColor.BLACK.getName())));

        BannerRecipe parsed = CommandConverter.fromCommand(command);

        assertEquals(expected, parsed);
        assertEquals(command, CommandConverter.toCommand(parsed));
    }

    @Test
    @DisplayName("STYLE_1_13 imports a bare colored banner item")
    void style1_13ImportsBareBannerCommand() {
        BannerRecipe recipe = CommandConverter.fromCommand("blue_banner");

        assertNotNull(recipe);
        assertEquals("blue", recipe.bannerColor());
        assertEquals("Unnamed banner", recipe.description());
        assertEquals(List.of(), recipe.layers());
    }

    @Test
    @DisplayName("STYLE_1_13 imports BlockEntityTag with legacy pattern codes")
    void style1_13ImportsLegacyItemNbtCommand() {
        String command = "/give @p minecraft:blue_banner{BlockEntityTag:{Patterns:["
                + "{Pattern:\"cr\",Color:14},"
                + "{Pattern:\"drs\",Color:15},"
                + "{Pattern:\"cbo\",Color:0}]}}";

        BannerRecipe recipe = CommandConverter.fromCommand(command);

        assertNotNull(recipe);
        assertEquals(
                List.of(
                        BannerRecipeLayer.of("minecraft:cross", "red"),
                        BannerRecipeLayer.of("minecraft:stripe_downright", "black"),
                        BannerRecipeLayer.of("minecraft:curly_border", "white")),
                recipe.layers());
    }

    @Test
    @DisplayName("STYLE_1_8 imports banner damage, Base, and legacy pattern colors")
    void style1_8ImportsPreFlatteningCommand() {
        String command = "/give @p minecraft:banner 1 4 {BlockEntityTag:{Base:11,Patterns:["
                + "{Pattern:\"cr\",Color:1},{Pattern:\"drs\",Color:0}]}}";

        BannerRecipe recipe = CommandConverter.fromCommand(command);

        assertNotNull(recipe);
        assertEquals("blue", recipe.bannerColor());
        assertEquals(
                List.of(
                        BannerRecipeLayer.of("minecraft:cross", "red"),
                        BannerRecipeLayer.of("minecraft:stripe_downright", "black")),
                recipe.layers());
    }

    @Test
    @DisplayName("STYLE_1_20_5 imports the current bracket component format")
    void style1_20_5ImportsBracketComponentCommand() {
        String command = "/give @p minecraft:blue_banner[minecraft:banner_patterns=["
                + "{pattern:\"minecraft:cross\",color:\"red\"},"
                + "{pattern:\"minecraft:stripe_downright\",color:\"black\"}]]";

        BannerRecipe recipe = CommandConverter.fromCommand(command);

        assertNotNull(recipe);
        assertEquals("blue", recipe.bannerColor());
        assertEquals(
                List.of(
                        BannerRecipeLayer.of("minecraft:cross", "red"),
                        BannerRecipeLayer.of("minecraft:stripe_downright", "black")),
                recipe.layers());
    }

    @Test
    void rejectsInvalidCommands() {
        assertNull(CommandConverter.fromCommand(null));
        assertNull(CommandConverter.fromCommand(""));
        assertNull(CommandConverter.fromCommand("/give @p"));
        assertNull(CommandConverter.fromCommand("/give @p minecraft:dirt"));
        assertNull(CommandConverter.fromCommand("/give @p {id:blue_banner,components:{"));
        assertNull(CommandConverter.fromCommand(
                "/give @p {id:blue_banner,components:{banner_patterns:[{pattern:cross,color:not_a_color}]}}"));
        assertNull(CommandConverter.fromCommand(
                "/give @p {id:minecraft:blue_banner,components:{banner_patterns:[{pattern:cross}]}}"));
        assertNull(CommandConverter.fromCommand(
                "/give @p blue_banner{BlockEntityTag:{Patterns:[{Pattern:cr,Color:16}]}}"));
    }
}
