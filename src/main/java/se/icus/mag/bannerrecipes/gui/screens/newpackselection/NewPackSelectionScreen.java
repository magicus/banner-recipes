/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.screens.newpackselection;

import com.mojang.blaze3d.Blaze3D;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import se.icus.mag.bannerrecipes.storage.BannerPackId;

public class NewPackSelectionScreen extends Screen {
    private static final Component AVAILABLE_TITLE = Component.translatable("pack.available.title");
    private static final Component SELECTED_TITLE = Component.translatable("pack.selected.title");
    private static final Component OPEN_PACK_FOLDER_TITLE = Component.translatable("pack.openFolder");
    private static final Component SEARCH =
            Component.translatable("gui.packSelection.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final Component DRAG_AND_DROP =
            Component.translatable("pack.dropInfo").withStyle(ChatFormatting.GRAY);
    private static final Component DIRECTORY_BUTTON_TOOLTIP = Component.translatable("pack.folderInfo");
    private static final int LIST_WIDTH = 200;
    private static final int LIST_HEIGHT_MARGIN = 66;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final FilesDropManager filesDropManager = new FilesDropManager(this);
    public final PackSelectionManager packSelectionManager = new PackSelectionManager();

    private BannerPackList availablePackList;
    private BannerPackList selectedPackList;
    private EditBox search;

    public NewPackSelectionScreen() {
        super(Component.translatable("banner-recipes.screen.manage.select_packs"));

        this.packSelectionManager.reload();
    }

    @Override
    protected void init() {
        this.layout.setHeaderHeight(49);
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.getTitle(), this.font));
        header.addChild(new StringWidget(DRAG_AND_DROP, this.font));
        this.search = header.addChild(new EditBox(this.font, 0, 0, LIST_WIDTH, 15, Component.empty()));
        this.search.setHint(SEARCH);
        this.search.setResponder(this::updateFilteredEntries);

        this.availablePackList = this.layout.addToContents(new BannerPackList(
                this.minecraft, this, LIST_WIDTH, this.height - LIST_HEIGHT_MARGIN, AVAILABLE_TITLE));
        this.selectedPackList = this.layout.addToContents(
                new BannerPackList(this.minecraft, this, LIST_WIDTH, this.height - LIST_HEIGHT_MARGIN, SELECTED_TITLE));
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));

        footer.addChild(
                Button.builder(OPEN_PACK_FOLDER_TITLE, button -> Blaze3D.openPath(packSelectionManager.getPacksRoot()))
                        .tooltip(Tooltip.create(DIRECTORY_BUTTON_TOOLTIP))
                        .build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .build());

        this.layout.visitWidgets(widget -> this.addRenderableWidget(widget));
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

    @Override
    public void onFilesDrop(List<Path> files) {
        filesDropManager.onFilesDrop(files);
    }

    private void updateFilteredEntries(String value) {
        this.updateFilteredEntries(value, null);
    }

    private void updateFilteredEntries(String value, BannerPackId selectedEntry) {
        this.filterEntries(value, this.packSelectionManager.getSelected(), this.selectedPackList, selectedEntry);
        this.filterEntries(value, this.packSelectionManager.getUnselected(), this.availablePackList, selectedEntry);
    }

    private void filterEntries(
            String value, List<BannerPackId> oldEntries, BannerPackList listToUpdate, BannerPackId selectedEntry) {
        if (listToUpdate != null) {
            List<BannerPackId> filteredEntries = oldEntries.stream()
                    .filter(packId -> packMatchesString(packId, value))
                    .toList();
            listToUpdate.updateList(filteredEntries, selectedEntry);
        }
    }

    private boolean packMatchesString(BannerPackId packId, String value) {
        String lowerCaseValue = value.toLowerCase(Locale.ROOT);
        return value.isBlank()
                || packId.id().toLowerCase(Locale.ROOT).contains(lowerCaseValue)
                || packSelectionManager
                        .getTitle(packId)
                        .toLowerCase(Locale.ROOT)
                        .contains(lowerCaseValue)
                || packSelectionManager
                        .getDescription(packId)
                        .toLowerCase(Locale.ROOT)
                        .contains(lowerCaseValue);
    }

    public void onModelChange(BannerPackId selectedEntry) {
        if (this.selectedPackList != null) {
            this.selectedPackList.updateList(this.packSelectionManager.getSelected(), selectedEntry);
        }

        if (this.availablePackList != null) {
            this.availablePackList.updateList(this.packSelectionManager.getUnselected(), selectedEntry);
        }

        if (this.search != null) {
            this.updateFilteredEntries(this.search.getValue(), selectedEntry);
        }
    }

    public void reload() {
        this.packSelectionManager.reload();
        this.onModelChange(null);
        this.packSelectionManager.packIcons.clear();
    }
}
