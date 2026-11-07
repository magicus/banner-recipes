/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.screens.newpackselection;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import se.icus.mag.bannerrecipes.storage.BannerPackId;

public class BannerPackList extends ObjectSelectionList<BannerPackList.BannerPackListEntry> {
    private static final WidgetSprites SELECTION_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("transferable_list/unselect"),
            Identifier.withDefaultNamespace("transferable_list/select"),
            Identifier.withDefaultNamespace("transferable_list/unselect_highlighted"),
            Identifier.withDefaultNamespace("transferable_list/select_highlighted"));

    private static final int ICON_SIZE = 32;
    private static final int SELECTED_COLOR = 0xA0909090;
    private static final int DESCRIPTION_TEXT_COLOR = 0xff808080;
    private static final int MAX_DESCRIPTION_WIDTH_PIXELS = 157;

    private final HeaderEntry headerEntry;
    private final NewPackSelectionScreen screen;
    private final PackSelectionManager packSelectionManager;
    private List<BannerPackId> entries;

    public BannerPackList(Minecraft minecraft, NewPackSelectionScreen screen, int width, int height, Component title) {
        super(minecraft, width, height, 33, 36);
        this.screen = screen;
        this.packSelectionManager = screen.packSelectionManager;
        headerEntry = new HeaderEntry(
                this.minecraft.font,
                Component.empty().append(title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD),
                this);
        entries = List.of();

        this.centerListVertically = false;
    }

    @Override
    public int getRowWidth() {
        return this.width - 4;
    }

    @Override
    protected int scrollBarX() {
        return this.getRight() - this.scrollbarWidth();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.getSelected() != null) {
            return this.getSelected().keyPressed(event);
        }

        return super.keyPressed(event);
    }

    public void updateList(List<BannerPackId> entries, BannerPackId selectedEntry) {
        this.clearEntries();

        this.addEntry(headerEntry, (int) 13.5f);
        this.setSelected(null);

        this.entries = entries;

        entries.forEach(e -> {
            BannerPackEntry entry = new BannerPackEntry(this.minecraft, this.packSelectionManager, this, e);
            this.addEntry(entry);
            if (selectedEntry != null && selectedEntry.equals(e)) {
                this.screen.setFocused(this);
                this.setFocused(entry);
            }
        });
        this.refreshScrollAmount();
    }

    private void extractContentEntry(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            boolean hovered,
            float a,
            BannerPackEntry bannerPackEntry) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                packSelectionManager.getIconTexture(bannerPackEntry.getPackId()),
                bannerPackEntry.getContentX(),
                bannerPackEntry.getContentY(),
                0.0F,
                0.0F,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE);

        if (hovered || (getSelected() == bannerPackEntry && isFocused())) {
            graphics.fill(
                    bannerPackEntry.getContentX(),
                    bannerPackEntry.getContentY(),
                    bannerPackEntry.getContentX() + ICON_SIZE,
                    bannerPackEntry.getContentY() + ICON_SIZE,
                    SELECTED_COLOR);
            int relX = mouseX - bannerPackEntry.getContentX();
            int relY = mouseY - bannerPackEntry.getContentY();

            boolean iconHovered = bannerPackEntry.mouseOverIcon(relX, relY, ICON_SIZE);
            Identifier sprite =
                    SELECTION_SPRITES.get(packSelectionManager.isSelected(bannerPackEntry.getPackId()), iconHovered);
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    sprite,
                    bannerPackEntry.getContentX(),
                    bannerPackEntry.getContentY(),
                    ICON_SIZE,
                    ICON_SIZE);

            if (iconHovered) {
                handleCursor(graphics);
            }
        }

        bannerPackEntry.nameWidget.setMaxWidth(MAX_DESCRIPTION_WIDTH_PIXELS - (scrollable() ? 6 : 0));
        bannerPackEntry.nameWidget.setPosition(
                bannerPackEntry.getContentX() + ICON_SIZE + 2, bannerPackEntry.getContentY() + 1);
        bannerPackEntry.nameWidget.extractRenderState(graphics, mouseX, mouseY, a);

        bannerPackEntry.descriptionWidget.setMaxWidth(MAX_DESCRIPTION_WIDTH_PIXELS - (scrollable() ? 6 : 0));
        bannerPackEntry.descriptionWidget.setPosition(
                bannerPackEntry.getContentX() + ICON_SIZE + 2, bannerPackEntry.getContentY() + 12);
        bannerPackEntry.descriptionWidget.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private boolean mouseClickedEntry(
            MouseButtonEvent event, boolean doubleClick, BannerPackList.BannerPackEntry bannerPackEntry) {
        int relX = (int) event.x() - bannerPackEntry.getContentX();
        int relY = (int) event.y() - bannerPackEntry.getContentY();

        if (packSelectionManager.canSelect(bannerPackEntry.getPackId())
                && bannerPackEntry.mouseOverIcon(relX, relY, ICON_SIZE)) {
            packSelectionManager.select(bannerPackEntry.getPackId());
            return true;
        }

        if (packSelectionManager.canUnselect(bannerPackEntry.getPackId())
                && bannerPackEntry.mouseOverLeftHalf(relX, relY, ICON_SIZE)) {
            packSelectionManager.unselect(bannerPackEntry.getPackId());
            return true;
        }

        return false;
    }

    private boolean keyPressedEntry(KeyEvent event, BannerPackEntry bannerPackEntry) {
        if (event.isConfirmation()) {
            if (packSelectionManager.canSelect(bannerPackEntry.getPackId())) {
                packSelectionManager.select(bannerPackEntry.getPackId());
            } else if (packSelectionManager.canUnselect(bannerPackEntry.getPackId())) {
                packSelectionManager.unselect(bannerPackEntry.getPackId());
            }
            return true;
        }
        return false;
    }

    boolean shouldTakeFocusAfterInteractionEntry(BannerPackEntry bannerPackEntry) {
        return entries.contains(bannerPackEntry.getPackId());
    }

    public abstract static class BannerPackListEntry extends ObjectSelectionList.Entry<BannerPackListEntry> {
        protected final BannerPackList parent;

        protected BannerPackListEntry(BannerPackList parent) {
            this.parent = parent;
        }

        @Override
        public int getWidth() {
            return super.getWidth() - (parent.scrollable() ? parent.scrollbarWidth() : 0);
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }
    }

    private static final class HeaderEntry extends BannerPackListEntry {
        private final Font font;
        private final Component text;

        private HeaderEntry(Font font, Component text, BannerPackList parent) {
            super(parent);
            this.font = font;
            this.text = text;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            graphics.centeredText(
                    this.font, this.text, this.getX() + this.getWidth() / 2, this.getContentYMiddle() - 9 / 2, -1);
        }
    }

    private static final class BannerPackEntry extends BannerPackListEntry implements SelectableEntry {
        private final BannerPackId bannerPackId;

        private final StringWidget nameWidget;
        private final MultiLineTextWidget descriptionWidget;

        private BannerPackEntry(
                Minecraft minecraft,
                PackSelectionManager packSelectionManager,
                BannerPackList parent,
                BannerPackId bannerPackId) {
            super(parent);

            this.bannerPackId = bannerPackId;

            this.nameWidget =
                    new StringWidget(Component.literal(packSelectionManager.getTitle(bannerPackId)), minecraft.font);
            this.descriptionWidget = new MultiLineTextWidget(
                    ComponentUtils.mergeStyles(
                            Component.literal(packSelectionManager.getDescription(bannerPackId)),
                            Style.EMPTY.withColor(DESCRIPTION_TEXT_COLOR)),
                    minecraft.font);
            this.descriptionWidget.setMaxRows(2);
        }

        public BannerPackId getPackId() {
            return bannerPackId;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            parent.extractContentEntry(graphics, mouseX, mouseY, hovered, a, this);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (parent.mouseClickedEntry(event, doubleClick, this)) {
                return true;
            }

            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (parent.keyPressedEntry(event, this)) {
                return true;
            }

            return super.keyPressed(event);
        }

        @Override
        public boolean shouldTakeFocusAfterInteraction() {
            return parent.shouldTakeFocusAfterInteractionEntry(this);
        }
    }
}
