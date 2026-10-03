/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.bannerpack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeCategory;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

public abstract class BannerPack {
    public static final String MCMETA_FILE = "bannerpack.mcmeta";
    public static final String BANNERS_DIR = "banners";
    private static final String CATEGORIES_DIR = "categories";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final BannerPackMetadata metadata;
    private final Path path;
    private final Map<String, BannerRecipe> recipes = new LinkedHashMap<>();
    private List<BannerRecipeCategory> categories = List.of();
    private Map<String, Map<String, String>> categoryTranslations = Map.of();

    protected BannerPack(BannerPackMetadata metadata, Path path) {
        this.metadata = metadata;
        this.path = path;
    }

    public BannerPackMetadata getMetadata() {
        return metadata;
    }

    public Path getPath() {
        return path;
    }

    public BannerRecipe getDesign(String recipeId) {
        return recipes.get(recipeId);
    }

    public Collection<BannerRecipe> getDesigns() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public List<BannerRecipeCategory> getCategories() {
        return categories;
    }

    public Map<String, Map<String, String>> getCategoryTranslations() {
        return categoryTranslations;
    }

    public abstract boolean isReadOnly();

    public abstract BannerRecipe addBannerRecipe(BannerRecipe recipe) throws IOException;

    public abstract void updateBannerRecipe(BannerRecipe recipe) throws IOException;

    public abstract void removeBannerRecipe(String recipeId) throws IOException;

    public BannerRecipe copyRecipeTo(BannerPack target, String recipeId) throws IOException {
        if (target.isReadOnly()) throw new IllegalArgumentException("cannot copy to read-only pack");
        BannerRecipe recipe = getDesign(recipeId);
        if (recipe == null) throw new IllegalArgumentException("recipe not found: " + recipeId);
        return target.addBannerRecipe(recipe.withId(null));
    }

