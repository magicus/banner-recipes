/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.icus.mag.bannerrecipes.converters.command.BannerItemConverter;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

class BannerParsingTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                .build(VanillaRegistries.createWorldLookup())
                .forEach(pending -> pending.apply());
    }

    private record Layer(String pattern, DyeColor color) {}

    private record Case(
            String name, String command, String bannerColor, String description, String author, List<Layer> layers) {}

    private static Layer layer(String pattern, DyeColor color) {
        return new Layer("minecraft:" + pattern, color);
    }

    private static BannerRecipe parse(String command) throws Exception {
        return BannerItemConverter.fromBannerItem(command.strip(), VanillaRegistries.createWorldLookup());
    }

    @TestFactory
    Stream<DynamicTest> validBanners() {
        List<Case> cases = List.of(
                new Case(
                        "Legacy: patterns and metadata",
                        """
						banner 1 5 {BlockEntityTag:{Patterns:[
						  {Pattern:"cr",Color:0},
						  {Pattern:"bs",Color:14}
						]},display:{
						  Name:"Legacy Banner",
						  Lore:["Legacy author"]
						}}
						""",
                        "purple",
                        "Legacy Banner",
                        "Legacy author",
                        List.of(layer("cross", DyeColor.WHITE), layer("stripe_bottom", DyeColor.RED))),
                new Case(
                        "NBT: patterns and JSON metadata",
                        """
						blue_banner{BlockEntityTag:{Patterns:[
						  {Pattern:"cr",Color:0},
						  {Pattern:"ts",Color:15}
						]},display:{
						  Name:'{"text":"NBT Banner"}',
						  Lore:['{"text":"NBT author"}']
						}} 1
						""",
                        "blue",
                        "NBT Banner",
                        "NBT author",
                        List.of(layer("cross", DyeColor.WHITE), layer("stripe_top", DyeColor.BLACK))),
                new Case(
                        "Component: JSON text components",
                        """
						blue_banner[
						  banner_patterns=[
							{pattern:"minecraft:cross",color:"white"},
							{pattern:"minecraft:stripe_bottom",color:"red"}
						  ],
						  custom_name='{"text":"Component Banner"}',
						  lore=['{"text":"Component author"}']
						] 1
						""",
                        "blue",
                        "Component Banner",
                        "Component author",
                        List.of(layer("cross", DyeColor.WHITE), layer("stripe_bottom", DyeColor.RED))),
                new Case(
                        "Modern: SNBT text components",
                        """
						blue_banner[
						  banner_patterns=[
							{pattern:"minecraft:cross",color:"white"},
							{pattern:"minecraft:stripe_bottom",color:"red"}
						  ],
						  custom_name={text:"Modern Banner"},
						  lore=[{text:"Modern author"}]
						] 1
						""",
                        "blue",
                        "Modern Banner",
                        "Modern author",
                        List.of(layer("cross", DyeColor.WHITE), layer("stripe_bottom", DyeColor.RED))),
                new Case(
                        "Legacy: white base, black pattern",
                        "banner 1 15 {BlockEntityTag:{Patterns:[{Pattern:\"cr\",Color:15}]}}",
                        "white",
                        null,
                        null,
                        List.of(layer("cross", DyeColor.BLACK))),
                new Case(
                        "Legacy: black base, white pattern",
                        "banner 1 0 {BlockEntityTag:{Patterns:[{Pattern:\"cr\",Color:0}]}}",
                        "black",
                        null,
                        null,
                        List.of(layer("cross", DyeColor.WHITE))),
                new Case(
                        "NBT: namespace and no metadata",
                        "minecraft:red_banner{BlockEntityTag:{Patterns:[{Pattern:\"bs\",Color:4}]}}",
                        "red",
                        null,
                        null,
                        List.of(layer("stripe_bottom", DyeColor.YELLOW))),
                new Case(
                        "Modern: no metadata",
                        "green_banner[banner_patterns=[{pattern:\"minecraft:cross\",color:\"white\"}]]",
                        "green",
                        null,
                        null,
                        List.of(layer("cross", DyeColor.WHITE))),
                new Case(
                        "Modern: name without lore",
                        "white_banner[banner_patterns=[{pattern:\"cross\",color:\"red\"}],custom_name={text:\"Only a name\"}]",
                        "white",
                        "Only a name",
                        null,
                        List.of(layer("cross", DyeColor.RED))),
                new Case(
                        "Modern: lore without name",
                        "black_banner[banner_patterns=[{pattern:\"cross\",color:\"white\"}],lore=[{text:\"Only an author\"}]]",
                        "black",
                        null,
                        "Only an author",
                        List.of(layer("cross", DyeColor.WHITE))),
                new Case(
                        "NBT: JSON text with extra",
                        """
						white_banner{BlockEntityTag:{
						  Patterns:[{Pattern:"cr",Color:0}]
						},display:{
						  Name:'{"text":"Hello ","extra":[{"text":"world"}]}',
						  Lore:['{"text":"Jane Doe"}']
						}}
						""",
                        "white",
                        "Hello world",
                        "Jane Doe",
                        List.of(layer("cross", DyeColor.WHITE))),
                new Case(
                        "Modern: multiple colors and ordered layers",
                        """
						black_banner[banner_patterns=[
						  {pattern:"cross",color:"white"},
						  {pattern:"stripe_top",color:"lime"},
						  {pattern:"border",color:"magenta"},
						  {pattern:"circle",color:"light_blue"}
						]]
						""",
                        "black",
                        null,
                        null,
                        List.of(
                                layer("cross", DyeColor.WHITE),
                                layer("stripe_top", DyeColor.LIME),
                                layer("border", DyeColor.MAGENTA),
                                layer("circle", DyeColor.LIGHT_BLUE))));

        return cases.stream()
                .map(test -> DynamicTest.dynamicTest(test.name(), () -> {
                    BannerRecipe actual = parse(test.command());

                    assertNotNull(actual);

                    assertAll(
                            () -> assertNull(actual.id(), "id"),
                            () -> assertNull(actual.url(), "url"),
                            () -> assertNull(actual.category(), "category"),
                            () -> assertEquals(test.bannerColor(), actual.bannerColor(), "bannerColor"),
                            () -> assertEquals(test.description(), actual.description(), "description"),
                            () -> assertEquals(test.author(), actual.author(), "author"),
                            () -> assertEquals(
                                    test.layers().stream().map(Layer::pattern).toList(),
                                    actual.layers().stream()
                                            .map(l -> l.pattern().toString())
                                            .toList(),
                                    "patterns"),
                            () -> assertEquals(
                                    test.layers().stream().map(Layer::color).toList(),
                                    actual.layers().stream()
                                            .map(BannerRecipeLayer::color)
                                            .toList(),
                                    "pattern colors"));
                }));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "banner",
                "banner 1 0",
                "banner 1 15 {display:{Name:\"Empty\"}}",
                "white_banner",
                "white_banner{display:{Name:'{\"text\":\"Empty\"}'}}",
                "white_banner{BlockEntityTag:{Patterns:[]}}",
                "white_banner[banner_patterns=[]]",
                "white_banner[custom_name={text:\"Empty\"}]",
                "white_banner[custom_name='{\"text\":\"Empty\"}']"
            })
    void rejectsBannersWithoutPatterns(String input) {
        assertThrows(Exception.class, () -> parse(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"minecraft:diamond_sword", "minecraft:stone", "not_an_item", "minecraft:red_dye"})
    void rejectsNonBannerItems(String input) {
        assertThrows(Exception.class, () -> parse(input));
    }

    @Test
    void rejectsUnknownLegacyPattern() {
        assertThrows(
                Exception.class,
                () -> parse("banner 1 0 {BlockEntityTag:{Patterns:" + "[{Pattern:\"invalid\",Color:0}]}}"));
    }

    @Test
    void rejectsUnknownModernPattern() {
        assertThrows(
                Exception.class,
                () -> parse("white_banner[banner_patterns="
                        + "[{pattern:\"minecraft:not_a_pattern\","
                        + "color:\"white\"}]]"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 16, 99})
    void rejectsInvalidLegacyPatternColor(int color) {
        assertThrows(
                Exception.class,
                () -> parse("banner 1 0 {BlockEntityTag:{Patterns:" + "[{Pattern:\"cr\",Color:" + color + "}]}}"));
    }

    @Test
    void rejectsInvalidModernPatternColor() {
        assertThrows(
                Exception.class,
                () -> parse("white_banner[banner_patterns=" + "[{pattern:\"minecraft:cross\",color:\"invalid\"}]]"));
    }
}
