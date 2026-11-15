/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.screens;

import com.mojang.blaze3d.Blaze3D;
import java.net.URI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.converters.RecipeConverter;
import se.icus.mag.bannerrecipes.converters.RecipeConverter.ParseResult;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceType;
import se.icus.mag.bannerrecipes.gui.ChildScreen;
import se.icus.mag.bannerrecipes.gui.widgets.RecipePreviewSlot;
import se.icus.mag.bannerrecipes.gui.widgets.SeparatorWidget;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.util.BannerUtils;
import se.icus.mag.bannerrecipes.util.TextUtil;

public class ManageBannerRecipesScreen extends Screen implements ChildScreen {
    private static final int MAX_PANEL_WIDTH = 360;
    private static final int SCREEN_MARGIN = 6;
    private static final int PANEL_PADDING = 6;
    private static final int INPUT_HEIGHT = 18;
    private static final int STATUS_HEIGHT = 9;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_GAP = 4;
    private static final int LINE_GAP = 2;
    private static final int SECTION_GAP = 8;
    private static final int IMPORT_BUTTON_WIDTH = 88;
    private static final int SMALL_BUTTON_WIDTH = 72;
    private static final int DONE_BUTTON_WIDTH = 72;
    private static final int DONE_TOP_GAP = 16;

    private static final int BACKGROUND_COLOR = 0xFFC6C6C6;
    private static final int BACKGROUND_DARK_COLOR = 0xFF555555;
    private static final int BACKGROUND_LIGHT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR = 0xFF000000;
    private static final int DISABLED_TEXT_COLOR = 0xFF888888;
    private static final int OK_COLOR = 0xFF2E7D32;
    private static final int ERROR_COLOR = 0xFFB00020;

    // Remember choice across session. Null means a /give command.
    private static OnlineServiceType selectedExportService = null;

    private final Screen previousScreen;

    private EditBox importBox;
    private EditBox exportBox;
    private Button importButton;
    private Button copyButton;
    private Button openButton;
    private StringWidget exportServiceLabel;
    private FrameLayout exportServiceLabelFrame;
    private StringWidget importStatus;
    private StringWidget exportHint;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private ParseResult importResult = new ParseResult(null, null);

