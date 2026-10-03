/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.screens.packdownload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.Config;
import se.icus.mag.bannerrecipes.bannerpack.repo.BannerPackDownloadService;
import se.icus.mag.bannerrecipes.bannerpack.repo.InstallResult;
import se.icus.mag.bannerrecipes.bannerpack.repo.PackUpdateStatus;
import se.icus.mag.bannerrecipes.bannerpack.repo.RemoteRepoIndex;
import se.icus.mag.bannerrecipes.gui.screens.packselection.BannerPackSelectionScreen;
import se.icus.mag.bannerrecipes.storage.SelectedBannerPacks;

public final class BannerPackDownloadManagementScreen extends Screen {
    private static final int LIST_WIDTH = 420;
    private static final int ROW_HEIGHT = 40;
    private static final int FOOTER_H = 32;
    private static final int BTN_ROW_H = 28;
    private static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");

    private final Screen previousScreen;
    private final BannerPackDownloadService service;
    private final Map<String, Identifier> iconCache = new HashMap<>();

    private PackListWidget packList;
    private StringWidget titleWidget;
    private Button checkUpdatesButton;
    private Button downloadButton;
    private Button deleteButton;
    private Checkbox activateCheckbox;
    private Button doneButton;

    private RemoteRepoIndex cachedIndex;
    private List<PackUpdateStatus> packStatuses = List.of();
    private boolean fetching;
    private String fetchError;
    private boolean operationInProgress;
    private String operationError;
    private String selectedPackIdBeforeRefresh;
    private boolean activateAfterDownload = Config.ACTIVATE_ON_DOWNLOAD;

    public BannerPackDownloadManagementScreen(Screen previousScreen) {
        super(Component.translatable("banner-recipes.screen.download_packs.title"));
        this.previousScreen = previousScreen;
        this.service = new BannerPackDownloadService(
                SelectedBannerPacks.getInstance().getAvailablePacks().getPacksRoot());
    }

