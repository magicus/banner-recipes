/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceException;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

class RecipeConverterTest {
    private static final String COMPONENT_ITEM =
            "blue_banner[banner_patterns=[{pattern:\"minecraft:cross\",color:\"white\"}]]";

    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createWorldLookup();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(pending -> pending.apply());
    }

    private static BannerRecipe parse(String input) throws Exception {
        return RecipeConverter.fromString(input, registries);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                COMPONENT_ITEM,
                "  " + COMPONENT_ITEM + "  ",
                "give @p " + COMPONENT_ITEM,
                "/give @p " + COMPONENT_ITEM,
                "/give Steve " + COMPONENT_ITEM + " 1",
                "/give @a[name=\"a b\",limit=1] " + COMPONENT_ITEM,
            })
    void itemsAndCommandsGiveSameRecipe(String input) throws Exception {
        BannerRecipe recipe = parse(input);

        assertEquals("blue", recipe.bannerColor());
        assertEquals(1, recipe.layers().size());
        assertEquals("minecraft:cross", recipe.layers().getFirst().pattern().toString());
    }

    @Test
    void legacyGiveCommand() throws Exception {
        BannerRecipe recipe = parse("/give @p banner 1 5 {BlockEntityTag:{Patterns:[{Pattern:\"cr\",Color:0}]}}");

        assertEquals("purple", recipe.bannerColor());
        assertEquals(1, recipe.layers().size());
    }

    @Test
    void nbtGiveCommand() throws Exception {
        BannerRecipe recipe = parse("give @p blue_banner{BlockEntityTag:{Patterns:[{Pattern:\"cr\",Color:0}]}} 1");

        assertEquals("blue", recipe.bannerColor());
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://example.invalid/foo", "https://example.invalid/foo", "HTTPS://example.invalid/foo"})
    void urlsAreDispatchedToUrlConverter(String url) {
        assertThrows(OnlineServiceException.UnsupportedUrlException.class, () -> parse(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "give", "/give @p", "/give @p stone", "stone"})
    void invalidInputIsRejected(String input) {
        assertThrows(IllegalArgumentException.class, () -> parse(input));
    }

    @Test
    void giveWithoutWordBoundaryIsNotACommand() {
        assertThrows(CommandSyntaxException.class, () -> parse("giveaway_banner"));
    }

    @Test
    void nullInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parse(null));
    }

    @Test
    void otherCommandsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> parse("/say hello"));
    }

    @Test
    void syntaxErrorPositionRefersToOriginalInput() {
        String broken = "blue_banner[banner_patterns=[{pattern:\"minecraft:cross\",color:\"white\"}";

        int plain =
                assertThrows(CommandSyntaxException.class, () -> parse(broken)).getCursor();
        int padded = assertThrows(CommandSyntaxException.class, () -> parse("  " + broken))
                .getCursor();
        int command = assertThrows(CommandSyntaxException.class, () -> parse("/give @p " + broken))
                .getCursor();

        assertEquals(plain + 2, padded);
        assertEquals(plain + "/give @p ".length(), command);
    }
}