    public ManageBannerRecipesScreen(Screen previousScreen) {
        super(Component.translatable("banner-recipes.screen.manage.title"));
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        int panelWidth =
                Math.min(Math.max(290, Math.min(MAX_PANEL_WIDTH, this.width - SCREEN_MARGIN * 2)), this.width - 2);
        int contentWidth = panelWidth - PANEL_PADDING * 2;
        int halfButtonWidth = (contentWidth - BUTTON_GAP) / 2;
        int smallButtonWidth = Math.clamp(contentWidth / 4, 64, SMALL_BUTTON_WIDTH);
        int importButtonWidth = Math.clamp(contentWidth / 3, 72, IMPORT_BUTTON_WIDTH);

        // Layout
        LinearLayout root = LinearLayout.vertical().spacing(SECTION_GAP);

        // Import section
        LinearLayout importSection = root.addChild(LinearLayout.vertical().spacing(LINE_GAP));
        importSection.addChild(label(Component.translatable("banner-recipes.screen.manage.label.import")));
        this.importBox =
                importSection.addChild(new EditBox(this.font, 0, 0, contentWidth, INPUT_HEIGHT, Component.empty()));
        this.importBox.setMaxLength(2048);
        this.importBox.setResponder(this::onImportTextChanged);
        this.importStatus =
                importSection.addChild(new StringWidget(contentWidth, STATUS_HEIGHT, Component.empty(), this.font));

        LinearLayout importActions =
                importSection.addChild(LinearLayout.horizontal().spacing(BUTTON_GAP));
        importActions.addChild(new RecipePreviewSlot(this.font, () -> importResult.recipe()));
        importActions.addChild(button("paste", smallButtonWidth, b -> pasteFromClipboard()));
        int fillWidth = contentWidth - RecipePreviewSlot.SIZE - smallButtonWidth - importButtonWidth - BUTTON_GAP * 3;
        importActions.addChild(SpacerElement.width(Math.max(0, fillWidth)));
        this.importButton = importActions.addChild(button("import", importButtonWidth, b -> doImport()));
        this.importButton.active = false;

        root.addChild(new SeparatorWidget(contentWidth, BACKGROUND_DARK_COLOR));

        // Export section
        LinearLayout exportSection = root.addChild(LinearLayout.vertical().spacing(LINE_GAP));
        LinearLayout exportHeader =
                exportSection.addChild(LinearLayout.horizontal().spacing(BUTTON_GAP));
        exportHeader.addChild(label(Component.translatable("banner-recipes.screen.manage.label.export")));
        this.exportHint = exportHeader.addChild(new StringWidget(
                TextUtil.colored(
                        Component.translatable("banner-recipes.screen.manage.select_banner_to_export"),
                        DISABLED_TEXT_COLOR),
                this.font));
        this.exportBox =
                exportSection.addChild(new EditBox(this.font, 0, 0, contentWidth, INPUT_HEIGHT, Component.empty()));
        this.exportBox.setMaxLength(2048);
        this.exportBox.active = false;

        int serviceLabelWidth = smallButtonWidth * 2 + BUTTON_GAP - BUTTON_HEIGHT * 2 - BUTTON_GAP * 2;
        LinearLayout serviceSelector = exportSection.addChild(
                LinearLayout.horizontal().spacing(BUTTON_GAP),
                settings -> settings.paddingLeft(RecipePreviewSlot.SIZE + BUTTON_GAP));
        serviceSelector.addChild(button(Component.literal("<"), BUTTON_HEIGHT, b -> cycleExportService(-1)));
        this.exportServiceLabelFrame = serviceSelector.addChild(new FrameLayout(serviceLabelWidth, BUTTON_HEIGHT));
        this.exportServiceLabel = this.exportServiceLabelFrame.addChild(new StringWidget(Component.empty(), this.font));
        serviceSelector.addChild(button(Component.literal(">"), BUTTON_HEIGHT, b -> cycleExportService(1)));

        LinearLayout exportActions =
                exportSection.addChild(LinearLayout.horizontal().spacing(BUTTON_GAP));
        exportActions.addChild(new RecipePreviewSlot(this.font, ManageBannerRecipesScreen::activeRecipe));
        this.copyButton = exportActions.addChild(button("copy", smallButtonWidth, b -> copyToClipboard()));
        this.openButton = exportActions.addChild(button("open", smallButtonWidth, b -> openExportedUrl()));

        root.addChild(new SeparatorWidget(contentWidth, BACKGROUND_DARK_COLOR));

        // Manage section
        LinearLayout manageSection = root.addChild(LinearLayout.vertical().spacing(LINE_GAP));
        manageSection.addChild(label(Component.translatable("banner-recipes.screen.manage.label.manage")));
        LinearLayout manageButtons =
                manageSection.addChild(LinearLayout.horizontal().spacing(BUTTON_GAP));
        manageButtons.addChild(button("select_packs", halfButtonWidth, b -> openBannerPackManager()));
        manageButtons.addChild(button("download_packs", halfButtonWidth, b -> openPackDownloadManager()));

        // Done button
        root.addChild(
                button(CommonComponents.GUI_DONE, DONE_BUTTON_WIDTH, b -> this.onClose()),
                settings -> settings.alignHorizontallyCenter().paddingTop(DONE_TOP_GAP - SECTION_GAP));

        // Position and register widgets
        root.arrangeElements();
        FrameLayout.centerInRectangle(root, 0, 0, this.width, this.height);
        root.visitWidgets(this::addRenderableWidget);

        this.panelX = root.getX() - PANEL_PADDING;
        this.panelY = root.getY() - PANEL_PADDING;
        this.panelWidth = root.getWidth() + PANEL_PADDING * 2;
        this.panelHeight = root.getHeight() + PANEL_PADDING * 2;

        this.setInitialFocus(this.importBox);
        updateExportWidgets();
    }

