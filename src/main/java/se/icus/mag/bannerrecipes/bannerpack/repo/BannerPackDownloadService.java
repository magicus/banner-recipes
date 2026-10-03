/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.bannerpack.repo;

import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.util.Util;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.Config;
import se.icus.mag.bannerrecipes.bannerpack.BannerPack;
import se.icus.mag.bannerrecipes.storage.AvailableBannerPacks;
import se.icus.mag.bannerrecipes.storage.InstalledPackRegistry;
import se.icus.mag.bannerrecipes.storage.InstalledPackState;
import se.icus.mag.bannerrecipes.storage.SelectedBannerPacks;

public final class BannerPackDownloadService {
    private final BannerPackRepoClient client;
    private final InstalledPackRegistry registry;
    private final Path packsRoot;

    public BannerPackDownloadService(Path packsRoot) {
        this(new BannerPackRepoClient(Config.REPO_INDEX_URL), new InstalledPackRegistry(), packsRoot);
    }

    BannerPackDownloadService(BannerPackRepoClient client, InstalledPackRegistry registry, Path packsRoot) {
        this.client = client;
        this.registry = registry;
        this.packsRoot = packsRoot;
    }

    public CompletableFuture<RemoteRepoIndex> fetchIndex() {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return this.client.fetchIndex();
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                },
                Util.ioPool());
    }

    public List<PackUpdateStatus> getPackStatuses(RemoteRepoIndex index) {
        List<PackUpdateStatus> statuses = new ArrayList<>();
        for (RemotePackEntry entry : index.packs()) {
            InstalledPackState installed = this.registry.getState(entry.id());
            PackUpdateStatus.InstallStatus status;
            if (installed == null) {
                AvailableBannerPacks available =
                        SelectedBannerPacks.getInstance().getAvailablePacks();
                boolean existsUnmanaged = available.getAvailablePack(entry.id()) != null
                        || Files.exists(this.packsRoot.resolve(entry.id() + ".zip"))
                        || Files.isDirectory(this.packsRoot.resolve(entry.id()));
                status = existsUnmanaged
                        ? PackUpdateStatus.InstallStatus.CONFLICT_UNMANAGED
                        : PackUpdateStatus.InstallStatus.NOT_INSTALLED;
            } else if (entry.sha256().equalsIgnoreCase(installed.sha256())) {
                status = PackUpdateStatus.InstallStatus.INSTALLED_UP_TO_DATE;
            } else {
                status = PackUpdateStatus.InstallStatus.UPDATE_AVAILABLE;
            }
            statuses.add(new PackUpdateStatus(entry, status));
        }
        return statuses;
    }

    public CompletableFuture<InstallResult> install(RemotePackEntry entry, boolean isUpdate) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return this.doInstall(entry, isUpdate);
                    } catch (IOException | IllegalArgumentException | IllegalStateException | JsonParseException e) {
                        BannerRecipesMod.LOGGER.error("Failed to install banner pack {}", entry.id(), e);
                        return InstallResult.DOWNLOAD_FAILED;
                    }
                },
                Util.ioPool());
    }

    private InstallResult doInstall(RemotePackEntry entry, boolean isUpdate) throws IOException {
        validateEntry(entry);
        Path installedZip = this.packsRoot.resolve(entry.id() + ".zip");
        boolean existsOnDisk = Files.exists(installedZip) || Files.isDirectory(this.packsRoot.resolve(entry.id()));
        boolean managed = this.registry.isManaged(entry.id());

        if (existsOnDisk && !managed) return InstallResult.CONFLICT_UNMANAGED;
        if (existsOnDisk && managed && !isUpdate) return InstallResult.CONFLICT_UNMANAGED;

        Files.createDirectories(this.packsRoot);
        Path temporaryZip = this.packsRoot.resolve(entry.id() + ".zip.tmp");
        Files.deleteIfExists(temporaryZip);
        try {
            this.client.downloadZip(entry.downloadUrl(), temporaryZip);
            String actualHash = sha256Hex(temporaryZip);
            if (!actualHash.equalsIgnoreCase(entry.sha256())) {
                Files.deleteIfExists(temporaryZip);
                return InstallResult.HASH_MISMATCH;
            }
            if (BannerPack.loadZipPack(temporaryZip, entry.id()) == null) {
                throw new IOException("Downloaded file is not a valid banner pack: " + entry.id());
            }
            moveIntoPlace(temporaryZip, installedZip);
        } catch (IOException | IllegalArgumentException | IllegalStateException | JsonParseException e) {
            Files.deleteIfExists(temporaryZip);
            throw e;
        }

        this.registry.put(new InstalledPackState(
                entry.id(),
                entry.id() + ".zip",
                entry.downloadUrl(),
                entry.sha256(),
                Instant.now().toString()));
        return managed ? InstallResult.SUCCESS_UPDATED : InstallResult.SUCCESS_NEW;
    }

    public CompletableFuture<Void> delete(String packId) {
        return CompletableFuture.runAsync(
                () -> {
                    if (!this.registry.isManaged(packId)) return;
                    if (packId == null || !packId.matches("[a-z0-9_-]+")) {
                        throw new IllegalArgumentException("Invalid banner pack ID: " + packId);
                    }
                    try {
                        Files.deleteIfExists(this.packsRoot.resolve(packId + ".zip"));
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                    this.registry.remove(packId);
                    SelectedBannerPacks.getInstance().load();
                },
                Util.ioPool());
    }

    public List<String> autoInstallDefaults() throws IOException {
        Files.createDirectories(this.packsRoot);
        RemoteRepoIndex index = this.client.fetchIndex();
        List<String> installedPackIds = new ArrayList<>();
        for (String packId : Config.AUTO_INSTALL_PACK_IDS) {
            if (Files.exists(this.packsRoot.resolve(packId + ".zip"))
                    || Files.isDirectory(this.packsRoot.resolve(packId))) {
                BannerRecipesMod.LOGGER.info("Skipping auto-install of '{}': it is already on disk", packId);
                continue;
            }
            RemotePackEntry entry = index.packs().stream()
                    .filter(pack -> packId.equals(pack.id()))
                    .findFirst()
                    .orElse(null);
            if (entry == null) {
                BannerRecipesMod.LOGGER.warn("Auto-install skipped: '{}' is absent from the repository index", packId);
                continue;
            }
            try {
                validateEntry(entry);
                Path zipPath = this.packsRoot.resolve(entry.id() + ".zip");
                installFile(entry, zipPath);
                this.registry.put(new InstalledPackState(
                        entry.id(),
                        entry.id() + ".zip",
                        entry.downloadUrl(),
                        entry.sha256(),
                        Instant.now().toString()));
                installedPackIds.add(packId);
            } catch (IOException | IllegalArgumentException | IllegalStateException | JsonParseException e) {
                BannerRecipesMod.LOGGER.warn("Auto-install of '{}' failed: {}", packId, e.getMessage());
            }
        }
        return List.copyOf(installedPackIds);
    }

    private void installFile(RemotePackEntry entry, Path zipPath) throws IOException {
        Path temporaryPath = this.packsRoot.resolve(entry.id() + ".zip.tmp");
        Files.deleteIfExists(temporaryPath);
        try {
            this.client.downloadZip(entry.downloadUrl(), temporaryPath);
            String actualHash = sha256Hex(temporaryPath);
            if (!actualHash.equalsIgnoreCase(entry.sha256())) {
                throw new IOException("SHA-256 mismatch while downloading banner pack " + entry.id());
            }
            if (BannerPack.loadZipPack(temporaryPath, entry.id()) == null) {
                throw new IOException("Downloaded file is not a valid banner pack: " + entry.id());
            }
            moveIntoPlace(temporaryPath, zipPath);
        } catch (IOException | IllegalArgumentException | IllegalStateException | JsonParseException e) {
            Files.deleteIfExists(temporaryPath);
            throw e;
        }
    }

    private static void validateEntry(RemotePackEntry entry) throws IOException {
        if (entry == null || entry.id() == null || !entry.id().matches("[a-z0-9_-]+")) {
            throw new IOException("Invalid pack ID in repository index");
        }
        if (entry.sha256() == null || !entry.sha256().matches("[0-9a-fA-F]{64}")) {
            throw new IOException("Invalid SHA-256 in repository index for " + entry.id());
        }
        if (entry.downloadUrl() == null || entry.downloadUrl().isBlank()) {
            throw new IOException("Missing download URL in repository index for " + entry.id());
        }
    }

    private static void moveIntoPlace(Path temporaryPath, Path targetPath) throws IOException {
        try {
            Files.move(temporaryPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporaryPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256Hex(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