    protected final BannerRecipe normalizeRecipeForPack(BannerRecipe recipe) {
        String id = recipe.id();
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString().replace("-", "");
        }
        if (!id.contains(":")) id = getMetadata().id() + ":" + id;
        return recipe.withId(normalizeRecipeId(id));
    }

    protected final void includeRecipe(BannerRecipe recipe) {
        recipes.put(recipe.id(), recipe);
    }

    protected final void excludeRecipe(String recipeId) {
        recipes.remove(recipeId);
    }

    public static DirectoryBannerPack createDirectoryPack(Path packDir, BannerPackMetadata metadata)
            throws IOException {
        Files.createDirectories(packDir.resolve(BANNERS_DIR));
        writeMcmeta(packDir, metadata);
        return new DirectoryBannerPack(metadata, packDir);
    }

    public static DirectoryBannerPack loadDirectoryPack(Path packDir, String packId) throws IOException {
        BannerPackMetadata metadata = readMcmeta(packDir.resolve(MCMETA_FILE), packId);
        if (metadata == null) return null;
        return new DirectoryBannerPack(
                metadata,
                packDir,
                packDir.resolve(BANNERS_DIR),
                packDir.resolve(CATEGORIES_DIR),
                packDir.resolve(CATEGORIES_DIR).resolve("lang"));
    }

    public static ZipBannerPack loadZipPack(Path zipPath, String packId) throws IOException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(zipPath, (ClassLoader) null)) {
            Path root = fileSystem.getPath("/");
            BannerPackMetadata metadata = readMcmeta(root.resolve(MCMETA_FILE), packId);
            if (metadata == null) return null;
            return new ZipBannerPack(
                    metadata,
                    zipPath,
                    root.resolve(BANNERS_DIR),
                    root.resolve(CATEGORIES_DIR),
                    root.resolve(CATEGORIES_DIR).resolve("lang"));
        }
    }

    public static void writeMcmeta(Path packDir, BannerPackMetadata metadata) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 1);
        pack.addProperty("description", metadata.description() == null ? "" : metadata.description());
        root.add("pack", pack);

        JsonObject packMetadata = new JsonObject();
        addIfPresent(packMetadata, "author", metadata.author());
        addIfPresent(packMetadata, "url", metadata.url());
        if (!packMetadata.isEmpty()) root.add("bannerpack", packMetadata);

        Files.createDirectories(packDir);
        try (Writer writer = Files.newBufferedWriter(packDir.resolve(MCMETA_FILE))) {
            GSON.toJson(root, writer);
        }
    }

    private static BannerPackMetadata readMcmeta(Path path, String packId) throws IOException {
        if (!Files.exists(path)) return null;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject pack = root.getAsJsonObject("pack");
            if (pack == null || !pack.has("pack_format") || !pack.has("description")) {
                throw new IllegalArgumentException("invalid bannerpack metadata: " + path);
            }
            int format = pack.get("pack_format").getAsInt();
            if (format != 1) throw new IllegalArgumentException("unsupported bannerpack format " + format);

            JsonObject packMetadata = root.has("bannerpack") ? root.getAsJsonObject("bannerpack") : new JsonObject();
            return new BannerPackMetadata(
                    packId,
                    pack.get("description").getAsString(),
                    getOptionalString(packMetadata, "author"),
                    getOptionalString(packMetadata, "url"));
        }
    }

    protected final void loadRecipesFromPath(Path bannersPath) throws IOException {
        for (Path namespaceDir : childDirectories(bannersPath)) {
            String namespace = namespaceDir.getFileName().toString();
            for (Path recipeFile : childJsonFiles(namespaceDir)) {
                String fileName = recipeFile.getFileName().toString();
                String recipeName = fileName.substring(0, fileName.length() - ".json".length());
                try (Reader reader = Files.newBufferedReader(recipeFile)) {
                    BannerRecipe recipe =
                            parseRecipe(JsonParser.parseReader(reader).getAsJsonObject());
                    includeRecipe(recipe.withId(normalizeRecipeId(namespace + ":" + recipeName)));
                }
            }
        }
    }

    protected final void loadCategoriesFromPath(Path categoriesPath) throws IOException {
        List<BannerRecipeCategory> loaded = new ArrayList<>();
        for (Path namespaceDir : childDirectories(categoriesPath)) {
            if ("lang".equals(namespaceDir.getFileName().toString())) continue;
            for (Path categoryFile : childJsonFiles(namespaceDir)) {
                String fileName = categoryFile.getFileName().toString();
                String id = fileName.substring(0, fileName.length() - ".json".length());
                try (Reader reader = Files.newBufferedReader(categoryFile)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    String description = getOptionalString(json, "description");
                    String icon = getOptionalString(json, "icon");
                    loaded.add(new BannerRecipeCategory(
                            id,
                            description == null || description.isBlank() ? id : description,
                            icon == null || icon.isBlank() ? "minecraft:lava_bucket" : icon));
                }
            }
        }
        categories = List.copyOf(loaded);
    }

    protected final void loadCategoryLangFiles(Path langDir) throws IOException {
        Map<String, Map<String, String>> loaded = new LinkedHashMap<>();
        for (Path file : childJsonFiles(langDir)) {
            String name = file.getFileName().toString();
            String locale = name.substring(0, name.length() - ".json".length()).toLowerCase(Locale.ROOT);
            Map<String, String> translations = new LinkedHashMap<>();
            try (Reader reader = Files.newBufferedReader(file)) {
                for (Map.Entry<String, JsonElement> entry :
                        JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
                    translations.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            loaded.put(locale, Map.copyOf(translations));
        }
        categoryTranslations = Map.copyOf(loaded);
    }

    private static BannerRecipe parseRecipe(JsonObject json) {
        String description = getOptionalString(json, "description");
        if (description == null || description.isBlank()) description = "Unnamed banner";
        String category = getOptionalString(json, "category");
        if (category == null || category.isBlank()) category = "misc";
        String bannerColor = getOptionalString(json, "banner_color");
        if (bannerColor == null || bannerColor.isBlank()) {
            bannerColor = "white";
        } else if (DyeColor.byName(bannerColor, null) == null) {
            throw new IllegalArgumentException("invalid banner color: " + bannerColor);
        }

        List<BannerRecipeLayer> layers = new ArrayList<>();
        if (json.has("layers")) {
            for (JsonElement element : json.getAsJsonArray("layers")) {
                JsonObject layer = element.getAsJsonObject();
                String pattern = getOptionalString(layer, "pattern");
                String color = getOptionalString(layer, "color");
                if (pattern == null || color == null) {
                    throw new IllegalArgumentException("recipe layer requires pattern and color");
                }
                layers.add(BannerRecipeLayer.of(pattern, color));
            }
        }

        return new BannerRecipe(
                null,
                description,
                getOptionalString(json, "author"),
                getOptionalString(json, "url"),
                category,
                bannerColor,
                layers);
    }

    protected static void writeRecipe(Path file, BannerRecipe recipe) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("description", recipe.description());
        addIfPresent(json, "author", recipe.author());
        addIfPresent(json, "url", recipe.url());
        json.addProperty("category", recipe.category());
        json.addProperty("banner_color", recipe.bannerColor());
        com.google.gson.JsonArray layers = new com.google.gson.JsonArray();
        json.add("layers", layers);
        for (BannerRecipeLayer layer : recipe.layers()) {
            JsonObject jsonLayer = new JsonObject();
            jsonLayer.addProperty("pattern", layer.pattern().toString());
            jsonLayer.addProperty("color", layer.color().getName());
            layers.add(jsonLayer);
        }
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(json, writer);
        }
    }

    protected static Path recipeFile(Path packPath, String recipeId) {
        Identifier id = Identifier.tryParse(normalizeRecipeId(recipeId));
        return packPath.resolve(BANNERS_DIR).resolve(id.getNamespace()).resolve(id.getPath() + ".json");
    }

    private static String normalizeRecipeId(String recipeId) {
        Identifier id = Identifier.tryParse(recipeId);
        if (id == null || id.getPath().contains("/")) {
            throw new IllegalArgumentException("invalid recipe ID: " + recipeId);
        }
        return id.toString();
    }

    private static List<Path> childDirectories(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return List.of();
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isDirectory).sorted().toList();
        }
    }

    private static List<Path> childJsonFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return List.of();
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static String getOptionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
    }

    private static void addIfPresent(JsonObject json, String key, String value) {
        if (value != null && !value.isBlank()) json.addProperty(key, value);
    }
}
