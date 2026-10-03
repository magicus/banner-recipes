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
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class InstalledPackRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "downloaded-bannerpacks.json";

    private final Path registryFile =
            FabricLoader.getInstance().getConfigDir().resolve("banner-recipes").resolve(FILE_NAME);
    private final Map<String, InstalledPackState> entries = new LinkedHashMap<>();

    public InstalledPackRegistry() {
        load();
    }

    public boolean isManaged(String packId) {
        return this.entries.containsKey(packId);
    }

    public InstalledPackState getState(String packId) {
        return this.entries.get(packId);
    }

    public void put(InstalledPackState state) {
        this.entries.put(state.packId(), state);
        this.save();
    }

    public void remove(String packId) {
        if (this.entries.remove(packId) != null) this.save();
    }

    private void load() {
        if (!Files.exists(this.registryFile)) return;
        try {
            RegistryData data =
                    GSON.fromJson(Files.readString(this.registryFile, StandardCharsets.UTF_8), RegistryData.class);
            if (data != null && data.installedPacks != null) {
                for (InstalledPackState state : data.installedPacks) {
                    if (state == null || state.packId() == null) continue;
                    this.entries.put(state.packId(), state);
                }
            }
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Failed to load downloaded banner-pack registry " + this.registryFile, e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(this.registryFile.getParent());
            Path temporaryFile = this.registryFile.resolveSibling(FILE_NAME + ".tmp");
            Files.writeString(
                    temporaryFile,
                    GSON.toJson(new RegistryData(new ArrayList<>(this.entries.values()))),
                    StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporaryFile,
                        this.registryFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, this.registryFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save downloaded banner-pack registry " + this.registryFile, e);
        }
    }

    private record RegistryData(ArrayList<InstalledPackState> installedPacks) {}
}
