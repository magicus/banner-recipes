/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SelectedPacksConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "bannerpacks.json";

    private final Path configFile;
    private List<String> selectedPacks;

    public SelectedPacksConfig(Path configDir) {
        this.configFile = configDir.resolve(CONFIG_FILE_NAME);
        load();
    }

    public List<String> getSelectedPacks() {
        return List.copyOf(selectedPacks);
    }

    public boolean isSelected(String packId) {
        return selectedPacks.contains(packId);
    }

    public void setSelectedPacks(List<String> packIds) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        normalized.add(AvailableBannerPacks.LOCAL_PACK_ID);
        for (String packId : packIds) {
            if (packId == null || packId.isBlank()) {
                throw new IllegalArgumentException("selected pack IDs cannot be empty");
            }
            normalized.add(packId);
        }
        selectedPacks = new ArrayList<>(normalized);
        save();
    }

    public void selectPack(String packId) {
        if (packId == null || packId.isBlank()) throw new IllegalArgumentException("pack ID cannot be empty");
        if (!selectedPacks.contains(packId)) {
            selectedPacks.add(packId);
            save();
        }
    }

    public void deselectPack(String packId) {
        if (AvailableBannerPacks.LOCAL_PACK_ID.equals(packId)) {
            throw new IllegalArgumentException("the local pack cannot be deselected");
        }
        if (selectedPacks.remove(packId)) save();
    }

    public void removeUnavailableSelections(Set<String> availablePackIds) {
        List<String> retained = selectedPacks.stream()
                .filter(packId ->
                        AvailableBannerPacks.LOCAL_PACK_ID.equals(packId) || availablePackIds.contains(packId))
                .toList();
        if (!retained.equals(selectedPacks)) {
            selectedPacks = new ArrayList<>(retained);
            save();
        }
    }

    private void load() {
        if (!Files.exists(configFile)) {
            selectedPacks = new ArrayList<>(List.of(AvailableBannerPacks.LOCAL_PACK_ID));
            save();
            return;
        }
        try {
            ConfigData data = GSON.fromJson(Files.readString(configFile, StandardCharsets.UTF_8), ConfigData.class);
            List<String> configuredIds = data == null || data.selectedPacks == null ? List.of() : data.selectedPacks;
            LinkedHashSet<String> normalized = new LinkedHashSet<>();
            normalized.add(AvailableBannerPacks.LOCAL_PACK_ID);
            for (String packId : configuredIds) {
                if (packId == null || packId.isBlank()) {
                    throw new IllegalArgumentException("selected pack IDs cannot be empty: " + configFile);
                }
                normalized.add(packId);
            }
            selectedPacks = new ArrayList<>(normalized);
            if (!selectedPacks.equals(new ArrayList<>(configuredIds))) save();
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Failed to load selected banner packs from " + configFile, e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(configFile.getParent());
            Path temporaryFile = configFile.resolveSibling(CONFIG_FILE_NAME + ".tmp");
            Files.writeString(temporaryFile, GSON.toJson(new ConfigData(selectedPacks)), StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporaryFile, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save selected banner packs to " + configFile, e);
        }
    }

    private record ConfigData(List<String> selectedPacks) {}
}
