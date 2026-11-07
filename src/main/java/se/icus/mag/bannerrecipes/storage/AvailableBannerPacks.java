/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.bannerpack.BannerPack;
import se.icus.mag.bannerrecipes.bannerpack.BannerPackMetadata;
import se.icus.mag.bannerrecipes.bannerpack.DirectoryBannerPack;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

public final class AvailableBannerPacks {
    public static final BannerPackId LOCAL_PACK_ID = BannerPackId.of("local");
    private static final Pattern INVALID_ID_CHARACTERS = Pattern.compile("[^a-z0-9_-]");

    private final Path packsRoot;
    private final Map<BannerPackId, BannerPack> packs = new LinkedHashMap<>();

    public AvailableBannerPacks(Path packsRoot) {
        this.packsRoot = packsRoot;
    }

    public void load() {
        packs.clear();
        try {
            Files.createDirectories(packsRoot);
            ensureLocalPackExists();
            try (Stream<Path> entries = Files.list(packsRoot)) {
                for (Path entry : entries.sorted().toList()) {
                    try {
                        String name = entry.getFileName().toString();
                        BannerPack pack = null;
                        if (Files.isDirectory(entry)) {
                            pack = BannerPack.loadDirectoryPack(entry, normalizeId(name));
                        } else if (name.endsWith(".zip")) {
                            pack = BannerPack.loadZipPack(
                                    entry, normalizeId(name.substring(0, name.length() - ".zip".length())));
                        }
                        if (pack != null) {
                            BannerPack duplicate =
                                    packs.putIfAbsent(pack.getMetadata().id(), pack);
                            if (duplicate != null) {
                                BannerRecipesMod.LOGGER.warn(
                                        "Ignoring duplicate banner pack ID '{}' at {}",
                                        pack.getMetadata().id(),
                                        entry);
                            }
                        }
                    } catch (IOException | RuntimeException e) {
                        BannerRecipesMod.LOGGER.error("Failed to load banner pack from {}", entry, e);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize banner pack repository at " + packsRoot, e);
        }
    }

    public Path getPacksRoot() {
        return packsRoot;
    }

    public Map<BannerPackId, BannerPack> getAvailablePacks() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(packs));
    }

    public List<BannerPackId> getAvailablePackIds() {
        return List.copyOf(packs.keySet());
    }

    public BannerPack getAvailablePack(BannerPackId packId) {
        return packs.get(packId);
    }

    public List<BannerRecipe> getAllBannerRecipes() {
        List<BannerRecipe> result = new ArrayList<>();
        for (BannerPack pack : packs.values()) result.addAll(pack.getDesigns());
        return List.copyOf(result);
    }

    public BannerRecipe getBannerRecipeById(String recipeId) {
        for (BannerPack pack : packs.values()) {
            BannerRecipe recipe = pack.getDesign(recipeId);
            if (recipe != null) return recipe;
        }
        return null;
    }

    public BannerPackId getBannerRecipePackId(String recipeId) {
        for (BannerPack pack : packs.values()) {
            if (pack.getDesign(recipeId) != null) return pack.getMetadata().id();
        }
        return null;
    }

    public BannerRecipe addBannerRecipe(BannerPackId packId, BannerRecipe recipe) {
        BannerPack pack = requirePack(packId);
        try {
            return pack.addBannerRecipe(recipe);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to add recipe to pack " + packId, e);
        }
    }

    public void updateBannerRecipe(BannerRecipe recipe) {
        BannerPackId packId = getBannerRecipePackId(recipe.id());
        if (packId == null) throw new IllegalArgumentException("recipe not found: " + recipe.id());
        try {
            requirePack(packId).updateBannerRecipe(recipe);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to update recipe " + recipe.id(), e);
        }
    }

    public void removeBannerRecipe(String recipeId) {
        BannerPackId packId = getBannerRecipePackId(recipeId);
        if (packId == null) throw new IllegalArgumentException("recipe not found: " + recipeId);
        try {
            requirePack(packId).removeBannerRecipe(recipeId);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to remove recipe " + recipeId, e);
        }
    }

    public BannerRecipe copyBannerRecipe(BannerPackId sourcePackId, BannerPackId targetPackId, String recipeId) {
        try {
            return requirePack(sourcePackId).copyRecipeTo(requirePack(targetPackId), recipeId);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to copy recipe " + recipeId, e);
        }
    }

    public BannerRecipe moveBannerRecipe(BannerPackId sourcePackId, BannerPackId targetPackId, String recipeId) {
        BannerPack source = requirePack(sourcePackId);
        if (!(source instanceof DirectoryBannerPack directory)) {
            throw new IllegalArgumentException("cannot move from a read-only pack; copy it instead");
        }
        try {
            return directory.moveRecipeTo(requirePack(targetPackId), recipeId);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to move recipe " + recipeId, e);
        }
    }

    private void ensureLocalPackExists() throws IOException {
        Path localDir = packsRoot.resolve(LOCAL_PACK_ID.id());
        Files.createDirectories(localDir.resolve(BannerPack.BANNERS_DIR));
        Path mcmeta = localDir.resolve(BannerPack.MCMETA_FILE);
        if (!Files.exists(mcmeta))
            BannerPack.writeMcmeta(localDir, new BannerPackMetadata(LOCAL_PACK_ID, "Local", null, null));
    }

    private BannerPack requirePack(BannerPackId packId) {
        BannerPack pack = packs.get(packId);
        if (pack == null) throw new IllegalArgumentException("pack not found: " + packId);
        return pack;
    }

    private static BannerPackId normalizeId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("pack ID cannot be empty");
        String sanitized =
                INVALID_ID_CHARACTERS.matcher(id.toLowerCase(Locale.ROOT)).replaceAll("_");
        return BannerPackId.of(sanitized);
    }
}
