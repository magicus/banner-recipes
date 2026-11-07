/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.screens.newpackselection;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.lang3.mutable.MutableBoolean;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.bannerpack.BannerPack;
import se.icus.mag.bannerrecipes.storage.BannerPackId;
import se.icus.mag.bannerrecipes.storage.SelectedBannerPacks;

public class PackSelectionManager {
    private static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");

    public final Map<BannerPackId, Identifier> packIcons = Maps.newHashMap();

    private final SelectedBannerPacks selectedPacks = SelectedBannerPacks.getInstance();

    public boolean isSelected(BannerPackId bannerPackId) {
        return selectedPacks.getSelectedPackIds().contains(bannerPackId);
    }

    public List<BannerPackId> getSelected() {
        return selectedPacks.getSelectedPackIds();
    }

    public List<BannerPackId> getUnselected() {
        return selectedPacks.getAvailablePacks().getAvailablePackIds().stream()
                .filter(p -> !isSelected(p))
                .toList();
    }

    public boolean canSelect(BannerPackId packId) {
        return !isSelected(packId);
    }

    public boolean canUnselect(BannerPackId packId) {
        return isSelected(packId);
    }

    public void select(BannerPackId packId) {
        selectedPacks.selectPack(packId);
    }

    public void unselect(BannerPackId packId) {
        selectedPacks.deselectPack(packId);
    }

    public Path getPacksRoot() {
        return selectedPacks.getAvailablePacks().getPacksRoot();
    }

    public void reload() {
        selectedPacks.reload();
    }

    public String getTitle(BannerPackId packId) {
        return selectedPacks
                .getAvailablePacks()
                .getAvailablePack(packId)
                .getMetadata()
                .description();
    }

    public String getDescription(BannerPackId packId) {
        // FIXME!!!!
        try {
            String desc = selectedPacks
                    .getAvailablePacks()
                    .getAvailablePack(packId)
                    .getMetadata()
                    .author();
            if (desc == null) {
                return "N/A";
            }
            return desc;
        } catch (Throwable e) {
            return "N/A-NPE";
        }
    }

    public Identifier getIconTexture(BannerPackId bannerPackId) {
        return getPackIcon(bannerPackId);
    }

    private Identifier getPackIcon(BannerPackId packId) {
        BannerPack pack = selectedPacks.getAvailablePacks().getAvailablePack(packId);
        return this.packIcons.computeIfAbsent(pack.getMetadata().id(), ignored -> this.loadPackIcon(pack));
    }

    private Identifier loadPackIcon(BannerPack pack) {
        Path packPath = pack.getPath();
        try {
            if (Files.isDirectory(packPath)) {
                return this.loadIconTexture(pack, packPath.resolve("pack.png"));
            }
            try (FileSystem fileSystem = FileSystems.newFileSystem(packPath, (ClassLoader) null)) {
                return this.loadIconTexture(pack, fileSystem.getPath("/pack.png"));
            }
        } catch (IOException e) {
            BannerRecipesMod.LOGGER.warn(
                    "Failed to load icon for banner pack {}", pack.getMetadata().id(), e);
            return DEFAULT_ICON;
        }
    }

    private Identifier loadIconTexture(BannerPack pack, Path iconPath) throws IOException {
        if (!Files.exists(iconPath)) return DEFAULT_ICON;
        String id = Util.sanitizeName(pack.getMetadata().id().id(), Identifier::validPathChar);
        Identifier location = Identifier.withDefaultNamespace("pack/" + id + "/icon");
        try (InputStream stream = Files.newInputStream(iconPath)) {
            NativeImage iconImage = NativeImage.read(stream);
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            textureManager.register(location, new DynamicTexture(location::toString, iconImage));
            return location;
        }
    }

    public boolean copyPacks(List<Path> properPacks) {
        Path targetDir = getPacksRoot();

        MutableBoolean success = new MutableBoolean(true);
        properPacks.forEach(pack -> {
            try (Stream<Path> contents = Files.walk(pack)) {
                contents.forEach(path -> {
                    try {
                        Util.copyBetweenDirs(pack.getParent(), targetDir, path);
                    } catch (IOException e) {
                        BannerRecipesMod.LOGGER.warn("Failed to copy datapack file from {} to {}", path, targetDir);
                        success.setFalse();
                    }
                });
            } catch (IOException e) {
                BannerRecipesMod.LOGGER.warn("Failed to copy datapack file from {} to {}", pack, targetDir);
                success.setFalse();
            }
        });
        return success.booleanValue();
    }

    public List<Path> getVerifiedPacks(List<Path> files) {
        List<Path> properPacks = new ArrayList<>();

        for (Path path : files) {
            boolean verified = verifyPack(path);
            if (verified) {
                properPacks.add(path);
            } else {
                BannerRecipesMod.LOGGER.warn("Path {} does not seem like pack", path);
            }
        }
        return properPacks;
    }

    private boolean verifyPack(Path path) {
        // FIXME!
        return true;
    }
}