    @Override
    protected void init() {
        int listX = (this.width - LIST_WIDTH) / 2;
        int listY = FOOTER_H;
        int bottomButtonY = this.height - BTN_ROW_H + 4;
        int topButtonY = bottomButtonY - BTN_ROW_H;
        int listHeight = topButtonY - 4 - listY;
        int buttonHeight = 20;

        this.titleWidget = this.addRenderableOnly(new StringWidget(this.getTitle(), this.font));
        this.titleWidget.setPosition((this.width - this.titleWidget.getWidth()) / 2, 8);

        this.activateCheckbox = Checkbox.builder(
                        Component.translatable("banner-recipes.screen.download_packs.activate_after_download"),
                        this.minecraft.font)
                .pos(0, topButtonY)
                .selected(this.activateAfterDownload)
                .maxWidth(280)
                .onValueChange((checkbox, value) -> this.activateAfterDownload = value)
                .build();
        int topRowWidth = 150 + 8 + this.activateCheckbox.getWidth();
        int buttonX = (this.width - topRowWidth) / 2;
        this.checkUpdatesButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("banner-recipes.screen.download_packs.check_updates"),
                        button -> this.onCheckUpdates())
                .bounds(buttonX, topButtonY, 150, buttonHeight)
                .build());
        buttonX += 150 + 8;
        this.activateCheckbox.setPosition(buttonX, topButtonY);
        this.addRenderableWidget(this.activateCheckbox);

        int bottomRowWidth = 80 + 8 + 80 + 8 + 60;
        buttonX = (this.width - bottomRowWidth) / 2;
        this.downloadButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("banner-recipes.screen.download_packs.action.download"),
                        button -> this.onDownload())
                .bounds(buttonX, bottomButtonY, 80, buttonHeight)
                .build());
        this.downloadButton.active = false;
        buttonX += 80 + 8;
        this.deleteButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("banner-recipes.screen.download_packs.action.delete"),
                        button -> this.onDelete())
                .bounds(buttonX, bottomButtonY, 80, buttonHeight)
                .build());
        this.deleteButton.active = false;
        buttonX += 80 + 8;
        this.doneButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(buttonX, bottomButtonY, 60, buttonHeight)
                .build());

        this.packList = new PackListWidget(this.minecraft, LIST_WIDTH, listHeight, listX, listY);
        this.addRenderableWidget(this.packList);
        if (this.packStatuses.isEmpty()) {
            this.onCheckUpdates();
        } else {
            this.reload();
        }
    }

    @Override
    protected void repositionElements() {
        if (this.titleWidget != null) {
            this.titleWidget.setPosition((this.width - this.titleWidget.getWidth()) / 2, 8);
        }
    }

    private void onCheckUpdates() {
        if (this.fetching || this.operationInProgress) return;
        PackUpdateStatus selected = this.getSelectedStatus();
        this.selectedPackIdBeforeRefresh =
                selected == null ? null : selected.remoteEntry().id();
        this.fetching = true;
        this.fetchError = null;
        this.operationError = null;
        this.checkUpdatesButton.active = false;
        this.checkUpdatesButton.setMessage(Component.translatable("banner-recipes.screen.download_packs.checking"));
        this.reload();
        BannerRecipesMod.LOGGER.info("Fetching banner-pack index from {}", Config.REPO_INDEX_URL);
        this.service
                .fetchIndex()
                .whenCompleteAsync(
                        (index, error) -> {
                            this.fetching = false;
                            this.checkUpdatesButton.setMessage(
                                    Component.translatable("banner-recipes.screen.download_packs.check_updates"));
                            this.checkUpdatesButton.active = true;
                            if (error != null) {
                                BannerRecipesMod.LOGGER.error("Failed to fetch banner-pack index", error);
                                this.fetchError = errorMessage(error);
                            } else {
                                this.cachedIndex = index;
                                this.packStatuses = this.service.getPackStatuses(index);
                                this.loadIconsFromData(this.packStatuses);
                            }
                            this.reload();
                        },
                        this.minecraft);
    }

    private void onDownload() {
        PackUpdateStatus selected = this.getSelectedStatus();
        if (selected == null || this.operationInProgress) return;
        PackUpdateStatus.InstallStatus status = selected.status();
        if (status == PackUpdateStatus.InstallStatus.UPDATE_AVAILABLE) {
            this.minecraft.gui.setScreen(new ConfirmScreen(
                    confirmed -> {
                        this.minecraft.gui.setScreen(this);
                        if (confirmed) this.startInstall(selected, true);
                    },
                    Component.translatable(
                            "banner-recipes.screen.download_packs.confirm.update",
                            selected.remoteEntry().displayTitle()),
                    Component.translatable("banner-recipes.screen.download_packs.confirm.update_body")));
        } else if (status == PackUpdateStatus.InstallStatus.NOT_INSTALLED) {
            this.startInstall(selected, false);
        }
    }

    private void startInstall(PackUpdateStatus target, boolean isUpdate) {
        this.operationInProgress = true;
        this.operationError = null;
        this.updateActionButtons(null);
        this.setButtonsEnabled(false);
        BannerRecipesMod.LOGGER.info(
                "Installing banner pack '{}' from {} (update={})",
                target.remoteEntry().id(),
                target.remoteEntry().downloadUrl(),
                isUpdate);
        this.service
                .install(target.remoteEntry(), isUpdate)
                .whenCompleteAsync(
                        (result, error) -> {
                            this.operationInProgress = false;
                            this.setButtonsEnabled(true);
                            if (error != null) {
                                BannerRecipesMod.LOGGER.error(
                                        "Failed to install banner pack {}",
                                        target.remoteEntry().id(),
                                        error);
                                this.operationError = Component.translatable(
                                                "banner-recipes.screen.download_packs.error.download_failed")
                                        .getString();
                            } else if (result == InstallResult.HASH_MISMATCH) {
                                this.operationError = Component.translatable(
                                                "banner-recipes.screen.download_packs.error.hash_mismatch")
                                        .getString();
                            } else if (result == InstallResult.CONFLICT_UNMANAGED) {
                                this.operationError = Component.translatable(
                                                "banner-recipes.screen.download_packs.status.conflict")
                                        .getString();
                            } else if (result == InstallResult.DOWNLOAD_FAILED) {
                                this.operationError = Component.translatable(
                                                "banner-recipes.screen.download_packs.error.download_failed")
                                        .getString();
                            } else {
                                SelectedBannerPacks selectedPacks = SelectedBannerPacks.getInstance();
                                selectedPacks.load();
                                if (this.activateAfterDownload)
                                    selectedPacks.selectPack(
                                            target.remoteEntry().id());
                                this.refreshStatuses();
                            }
                            this.reload();
                        },
                        this.minecraft);
    }

    private void onDelete() {
        PackUpdateStatus selected = this.getSelectedStatus();
        if (selected == null || this.operationInProgress) return;
        this.minecraft.gui.setScreen(new ConfirmScreen(
                confirmed -> {
                    this.minecraft.gui.setScreen(this);
                    if (confirmed) this.deletePack(selected);
                },
                Component.translatable(
                        "banner-recipes.screen.download_packs.confirm.delete",
                        selected.remoteEntry().displayTitle()),
                Component.translatable("banner-recipes.screen.download_packs.confirm.delete_body")));
    }

    private void deletePack(PackUpdateStatus selected) {
        this.operationInProgress = true;
        this.operationError = null;
        this.setButtonsEnabled(false);
        this.service
                .delete(selected.remoteEntry().id())
                .whenCompleteAsync(
                        (ignored, error) -> {
                            this.operationInProgress = false;
                            this.setButtonsEnabled(true);
                            if (error != null) {
                                BannerRecipesMod.LOGGER.error(
                                        "Failed to delete banner pack {}",
                                        selected.remoteEntry().id(),
                                        error);
                                this.operationError = errorMessage(error);
                            } else {
                                this.refreshStatuses();
                            }
                            this.reload();
                        },
                        this.minecraft);
    }

    private PackUpdateStatus getSelectedStatus() {
        if (this.packList == null) return null;
        PackListEntry entry = this.packList.getSelected();
        return entry instanceof PackRowEntry row ? row.status : null;
    }

    private void refreshStatuses() {
        if (this.cachedIndex != null) this.packStatuses = this.service.getPackStatuses(this.cachedIndex);
    }

    private void reload() {
        if (this.packList == null) return;
        this.packList.refresh(this.packStatuses, this.fetchError, this.fetching, this.operationError);
        this.updateActionButtons(this.getSelectedStatus());
    }

    private void onEntrySelected(PackUpdateStatus status) {
        this.updateActionButtons(status);
    }

    private void updateActionButtons(PackUpdateStatus status) {
        if (this.downloadButton == null || this.deleteButton == null) return;
        if (status == null || this.operationInProgress) {
            this.downloadButton.active = false;
            this.deleteButton.active = false;
            return;
        }
        PackUpdateStatus.InstallStatus state = status.status();
        this.downloadButton.active = state == PackUpdateStatus.InstallStatus.NOT_INSTALLED
                || state == PackUpdateStatus.InstallStatus.UPDATE_AVAILABLE;
        this.deleteButton.active = state == PackUpdateStatus.InstallStatus.INSTALLED_UP_TO_DATE
                || state == PackUpdateStatus.InstallStatus.UPDATE_AVAILABLE;
        this.downloadButton.setMessage(Component.translatable(
                state == PackUpdateStatus.InstallStatus.UPDATE_AVAILABLE
                        ? "banner-recipes.screen.download_packs.action.update"
                        : "banner-recipes.screen.download_packs.action.download"));
    }

    private void setButtonsEnabled(boolean enabled) {
        if (this.checkUpdatesButton != null) this.checkUpdatesButton.active = enabled;
        if (this.downloadButton != null) this.downloadButton.active = enabled;
        if (this.deleteButton != null) this.deleteButton.active = enabled;
        if (this.doneButton != null) this.doneButton.active = enabled;
        if (this.activateCheckbox != null) this.activateCheckbox.active = enabled;
    }

    @Override
    public void onClose() {
        if (this.previousScreen instanceof BannerPackSelectionScreen selectionScreen) {
            selectionScreen.refreshFromStorage();
        }
        this.minecraft.gui.setScreen(this.previousScreen);
    }

    private void loadIconsFromData(List<PackUpdateStatus> statuses) {
        for (PackUpdateStatus status : statuses) {
            String packId = status.remoteEntry().id();
            if (this.iconCache.containsKey(packId)) continue;
            String iconData = status.remoteEntry().iconData();
            if (iconData == null || iconData.isBlank()) continue;
            try {
                byte[] bytes = Base64.getDecoder().decode(iconData);
                com.mojang.blaze3d.platform.NativeImage image =
                        com.mojang.blaze3d.platform.NativeImage.read(new ByteArrayInputStream(bytes));
                Identifier location = Identifier.withDefaultNamespace(
                        "pack-download/icon/" + Util.sanitizeName(packId, Identifier::validPathChar));
                this.minecraft.getTextureManager().register(location, new DynamicTexture(location::toString, image));
                this.iconCache.put(packId, location);
            } catch (IOException | IllegalArgumentException e) {
                BannerRecipesMod.LOGGER.debug("Could not load icon for banner pack {}", packId, e);
            }
        }
    }

    private static String errorMessage(Throwable error) {
        Throwable cause = error;
        while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private final class PackListWidget extends ObjectSelectionList<PackListEntry> {
        private PackListWidget(Minecraft minecraft, int width, int height, int x, int y) {
            super(minecraft, width, height, y, ROW_HEIGHT);
            this.centerListVertically = false;
            this.setX(x);
        }

        @Override
        public int getRowWidth() {
            return this.width - 6;
        }

        @Override
        protected int scrollBarX() {
            return this.getRight() - this.scrollbarWidth();
        }

        private void refresh(
                List<PackUpdateStatus> statuses, String fetchError, boolean fetching, String operationError) {
            String previousId = BannerPackDownloadManagementScreen.this.selectedPackIdBeforeRefresh;
            if (previousId == null) {
                PackListEntry previous = this.getSelected();
                if (previous instanceof PackRowEntry row)
                    previousId = row.status.remoteEntry().id();
            }
            this.clearEntries();
            if (operationError != null) {
                this.addEntry(new StatusTextEntry(
                        this.minecraft.font, Component.literal(operationError).withStyle(ChatFormatting.RED)));
            }
            if (fetching) {
                this.addEntry(new StatusTextEntry(
                        this.minecraft.font,
                        Component.translatable("banner-recipes.screen.download_packs.checking")
                                .withStyle(ChatFormatting.GRAY)));
            } else if (fetchError != null) {
                this.addEntry(new StatusTextEntry(
                        this.minecraft.font, Component.literal(fetchError).withStyle(ChatFormatting.RED)));
            } else if (statuses.isEmpty()) {
                this.addEntry(new StatusTextEntry(
                        this.minecraft.font,
                        Component.translatable("banner-recipes.screen.download_packs.no_packs")
                                .withStyle(ChatFormatting.GRAY)));
            } else {
                PackRowEntry toReselect = null;
                for (PackUpdateStatus status : statuses) {
                    PackRowEntry row = new PackRowEntry(BannerPackDownloadManagementScreen.this, status);
                    this.addEntry(row);
                    if (status.remoteEntry().id().equals(previousId)) toReselect = row;
                }
                if (toReselect != null) this.setSelected(toReselect);
            }
            if (!fetching) BannerPackDownloadManagementScreen.this.selectedPackIdBeforeRefresh = null;
        }
    }

    private abstract static class PackListEntry extends ObjectSelectionList.Entry<PackListEntry> {}

    private static final class StatusTextEntry extends PackListEntry {
        private final Font font;
        private final Component text;

        private StatusTextEntry(Font font, Component text) {
            this.font = font;
            this.text = text;
        }

        @Override
        public Component getNarration() {
            return this.text;
        }

        @Override
        public void extractContent(
                GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            graphics.centeredText(
                    this.font,
                    this.text,
                    this.getContentX() + this.getWidth() / 2,
                    this.getContentYMiddle() - 4,
                    0xAAAAAA);
        }
    }

    private static final class PackRowEntry extends PackListEntry {
        private static final int PADDING = 4;
        private final BannerPackDownloadManagementScreen screen;
        private final PackUpdateStatus status;

        private PackRowEntry(BannerPackDownloadManagementScreen screen, PackUpdateStatus status) {
            this.screen = screen;
            this.status = status;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.status.remoteEntry().displayTitle());
        }

        @Override
        public void extractContent(
                GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            boolean selected = this.screen.packList != null && this.screen.packList.getSelected() == this;
            if (hovered || selected) {
                graphics.fill(
                        this.getContentX(),
                        this.getContentY(),
                        this.getContentX() + this.getWidth(),
                        this.getContentY() + this.getContentHeight(),
                        selected ? 0x66FFFFFF : 0x33FFFFFF);
            }
            Identifier icon =
                    this.screen.iconCache.getOrDefault(this.status.remoteEntry().id(), DEFAULT_ICON);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    icon,
                    this.getContentX() + 2,
                    this.getContentY() + 4,
                    0f,
                    0f,
                    32,
                    32,
                    32,
                    32);
            int textX = this.getContentX() + 38;
            graphics.text(
                    this.screen.font,
                    Component.literal(this.status.remoteEntry().displayTitle()),
                    textX,
                    this.getContentY() + PADDING,
                    0xFFFFFFFF,
                    false);
            String author = this.status.remoteEntry().author();
            Component statusLabel = getStatusLabel(this.status.status());
            String secondLine = (author != null && !author.isBlank() ? author + " — " : "") + statusLabel.getString();
            graphics.text(
                    this.screen.font,
                    Component.literal(secondLine),
                    textX,
                    this.getContentY() + PADDING + 14,
                    0xFFAAAAAA,
                    false);
        }

        private static Component getStatusLabel(PackUpdateStatus.InstallStatus status) {
            return switch (status) {
                case NOT_INSTALLED ->
                    Component.translatable("banner-recipes.screen.download_packs.status.not_installed")
                            .withStyle(ChatFormatting.GRAY);
                case INSTALLED_UP_TO_DATE ->
                    Component.translatable("banner-recipes.screen.download_packs.status.installed")
                            .withStyle(ChatFormatting.GREEN);
                case UPDATE_AVAILABLE ->
                    Component.translatable("banner-recipes.screen.download_packs.status.update_available")
                            .withStyle(ChatFormatting.YELLOW);
                case CONFLICT_UNMANAGED ->
                    Component.translatable("banner-recipes.screen.download_packs.status.conflict")
                            .withStyle(ChatFormatting.RED);
            };
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.screen.packList != null) this.screen.packList.setSelected(this);
            this.screen.onEntrySelected(this.status);
            return true;
        }
    }
}