    private static Button button(String key, int width, Button.OnPress onPress) {
        return button(Component.translatable("banner-recipes.screen.manage.button." + key), width, onPress);
    }

    private static Button button(Component message, int width, Button.OnPress onPress) {
        return Button.builder(message, onPress).size(width, BUTTON_HEIGHT).build();
    }

    private StringWidget label(Component text) {
        return new StringWidget(TextUtil.colored(text, TEXT_COLOR), this.font);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawPanel(graphics, panelX, panelY, panelWidth, panelHeight);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, BACKGROUND_COLOR);
        graphics.fill(x, y, x + width, y + 1, BACKGROUND_LIGHT_COLOR);
        graphics.fill(x, y, x + 1, y + height, BACKGROUND_LIGHT_COLOR);
        graphics.fill(x, y + height - 1, x + width, y + height, BACKGROUND_DARK_COLOR);
        graphics.fill(x + width - 1, y, x + width, y + height, BACKGROUND_DARK_COLOR);
    }

    private void onImportTextChanged(String text) {
        importResult = RecipeConverter.parse(text);
        importButton.active = importResult.recipe() != null;
        updateImportStatus();
    }

    private void updateImportStatus() {
        if (importResult.recipe() != null) {
            importStatus.setMessage(
                    TextUtil.colored(Component.translatable("banner-recipes.screen.manage.parse_ok"), OK_COLOR));
        } else if (importResult.error() != null) {
            importStatus.setMessage(TextUtil.colored(importResult.error(), ERROR_COLOR));
        } else {
            importStatus.setMessage(Component.empty());
        }
    }

    private void cycleExportService(int delta) {
        OnlineServiceType[] services = OnlineServiceType.values();
        int count = services.length + 1;
        int index = selectedExportService == null ? 0 : selectedExportService.ordinal() + 1;
        index = (index + delta + count) % count;
        selectedExportService = index == 0 ? null : services[index - 1];
        updateExportWidgets();
    }

    private static Component exportServiceName() {
        return selectedExportService == null
                ? Component.translatable("banner-recipes.banner.convert.export_format.command")
                : selectedExportService.displayName();
    }

    private void updateExportWidgets() {
        BannerRecipe recipe = activeRecipe();
        String text = RecipeConverter.export(recipe, selectedExportService);
        exportBox.setValue(text == null ? "" : text);

        exportServiceLabel.setMessage(TextUtil.colored(exportServiceName(), TEXT_COLOR));
        exportServiceLabelFrame.arrangeElements();

        exportHint.visible = recipe == null;
        copyButton.active = text != null;
        openButton.active = text != null && selectedExportService != null;
    }

    private static BannerRecipe activeRecipe() {
        return BannerRecipesMod.getManager().getActiveRecipe();
    }

    private void pasteFromClipboard() {
        if (this.minecraft == null) return;

        String clipboard = this.minecraft.keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isBlank()) return;

        importBox.setValue(clipboard.trim());
        importBox.setCursorPosition(importBox.getValue().length());
    }

    private void doImport() {
        BannerRecipe recipe = importResult.recipe();
        if (recipe == null || this.minecraft == null) return;

        // TODO: let the user name/categorize the banner and store it.
        BannerRecipesMod.LOGGER.info(
                "Import not yet implemented for: {}",
                BannerUtils.displayName(recipe).getString());
    }

    private void copyToClipboard() {
        if (this.minecraft == null) return;

        this.minecraft.keyboardHandler.setClipboard(exportBox.getValue());
    }

    private void openExportedUrl() {
        String url = exportBox.getValue();
        if (url.isBlank()) return;

        Blaze3D.openUri(URI.create(url));
    }

    private void openBannerPackManager() {
        // TODO: open banner pack selection screen.
    }

    private void openPackDownloadManager() {
        // TODO: open banner pack download screen.
    }

    @Override
    public Screen getParentScreen() {
        return previousScreen;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(previousScreen);
    }
}
