/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import se.icus.mag.bannerrecipes.converters.command.CommandConverter;
import se.icus.mag.bannerrecipes.converters.command.CommandConverter.CommandParseResult;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

/**
 * Uses the real Minecraft TagParser. No mocks or Minecraft client are required.
 * Dynamic tests need junit-jupiter-api, not junit-jupiter-params.
 *
 * Three historical item formats are tested separately. The serialized item
 * object is a compatibility input, not a historical /give format.
 * Round trips compare recipes: export deliberately normalizes syntax and drops
 * counts, item damage overridden by Base, and unrelated item data.
 */
class CommandConverterTest {
    private static final String[] COLORS = {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    // Independent expected values, not obtained from the converter's table.
    private static final String[][] LEGACY_PATTERNS = {
        {"b", "base"},
        {"bl", "square_bottom_left"},
        {"br", "square_bottom_right"},
        {"tl", "square_top_left"},
        {"tr", "square_top_right"},
        {"bs", "stripe_bottom"},
        {"ts", "stripe_top"},
        {"ls", "stripe_left"},
        {"rs", "stripe_right"},
        {"cs", "stripe_center"},
        {"ms", "stripe_middle"},
        {"drs", "stripe_downright"},
        {"dls", "stripe_downleft"},
        {"ss", "small_stripes"},
        {"cr", "cross"},
        {"sc", "straight_cross"},
        {"bt", "triangle_bottom"},
        {"tt", "triangle_top"},
        {"bts", "triangles_bottom"},
        {"tts", "triangles_top"},
        {"ld", "diagonal_left"},
        {"rd", "diagonal_up_right"},
        {"lud", "diagonal_up_left"},
        {"rud", "diagonal_right"},
        {"mc", "circle"},
        {"mr", "rhombus"},
        {"vh", "half_vertical"},
        {"hh", "half_horizontal"},
        {"vhr", "half_vertical_right"},
        {"hhb", "half_horizontal_bottom"},
        {"bo", "border"},
        {"cbo", "curly_border"},
        {"gra", "gradient"},
        {"gru", "gradient_up"},
        {"bri", "bricks"},
        {"glb", "globe"},
        {"cre", "creeper"},
        {"sku", "skull"},
        {"flo", "flower"},
        {"moj", "mojang"},
        {"pig", "piglin"}
    };

    @Test
    @DisplayName("1.8: Base overrides damage, and all legacy dye IDs are reversed")
    void baseOverridesDamage() {
        String input = "/give @p minecraft:banner 1 4 {BlockEntityTag:{Base:11,Patterns:["
                + "{Pattern:\"cr\",Color:1},{Pattern:\"drs\",Color:0}]}}";
        assertRecipe(
                input, recipe("yellow", "Unnamed banner", layer("cross", "red"), layer("stripe_downright", "black")));
        assertEquals(
                CommandConverter.fromCommand(input), CommandConverter.fromCommand(input.replace(" 1 4 ", " 1 5 ")));
    }

    @Test
    @DisplayName("1.8: Patterns without Base use the item damage for the background")
    void legacyPatternsWithoutBase() {
        assertRecipe(
                "/give @p minecraft:banner 1 4 {BlockEntityTag:{Patterns:[{Pattern:cr,Color:1}]}}",
                recipe("blue", "Unnamed banner", layer("cross", "red")));
    }

    @TestFactory
    @DisplayName("All sixteen colors: legacy damage, Base, legacy layers, colored items and modern layers")
    Stream<DynamicTest> allColorMappings() {
        List<DynamicTest> tests = new ArrayList<>();
        for (int i = 0; i < COLORS.length; i++) {
            String color = COLORS[i];
            int modernId = i;
            int oldId = 15 - i;
            tests.add(valid("1.8 damage " + oldId, "banner 1 " + oldId, recipe(color)));
            tests.add(valid("1.8 Base " + oldId, "banner 1 0 {BlockEntityTag:{Base:" + oldId + "}}", recipe(color)));
            tests.add(valid(
                    "1.8 layer " + oldId,
                    "banner 1 4 {BlockEntityTag:{Patterns:[{Pattern:cr,Color:" + oldId + "}]}}",
                    recipe("blue", "Unnamed banner", layer("cross", color))));
            tests.add(valid("1.13 item " + color, "minecraft:" + color + "_banner", recipe(color)));
            tests.add(valid(
                    "1.13 layer " + modernId,
                    "blue_banner{BlockEntityTag:{Patterns:[{Pattern:cr,Color:" + modernId + "}]}}",
                    recipe("blue", "Unnamed banner", layer("cross", color))));
            tests.add(valid(
                    "Modern layer " + color,
                    "blue_banner[banner_patterns=[{pattern:cross,color:" + color + "}]]",
                    recipe("blue", "Unnamed banner", layer("cross", color))));
        }
        return tests.stream();
    }

    @TestFactory
    @DisplayName("Every legacy pattern code in both legacy formats")
    Stream<DynamicTest> allLegacyPatterns() {
        List<DynamicTest> tests = new ArrayList<>();
        for (String[] mapping : LEGACY_PATTERNS) {
            BannerRecipe expected = recipe("blue", "Unnamed banner", layer(mapping[1], "red"));
            tests.add(valid(
                    "1.8 " + mapping[0],
                    "banner 1 4 {BlockEntityTag:{Patterns:[{Pattern:" + mapping[0] + ",Color:1}]}}",
                    expected));
            tests.add(valid(
                    "1.13 " + mapping[0],
                    "blue_banner{BlockEntityTag:{Patterns:[{Pattern:" + mapping[0] + ",Color:14}]}}",
                    expected));
        }
        return tests.stream();
    }

    @TestFactory
    @DisplayName("Missing and empty patterns produce plain banners")
    Stream<DynamicTest> plainBanners() {
        return Stream.of(
                        "blue_banner",
                        "minecraft:blue_banner 16",
                        "blue_banner{}",
                        "blue_banner{BlockEntityTag:{}}",
                        "blue_banner{BlockEntityTag:{Patterns:[]}} 2",
                        "blue_banner[]",
                        "blue_banner[banner_patterns=[]]",
                        "blue_banner[!banner_patterns]",
                        "{id:blue_banner}",
                        "{id:blue_banner,components:{}}",
                        "banner 1 4",
                        "banner 1 4 {}",
                        "banner 1 4 {BlockEntityTag:{}}",
                        "banner 1 4 {BlockEntityTag:{Patterns:[]}}")
                .map(input -> valid(input, input, recipe("blue")));
    }

    @TestFactory
    @DisplayName("Quoting and integer SNBT types in legacy layers")
    Stream<DynamicTest> legacyQuotingAndIntegerTypes() {
        List<DynamicTest> tests = new ArrayList<>();
        for (String code : List.of("cr", "'cr'", "\"cr\"")) {
            for (String suffix : List.of("", "b", "s")) {
                tests.add(valid(
                        "1.8 " + code + " / " + suffix,
                        "banner 1 4 {BlockEntityTag:{Patterns:[{Pattern:" + code + ",Color:1" + suffix + "}]}}",
                        recipe("blue", "Unnamed banner", layer("cross", "red"))));
                tests.add(valid(
                        "1.13 " + code + " / " + suffix,
                        "blue_banner{BlockEntityTag:{Patterns:[{Pattern:" + code + ",Color:14" + suffix + "}]}}",
                        recipe("blue", "Unnamed banner", layer("cross", "red"))));
            }
        }
        return tests.stream();
    }

    @TestFactory
    @DisplayName("Component namespaces, pattern namespaces, quotes and field order")
    Stream<DynamicTest> modernSpellings() {
        List<DynamicTest> tests = new ArrayList<>();
        for (String key : List.of("banner_patterns", "minecraft:banner_patterns")) {
            for (String pattern :
                    List.of("cross", "'cross'", "\"cross\"", "'minecraft:cross'", "\"minecraft:cross\"")) {
                for (String color : List.of("red", "'red'", "\"red\"")) {
                    tests.add(valid(
                            key + " / " + pattern + " / " + color,
                            "blue_banner[" + key + "=[{color:" + color + ",pattern:" + pattern + "}]]",
                            recipe("blue", "Unnamed banner", layer("cross", "red"))));
                }
            }
        }
        return tests.stream();
    }

    @Test
    void preservesLayerOrderAndRepetitions() {
        assertRecipe(
                "blue_banner[banner_patterns=[{pattern:cross,color:red},{pattern:cross,color:black},"
                        + "{pattern:flower,color:white},{pattern:cross,color:red}]]",
                recipe(
                        "blue",
                        "Unnamed banner",
                        layer("cross", "red"),
                        layer("cross", "black"),
                        layer("flower", "white"),
                        layer("cross", "red")));
    }

    @Test
    void preservesDatapackNamespace() {
        assertRecipe(
                "blue_banner[banner_patterns=[{pattern:'example:custom/pattern',color:red}]]",
                recipe("blue", "Unnamed banner", layer("example:custom/pattern", "red")));
    }

    @TestFactory
    @DisplayName("Give prefix is optional; command targets never enter item parsing")
    Stream<DynamicTest> commandEnvelopes() {
        String item = "minecraft:blue_banner[banner_patterns=[{pattern:cross,color:red}]] 3";
        BannerRecipe expected = recipe("blue", "Unnamed banner", layer("cross", "red"));
        return Stream.of(
                        "",
                        "/give @p ",
                        "give @p ",
                        "/minecraft:give @s ",
                        "minecraft:give Steve ",
                        "/give @a ",
                        "/give @e[type=player,limit=1] ",
                        "/give @p[name='A B',nbt={Tags:['x,y']}] ",
                        "/give 12345678-1234-1234-1234-123456789abc ",
                        " \t/give\t@p\t")
                .map(prefix -> valid(
                        prefix.isBlank() ? "Without command prefix" : prefix.strip(), prefix + item + " \n", expected));
    }

    @Test
    void itemDescriptionApiDoesNotStripCommands() {
        String item = "blue_banner[banner_patterns=[{pattern:cross,color:red}]]";
        assertEquals(CommandConverter.fromCommand(item), CommandConverter.fromItemDescription(item));
        assertNull(CommandConverter.fromItemDescription("/give @p " + item));
        assertNull(CommandConverter.fromItemDescription("give @p " + item));
    }

    @Test
    void acceptsWhitespaceInsideSnbtAndBetweenComponentAssignments() {
        assertRecipe(
                "\n/give @p blue_banner[ banner_patterns = [ { color : red , pattern : 'minecraft:cross' } ],"
                        + " custom_name = {text: 'A name'} ] 16\t",
                recipe("blue", "A name", layer("cross", "red")));
    }

    @Test
    void validatesButIgnoresUnrelatedComponents() {
        assertRecipe(
                "blue_banner[minecraft:custom_data={nested:[{text:'a,b]}',other:[I;1,2,3]}]},"
                        + "banner_patterns=[{pattern:cross,color:red}],!minecraft:damage]",
                recipe("blue", "Unnamed banner", layer("cross", "red")));
        assertRecipe(
                "blue_banner{display:{Name:'ignored'},BlockEntityTag:{Patterns:[{Pattern:cr,Color:14}]}}",
                recipe("blue", "Unnamed banner", layer("cross", "red")));
    }

    @Test
    @DisplayName("Compatibility object: namespace-bearing SNBT strings must be quoted")
    void importsSerializedItemCompatibilityForm() {
        assertRecipe(
                "/give @p {id:light_blue_banner,components:{banner_patterns:[{pattern:cross,color:white},"
                        + "{pattern:'minecraft:curly_border',color:black},{pattern:triangle_top,color:yellow}],"
                        + "custom_name:'Letter C'},count:16}",
                recipe(
                        "light_blue",
                        "Letter C",
                        layer("cross", "white"),
                        layer("curly_border", "black"),
                        layer("triangle_top", "yellow")));
        assertRecipe(
                "{id:'minecraft:blue_banner',components:{'minecraft:banner_patterns':"
                        + "[{pattern:'minecraft:cross',color:red}],'minecraft:custom_name':{text:'Letter C'}}}",
                recipe("blue", "Letter C", layer("cross", "red")));
    }

    @Test
    @DisplayName("Round trip compares recipes; export uses current bracket components")
    void sixLayersRoundTrip() {
        BannerRecipe expected = recipe(
                "blue",
                "Six layers",
                layer("small_stripes", "black"),
                layer("straight_cross", "black"),
                layer("flower", "light_blue"),
                layer("circle", "white"),
                layer("curly_border", "black"),
                layer("skull", "black"));
        String compatibilityInput = "{id:blue_banner,components:{banner_patterns:["
                + "{pattern:small_stripes,color:black},{pattern:straight_cross,color:black},"
                + "{pattern:flower,color:light_blue},{pattern:circle,color:white},"
                + "{pattern:curly_border,color:black},{pattern:skull,color:black}],custom_name:'Six layers'}}";
        assertRecipe(compatibilityInput, expected);
        assertRoundTrip(expected);
    }

    @TestFactory
    @DisplayName("Export/import preserves plain text containing syntax characters, escapes and Unicode")
    Stream<DynamicTest> namesRoundTrip() {
        return Stream.of(
                        "Letter C",
                        "a,b]}:{[",
                        "He said \"hello\"",
                        "It's mine",
                        "C:\\banners\\blue",
                        "line 1\nline 2\tend",
                        "Hjärtero — blå 💙",
                        "<tag> & =")
                .map(name -> DynamicTest.dynamicTest(
                        name, () -> assertRoundTrip(recipe("blue", name, layer("flower", "red")))));
    }

    @TestFactory
    Stream<DynamicTest> allColorsRoundTrip() {
        return Stream.of(COLORS).map(color -> DynamicTest.dynamicTest(color, () -> assertRoundTrip(recipe(color))));
    }

    @Test
    void exporterIncludesAllPatternNamespaces() {
        BannerRecipe expected = recipe("blue", "Unnamed banner", layer("example:custom/pattern", "red"));
        assertEquals(
                "minecraft:blue_banner[minecraft:banner_patterns=[{pattern:\"example:custom/pattern\",color:\"red\"}]]",
                CommandConverter.toItemDescription(expected));
        assertRoundTrip(expected);
    }

    @Test
    void exportsPlainBannerWithoutUnnecessaryComponents() {
        assertEquals("/give @p minecraft:blue_banner", CommandConverter.toCommand(recipe("blue")));
        assertThrows(IllegalArgumentException.class, () -> CommandConverter.toCommand(null));
        assertThrows(IllegalArgumentException.class, () -> CommandConverter.toCommand(recipe("not_a_color")));
    }

    @TestFactory
    @DisplayName("Malformed commands are rejected completely, without a fallback to a plain banner")
    Stream<DynamicTest> invalidSyntax() {
        return Stream.of(
                        "",
                        " \t\n",
                        "/give",
                        "/give @p",
                        "/blue_banner",
                        "/give @z blue_banner",
                        "/give @px blue_banner",
                        "give blue_banner",
                        "/give @p[ blue_banner",
                        "dirt",
                        "minecraft:dirt",
                        "other:blue_banner",
                        "not_a_color_banner",
                        "blue_banner junk",
                        "blue_banner 0",
                        "blue_banner -1",
                        "blue_banner 1.5",
                        "blue_banner 2147483648",
                        "blue_banner 1 junk",
                        "blue_banner{}junk",
                        "banner 0 4",
                        "banner 1 -1",
                        "banner 1 16",
                        "banner 1 4{}",
                        "banner 1 4 {} 1",
                        "blue_banner [banner_patterns=[]]",
                        "blue_banner{}[banner_patterns=[]]",
                        "blue_banner[banner_patterns=[]]junk",
                        "blue_banner[banner_patterns=[]] 1 junk",
                        "blue_banner[banner_patterns=]",
                        "blue_banner[banner_patterns=[]",
                        "blue_banner[banner_patterns=[}]]",
                        "blue_banner[banner_patterns=[],]",
                        "blue_banner[,banner_patterns=[]]",
                        "blue_banner[banner_patterns=[],,other=1]",
                        "blue_banner[banner_patterns:[]]",
                        "blue_banner[banner_patterns=[] other=1]",
                        "blue_banner[banner_patterns=[],minecraft:banner_patterns=[]]",
                        "blue_banner[!banner_patterns,banner_patterns=[]]",
                        "blue_banner[!banner_patterns=[]]",
                        "blue_banner[custom_name='unfinished]",
                        "blue_banner[other={text:'unfinished}]",
                        "blue_banner[other={broken:}]",
                        "blue_banner[other=[I;not_a_number]]",
                        "blue_banner{BlockEntityTag:{Patterns:[{Pattern:cr,Color:14}]}",
                        "{id:blue_banner,components:{",
                        "{id:minecraft:blue_banner}",
                        "{id:blue_banner} trailing",
                        "{id:blue_banner,count:0}",
                        "{id:blue_banner,components:{banner_patterns:[],'minecraft:banner_patterns':[]}}")
                .map(input -> DynamicTest.dynamicTest(input.isBlank() ? "blank" : input, () -> assertRejected(input)));
    }

    @TestFactory
    @DisplayName("Missing fields, wrong SNBT types, invalid IDs and invalid dye values")
    Stream<DynamicTest> invalidBannerData() {
        List<String> inputs = new ArrayList<>(List.of(
                "{id:42}",
                "{components:{}}",
                "{id:blue_banner,components:[]}",
                "blue_banner{BlockEntityTag:[]}",
                "blue_banner{BlockEntityTag:{Patterns:42}}",
                "blue_banner{BlockEntityTag:{Patterns:[1]}}",
                "blue_banner{BlockEntityTag:{Patterns:[{Pattern:unknown,Color:14}]}}",
                "blue_banner{BlockEntityTag:{Patterns:[{Pattern:cross,Color:14}]}}"));
        for (String badColor : List.of("-1", "16", "256", "14.5f", "14.0d", "'14'")) {
            inputs.add("blue_banner{BlockEntityTag:{Patterns:[{Pattern:cr,Color:" + badColor + "}]}}");
            inputs.add("banner 1 4 {BlockEntityTag:{Patterns:[{Pattern:cr,Color:" + badColor + "}]}}");
            inputs.add("banner 1 4 {BlockEntityTag:{Base:" + badColor + "}}");
        }
        for (String layer : List.of("{}", "{Pattern:cr}", "{Color:14}", "{Pattern:42,Color:14}")) {
            inputs.add("blue_banner{BlockEntityTag:{Patterns:[" + layer + "]}}");
        }
        for (String patterns : List.of(
                "42",
                "{}",
                "[1]",
                "[{}]",
                "[{pattern:cross}]",
                "[{color:red}]",
                "[{pattern:42,color:red}]",
                "[{pattern:cross,color:14}]",
                "[{pattern:cross,color:not_a_color}]",
                "[{pattern:'',color:red}]",
                "[{pattern:'Minecraft:cross',color:red}]",
                "[{pattern:'minecraft:bad pattern',color:red}]",
                "[{pattern:'minecraft:cross:extra',color:red}]")) {
            inputs.add("blue_banner[banner_patterns=" + patterns + "]");
            inputs.add("{id:blue_banner,components:{banner_patterns:" + patterns + "}}");
        }
        return inputs.stream().map(input -> DynamicTest.dynamicTest(input, () -> assertRejected(input)));
    }

    @TestFactory
    @DisplayName("Each historical format imports with or without a command envelope")
    Stream<DynamicTest> envelopesForEveryFormat() {
        List<DynamicTest> tests = new ArrayList<>();
        for (String item : List.of(
                "banner 1 4 {BlockEntityTag:{Patterns:[{Pattern:cr,Color:1}]}}",
                "blue_banner{BlockEntityTag:{Patterns:[{Pattern:cr,Color:14}]}} 1",
                "blue_banner[banner_patterns=[{pattern:cross,color:red}]] 1")) {
            for (String prefix : List.of("", "/give @p ", "give Steve ", "  /give @a ")) {
                tests.add(valid(prefix + item, prefix + item, recipe("blue", "Unnamed banner", layer("cross", "red"))));
            }
        }
        return tests.stream();
    }

    @Test
    void newerNbtFormatGetsBackgroundFromItemId() {
        assertRecipe(
                "blue_banner{BlockEntityTag:{Base:11,Patterns:[{Pattern:cr,Color:14}]}}",
                recipe("blue", "Unnamed banner", layer("cross", "red")));
    }

    @Test
    void oldestDefaultsToBlackWhenDamageIsOmitted() {
        assertRecipe("banner", recipe("black"));
        assertRecipe("banner 1", recipe("black"));
        assertRecipe("banner {BlockEntityTag:{Base:4}}", recipe("blue"));
    }

    @TestFactory
    @DisplayName("Importing an older format then exporting preserves its visible recipe")
    Stream<DynamicTest> legacyToModernRoundTrips() {
        return Stream.of(
                        "banner 1 4 {BlockEntityTag:{Patterns:[{Pattern:cr,Color:1},{Pattern:drs,Color:0}]}}",
                        "banner 1 4 {BlockEntityTag:{Base:11,Patterns:[{Pattern:cr,Color:1}]}}",
                        "blue_banner{BlockEntityTag:{Patterns:[{Pattern:cr,Color:14},{Pattern:drs,Color:15}]}}")
                .map(input -> DynamicTest.dynamicTest(input, () -> {
                    BannerRecipe parsed = CommandConverter.fromCommand(input);
                    assertNotNull(parsed);
                    assertRoundTrip(parsed);
                }));
    }

    @TestFactory
    @DisplayName("An invalid later layer rejects the whole banner, not just that layer")
    Stream<DynamicTest> invalidLaterLayers() {
        return Stream.of(
                        "banner 1 4 {BlockEntityTag:{Patterns:[{Pattern:cr,Color:1},{Pattern:flo}]}}",
                        "blue_banner{BlockEntityTag:{Patterns:[{Pattern:cr,Color:14},{Pattern:unknown,Color:0}]}}",
                        "blue_banner[banner_patterns=[{pattern:cross,color:red},{pattern:flower}]]",
                        "{id:blue_banner,components:{banner_patterns:[{pattern:cross,color:red},{pattern:flower}]}}")
                .map(input -> DynamicTest.dynamicTest(input, () -> assertRejected(input)));
    }

    @Test
    void nullInputIsRejectedByBothEntryPoints() {
        assertNull(CommandConverter.fromCommand(null));
        assertNull(CommandConverter.fromItemDescription(null));
        assertNotNull(CommandConverter.parseCommandDetailed(null).errorMessage());
    }

    @Test
    void errorPositionsReferToOriginalInput() {
        String item = "blue_banner[banner_patterns=[{pattern:cross,color:red}]] trailing";
        String prefix = "  /give @p ";
        CommandParseResult bare = CommandConverter.parseItemDescriptionDetailed(item);
        CommandParseResult command = CommandConverter.parseCommandDetailed(prefix + item);
        assertNotNull(bare.errorPosition());
        assertEquals(item.indexOf("trailing"), bare.errorPosition());
        assertEquals(prefix.length() + bare.errorPosition(), command.errorPosition());
    }

    @Test
    void boundedInputFailsGracefully() {
        assertRejected("blue_banner[other=" + "[".repeat(129) + "]".repeat(129) + "]");
        assertRejected(" ".repeat(1_048_577));
    }

    private static DynamicTest valid(String name, String input, BannerRecipe expected) {
        return DynamicTest.dynamicTest(name, () -> assertRecipe(input, expected));
    }

    private static void assertRecipe(String input, BannerRecipe expected) {
        CommandParseResult result = CommandConverter.parseCommandDetailed(input);
        assertNotNull(result.recipe(), () -> input + " -> " + result.errorMessage() + " at " + result.errorPosition());
        assertNull(result.errorMessage(), input);
        assertNull(result.errorPosition(), input);
        assertEquals(expected, result.recipe(), input);
        assertEquals(expected, CommandConverter.fromCommand(input), input);
    }

    private static void assertRejected(String input) {
        CommandParseResult result = CommandConverter.parseCommandDetailed(input);
        assertNull(result.recipe(), () -> "Unexpectedly accepted: " + input);
        assertNotNull(result.errorMessage());
        assertFalse(result.errorMessage().isBlank());
        assertNotNull(result.errorPosition());
        assertTrue(result.errorPosition() >= 0 && result.errorPosition() <= input.length());
        assertNull(CommandConverter.fromCommand(input));
    }

    private static void assertRoundTrip(BannerRecipe expected) {
        String command = CommandConverter.toCommand(expected);
        assertTrue(command.startsWith("/give @p minecraft:"));
        assertFalse(command.startsWith("/give @p {"));
        assertRecipe(command, expected);
        String item = CommandConverter.toItemDescription(expected);
        assertEquals("/give @p " + item, command);
        assertEquals(expected, CommandConverter.fromItemDescription(item));
    }

    private static BannerRecipe recipe(String color) {
        return recipe(color, "Unnamed banner");
    }

    private static BannerRecipe recipe(String color, String description, BannerRecipeLayer... layers) {
        return new BannerRecipe(null, description, null, null, "misc", color, List.of(layers));
    }

    private static BannerRecipeLayer layer(String pattern, String color) {
        return BannerRecipeLayer.of(pattern.contains(":") ? pattern : "minecraft:" + pattern, color);
    }
}
