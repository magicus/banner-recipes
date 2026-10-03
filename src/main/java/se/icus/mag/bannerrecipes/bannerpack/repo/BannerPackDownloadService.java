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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.Config;
import se.icus.mag.bannerrecipes.bannerpack.BannerPack;

public final class BannerPackDownloadService {
    private final BannerPackRepoClient client;
    private final Path packsRoot;

    public BannerPackDownloadService(Path packsRoot) {
        this(new BannerPackRepoClient(Config.REPO_INDEX_URL), packsRoot);
    }

    BannerPackDownloadService(BannerPackRepoClient client, Path packsRoot) {
        this.client = client;
        this.packsRoot = packsRoot;
    }

    public List<String> autoInstallDefaults() throws IOException {
        Files.createDirectories(packsRoot);
        RemoteRepoIndex index = client.fetchIndex();
        List<String> installedPackIds = new ArrayList<>();
        for (String packId : Config.AUTO_INSTALL_PACK_IDS) {
            Path zipPath = packsRoot.resolve(packId + ".zip");
            if (Files.exists(zipPath) || Files.isDirectory(packsRoot.resolve(packId))) {
                BannerRecipesMod.LOGGER.info("Skipping auto-install of '{}': it is already on disk", packId);
                continue;
            }

            RemotePackEntry entry = index.packs().stream()
                    .filter(pack -> pack != null && packId.equals(pack.id()))
                    .findFirst()
                    .orElse(null);
            if (entry == null) {
                BannerRecipesMod.LOGGER.warn("Auto-install skipped: '{}' is absent from the repository index", packId);
                continue;
            }
            try {
                install(entry, zipPath);
                installedPackIds.add(packId);
            } catch (IOException e) {
                BannerRecipesMod.LOGGER.warn("Auto-install of '{}' failed: {}", packId, e.getMessage());
            }
        }
        return List.copyOf(installedPackIds);
    }

    private void install(RemotePackEntry entry, Path zipPath) throws IOException {
        if (entry.id() == null || !entry.id().matches("[a-z0-9_-]+")) {
            throw new IOException("Invalid pack ID in repository index");
        }
        if (entry.sha256() == null || !entry.sha256().matches("[0-9a-fA-F]{64}")) {
            throw new IOException("Invalid SHA-256 in repository index for " + entry.id());
        }
        if (entry.downloadUrl() == null || entry.downloadUrl().isBlank()) {
            throw new IOException("Missing download URL in repository index for " + entry.id());
        }

        Path temporaryPath = packsRoot.resolve(entry.id() + ".zip.tmp");
        Files.deleteIfExists(temporaryPath);
        try {
            client.downloadZip(entry.downloadUrl(), temporaryPath);
            String actualHash = sha256Hex(temporaryPath);
            if (!actualHash.equalsIgnoreCase(entry.sha256())) {
                throw new IOException("SHA-256 mismatch while downloading banner pack " + entry.id());
            }
            if (BannerPack.loadZipPack(temporaryPath, entry.id()) == null) {
                throw new IOException("Downloaded file is not a valid banner pack: " + entry.id());
            }
            try {
                Files.move(temporaryPath, zipPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryPath, zipPath, StandardCopyOption.REPLACE_EXISTING);
            }
            BannerRecipesMod.LOGGER.info("Downloaded banner pack '{}'", entry.id());
        } catch (IOException | IllegalArgumentException | IllegalStateException | JsonParseException e) {
            Files.deleteIfExists(temporaryPath);
            throw new IOException("Failed to validate or install banner pack " + entry.id(), e);
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
