/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.screens.packselection;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.bannerpack.BannerPack;
import se.icus.mag.bannerrecipes.gui.screens.packdownload.BannerPackDownloadManagementScreen;
import se.icus.mag.bannerrecipes.storage.AvailableBannerPacks;
import se.icus.mag.bannerrecipes.storage.SelectedBannerPacks;

public final class BannerPackSelectionScreen extends Screen {
    private static final Component AVAILABLE_TITLE = Component.translatable("pack.available.title");
    private static final Component SELECTED_TITLE = Component.translatable("pack.selected.title");
    private static final Component OPEN_PACK_DIR_TITLE = Component.translatable("pack.openFolder");
    private static final Component SEARCH =
            Component.translatable("gui.packSelection.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final Component DRAG_AND_DROP =
            Component.translatable("pack.dropInfo").withStyle(ChatFormatting.GRAY);
    private static final Component PACK_FOLDER_TOOLTIP = Component.translatable("pack.folderInfo");
    private static final int LIST_WIDTH = 200;
    private static final int HEADER_ELEMENT_SPACING = 4;
    private static final int SEARCH_BOX_HEIGHT = 15;
    private static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final SelectedBannerPacks selectedPacks;
    private final Screen parentScreen;
    private final Map<String, Identifier> packIcons = new HashMap<>();

    private BannerPackListWidget availablePackList;
    private BannerPackListWidget selectedPackList;
    private EditBox search;
    private Button doneButton;

    public BannerPackSelectionScreen(Screen parentScreen) {
        super(Component.literal("Select Banner Packs"));
        this.parentScreen = parentScreen;
        this.selectedPacks = SelectedBannerPacks.getInstance();
        this.selectedPacks.load();
    }

    @Override
    public void onClose() {
        this.selectedPacks.load();
        this.minecraft.gui.setScreen(this.parentScreen);
    }

    @Override
    protected void init() {
        this.layout.setHeaderHeight(SEARCH_BOX_HEIGHT + 34);
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(HEADER_ELEMENT_SPACING));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.getTitle(), this.font));
        header.addChild(new StringWidget(DRAG_AND_DROP, this.font));
        this.search = header.addChild(new EditBox(this.font, 0, 0, LIST_WIDTH, SEARCH_BOX_HEIGHT, Component.empty()));
        this.search.setHint(SEARCH);
        this.search.setResponder(this::updateFilteredEntries);

        this.availablePackList = this.layout.addToContents(
                new BannerPackListWidget(this.minecraft, this, LIST_WIDTH, this.height - 66, AVAILABLE_TITLE, false));
        this.selectedPackList = this.layout.addToContents(
                new BannerPackListWidget(this.minecraft, this, LIST_WIDTH, this.height - 66, SELECTED_TITLE, true));

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(
                        Component.translatable("banner-recipes.screen.download_packs.title"),
                        button -> this.minecraft.gui.setScreen(new BannerPackDownloadManagementScreen(this)))
                .build());
        footer.addChild(Button.builder(OPEN_PACK_DIR_TITLE, button -> this.openPackFolder())
                .tooltip(Tooltip.create(PACK_FOLDER_TOOLTIP))
                .build());
        this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
        this.reload();
    }

    @Override
    protected void setInitialFocus() {
        if (this.search != null) {
            this.setInitialFocus(this.search);
        } else {
            super.setInitialFocus();
        }
    }

    private void updateFilteredEntries(String value) {
        if (this.availablePackList == null || this.selectedPackList == null) return;
        String lowerCaseValue = value.toLowerCase(Locale.ROOT);
        Stream<BannerPackModelEntry> available = this.getAvailablePacks().values().stream()
                .filter(pack -> !this.isPackSelected(pack.getMetadata().id()))
                .map(pack -> this.toEntry(pack, false))
                .filter(entry -> this.matchesFilter(entry, value, lowerCaseValue));
        Stream<BannerPackModelEntry> selected = this.getAvailablePacks().values().stream()
                .filter(pack -> this.isPackSelected(pack.getMetadata().id()))
                .map(pack -> this.toEntry(pack, true))
                .filter(entry -> this.matchesFilter(entry, value, lowerCaseValue));
        this.availablePackList.updateList(available);
        this.selectedPackList.updateList(selected);
    }

    private boolean matchesFilter(BannerPackModelEntry entry, String value, String lowerCaseValue) {
        return value.isBlank()
                || entry.packId().toLowerCase(Locale.ROOT).contains(lowerCaseValue)
                || entry.title().toLowerCase(Locale.ROOT).contains(lowerCaseValue)
                || entry.description().getString().toLowerCase(Locale.ROOT).contains(lowerCaseValue);
    }

    private BannerPackModelEntry toEntry(BannerPack pack, boolean selected) {
        String packId = pack.getMetadata().id();
        return new BannerPackModelEntry(
                packId, displayName(pack), description(pack), secondLine(pack), this.getPackIcon(pack), selected);
    }

    private static Component description(BannerPack pack) {
        String description = pack.getMetadata().description();
        return description == null || description.isBlank() ? Component.empty() : Component.literal(description);
    }

    private static Component secondLine(BannerPack pack) {
        String author = pack.getMetadata().author();
        return author != null && !author.isBlank() ? Component.literal(author) : description(pack);
    }

    private static String displayName(BannerPack pack) {
        Path fileName = pack.getPath().getFileName();
        if (fileName == null) return pack.getMetadata().id();
        String name = fileName.toString();
        return name.toLowerCase(Locale.ROOT).endsWith(".zip") ? name.substring(0, name.length() - 4) : name;
    }

    private boolean isPackSelected(String packId) {
        return this.selectedPacks.getSelectedPackIds().contains(packId);
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.availablePackList != null) {
            this.availablePackList.updateSizeAndPosition(
                    LIST_WIDTH,
                    this.layout.getContentHeight(),
                    this.width / 2 - 15 - LIST_WIDTH,
                    this.layout.getHeaderHeight());
        }
        if (this.selectedPackList != null) {
            this.selectedPackList.updateSizeAndPosition(
                    LIST_WIDTH, this.layout.getContentHeight(), this.width / 2 + 15, this.layout.getHeaderHeight());
        }
    }

    private void reload() {
        this.packIcons.clear();
        this.updateFilteredEntries(this.search == null ? "" : this.search.getValue());
        if (this.doneButton != null && this.selectedPackList != null) {
            this.doneButton.active = !this.selectedPackList.children().isEmpty();
        }
    }

    public void refreshFromStorage() {
        this.selectedPacks.load();
        this.reload();
    }

    public void movePackToSelected(String packId) {
        if (!this.isPackSelected(packId)) this.selectedPacks.selectPack(packId);
        this.reload();
    }

    public void movePackToAvailable(String packId) {
        if (AvailableBannerPacks.LOCAL_PACK_ID.equals(packId)) return;
        this.selectedPacks.deselectPack(packId);
        this.reload();
    }

    private void openPackFolder() {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.OPEN)) {
                throw new UnsupportedOperationException("Opening folders is not supported on this platform");
            }
            desktop.open(this.selectedPacks.getAvailablePacks().getPacksRoot().toFile());
        } catch (IOException | UnsupportedOperationException e) {
            BannerRecipesMod.LOGGER.warn("Failed to open banner-pack folder", e);
        }
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        if (files.isEmpty()) return;
        for (Path file : files) {
            try {
                this.copyPackToRoot(file);
            } catch (IOException e) {
                BannerRecipesMod.LOGGER.error("Failed to add dropped banner pack {}", file, e);
            }
        }
        this.refreshFromStorage();
    }

    private Map<String, BannerPack> getAvailablePacks() {
        return this.selectedPacks.getAvailablePacks().getAvailablePacks();
    }

    private Identifier getPackIcon(BannerPack pack) {
        return this.packIcons.computeIfAbsent(pack.getMetadata().id(), ignored -> this.loadPackIcon(pack));
    }

    private Identifier loadPackIcon(BannerPack pack) {
        Path packPath = pack.getPath();
        try {
            if (Files.isDirectory(packPath)) return this.loadIconTexture(pack, packPath.resolve("pack.png"));
            try (FileSystem fileSystem = FileSystems.newFileSystem(packPath, (ClassLoader) null)) {
                return this.loadIconTexture(pack, fileSystem.getPath("/pack.png"));
            }
        } catch (IOException e) {
            BannerRecipesMod.LOGGER.warn(
                    "Failed to load icon for banner pack {}", pack.getMetadata().id(), e);
            return DEFAULT_ICON;
        }
    }

    private void copyPackToRoot(Path source) throws IOException {
        Path targetRoot = this.selectedPacks.getAvailablePacks().getPacksRoot();
        if (Files.isDirectory(source)) {
            copyDirectory(source, targetRoot.resolve(source.getFileName().toString()));
        } else if (source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            Files.copy(
                    source, targetRoot.resolve(source.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            paths.forEach(path -> {
                try {
                    Path destination = target.resolve(source.relativize(path).toString());
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.createDirectories(destination.getParent());
                        Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private Identifier loadIconTexture(BannerPack pack, Path iconPath) throws IOException {
        if (!Files.exists(iconPath)) return DEFAULT_ICON;
        String id = Util.sanitizeName(pack.getMetadata().id(), Identifier::validPathChar);
        Identifier location = Identifier.withDefaultNamespace("pack/" + id + "/icon");
        try (InputStream stream = Files.newInputStream(iconPath)) {
            NativeImage iconImage = NativeImage.read(stream);
            TextureManager textureManager = this.minecraft.getTextureManager();
            textureManager.register(location, new DynamicTexture(location::toString, iconImage));
            return location;
        }
    }
}
