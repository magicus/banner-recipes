/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.icus.mag.bannerrecipes.bannerpack.BannerPack;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeCategory;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

class BannerPacksTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void loadsLocalDirectoryZipRecipesAndCategoryMetadata(@TempDir Path gameDir) throws IOException {
        Path packs = gameDir.resolve("bannerpacks");
        writePack(packs.resolve("letters"), """
                {"description":"Letter A","category":"letters","banner_color":"white",
                 "layers":[{"pattern":"minecraft:stripe_top","color":"red"}]}
                """);
        writeCategory(packs.resolve("letters"), "letters", "Letters", "minecraft:book");
        writePackZip(packs.resolve("numbers.zip"));

        AvailableBannerPacks availablePacks = new AvailableBannerPacks(packs);
        availablePacks.load();

        assertEquals(
                List.of("letters", "local", "numbers"),
                availablePacks.getAvailablePacks().keySet().stream().toList());
        assertNotNull(availablePacks.getAvailablePack("local"));
        assertEquals(
                "letters:a", availablePacks.getBannerRecipeById("letters:a").id());
        assertEquals("letters", availablePacks.getBannerRecipePackId("letters:a"));
        assertEquals(
                "Letters",
                availablePacks
                        .getAvailablePack("letters")
                        .getCategories()
                        .getFirst()
                        .description());
        assertEquals(
                "numbers:0", availablePacks.getBannerRecipeById("numbers:0").id());
        assertEquals(
                "Siffror",
                availablePacks
                        .getAvailablePack("numbers")
                        .getCategoryTranslations()
                        .get("sv_se")
                        .get("numbers"));
        assertTrue(availablePacks.getAvailablePack("numbers").getCategories().isEmpty());
        assertTrue(availablePacks.getAvailablePack("numbers").isReadOnly());
    }

    @Test
    void storesAndRemovesLocalRecipes(@TempDir Path gameDir) throws IOException {
        AvailableBannerPacks availablePacks = new AvailableBannerPacks(gameDir.resolve("bannerpacks"));
        availablePacks.load();

        BannerRecipe recipe = new BannerRecipe(
                null,
                "Test",
                null,
                null,
                "misc",
                "white",
                List.of(BannerRecipeLayer.of("minecraft:stripe_top", "red")));
        BannerRecipe localRecipe = availablePacks.addBannerRecipe("local", recipe);
        assertTrue(localRecipe.id().startsWith("local:"));
        assertEquals("local", availablePacks.getBannerRecipePackId(localRecipe.id()));

        BannerRecipe namedRecipe = availablePacks.addBannerRecipe("local", recipe.withId("my-design"));
        assertEquals("local:my-design", namedRecipe.id());
        availablePacks.load();
        assertNotNull(availablePacks.getBannerRecipeById(localRecipe.id()));

        availablePacks.removeBannerRecipe(namedRecipe.id());
        availablePacks.removeBannerRecipe(localRecipe.id());
        assertNull(availablePacks.getBannerRecipeById(localRecipe.id()));
    }

    @Test
    void rejectsWritingToZipPacks(@TempDir Path gameDir) throws IOException {
        Path packs = gameDir.resolve("bannerpacks");
        writePackZip(packs.resolve("numbers.zip"));
        AvailableBannerPacks availablePacks = new AvailableBannerPacks(packs);
        availablePacks.load();

        assertThrows(
                IllegalStateException.class,
                () -> availablePacks.addBannerRecipe(
                        "numbers", new BannerRecipe(null, "Nope", null, null, "misc", "white", List.of())));
        assertFalse(availablePacks.getAvailablePack("numbers").getDesigns().isEmpty());
    }

    @Test
    void exposesOnlySelectedAvailablePacks(@TempDir Path gameDir) throws IOException {
        Path packs = gameDir.resolve("bannerpacks");
        writePackZip(packs.resolve("numbers.zip"));
        Path config = gameDir.resolve("config/banner-recipes");

        SelectedBannerPacks selected = new SelectedBannerPacks(packs, config);
        selected.load();

        assertEquals(List.of("local"), selected.getSelectedPackIds());
        assertTrue(selected.getRecipes().isEmpty());
        assertEquals(
                List.of("all"),
                selected.getCategories().stream().map(BannerRecipeCategory::id).toList());

        selected.selectPack("numbers");
        assertEquals(List.of("local", "numbers"), selected.getSelectedPackIds());
        assertEquals("numbers:0", selected.getRecipes().getFirst().id());
        assertTrue(selected.getCategories().stream()
                .anyMatch(category -> category.id().equals("numbers")));

        SelectedBannerPacks restartedStorage = new SelectedBannerPacks(packs, config);
        restartedStorage.load();
        assertEquals("numbers:0", restartedStorage.getRecipes().getFirst().id());
    }

    @Test
    void removesUnavailablePacksFromSelectedConfig(@TempDir Path gameDir) throws IOException {
        Path packs = gameDir.resolve("bannerpacks");
        writePackZip(packs.resolve("numbers.zip"));
        Path config = gameDir.resolve("config/banner-recipes/bannerpacks.json");
        Files.createDirectories(config.getParent());
        Files.writeString(config, """
                {"selectedPacks":["local","numbers","missing-pack"]}
                """);

        SelectedBannerPacks selected = new SelectedBannerPacks(packs, config.getParent());
        selected.load();

        assertEquals(List.of("local", "numbers"), selected.getSelectedPackIds());
        assertEquals("""
                {
                  "selectedPacks": [
                    "local",
                    "numbers"
                  ]
                }
                """.trim(), Files.readString(config).trim());
    }

    private static void writePack(Path packDir, String recipe) throws IOException {
        Path metadata = packDir.resolve(BannerPack.MCMETA_FILE);
        Files.createDirectories(metadata.getParent());
        Files.writeString(metadata, """
                {"pack":{"pack_format":1,"description":"Test pack"}}
                """);
        Path recipeFile = packDir.resolve("banners/letters/a.json");
        Files.createDirectories(recipeFile.getParent());
        Files.writeString(recipeFile, recipe);
    }

    private static void writeCategory(Path packDir, String id, String description, String icon) throws IOException {
        Path categoryFile = packDir.resolve("categories/test/" + id + ".json");
        Files.createDirectories(categoryFile.getParent());
        Files.writeString(categoryFile, """
                {"description":"%s","icon":"%s"}
                """.formatted(description, icon));
    }

    private static void writePackZip(Path zip) throws IOException {
        Files.createDirectories(zip.getParent());
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            addZipEntry(output, "bannerpack.mcmeta", """
                    {"pack":{"pack_format":1,"description":"Numbers"}}
                    """);
            addZipEntry(output, "banners/numbers/0.json", """
                    {"description":"Number zero","category":"numbers","banner_color":"blue","layers":[]}
                    """);
            addZipEntry(output, "categories/lang/sv_se.json", """
                    {"numbers":"Siffror"}
                    """);
        }
    }

    private static void addZipEntry(ZipOutputStream output, String name, String contents) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        output.write(contents.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        output.closeEntry();
    }
}
