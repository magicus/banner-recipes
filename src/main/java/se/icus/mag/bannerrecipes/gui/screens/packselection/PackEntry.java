/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.screens.packselection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import se.icus.mag.bannerrecipes.storage.AvailableBannerPacks;

public final class PackEntry extends BannerPackListEntry implements SelectableEntry {
    private static final Identifier UNSELECT_HIGHLIGHTED_SPRITE =
            Identifier.withDefaultNamespace("transferable_list/unselect_highlighted");
    private static final Identifier SELECT_HIGHLIGHTED_SPRITE =
            Identifier.withDefaultNamespace("transferable_list/select_highlighted");
    private static final int ICON_SIZE = 32;

    private final BannerPackSelectionScreen screen;
    private final BannerPackListWidget parent;
    private final BannerPackModelEntry pack;
    private final StringWidget nameWidget;
    private final MultiLineTextWidget secondLineWidget;

    public PackEntry(
            Minecraft minecraft,
            BannerPackSelectionScreen screen,
            BannerPackListWidget parent,
            BannerPackModelEntry pack,
            boolean selected) {
        this.screen = screen;
        this.parent = parent;
        this.pack = pack.withSelected(selected);
        this.nameWidget = new StringWidget(this.pack.getTitle(), minecraft.font);
        this.secondLineWidget = new MultiLineTextWidget(
                ComponentUtils.mergeStyles(this.pack.secondLine(), Style.EMPTY.withColor(-8355712)), minecraft.font);
        this.secondLineWidget.setMaxRows(2);
    }

    @Override
    public Component getNarration() {
        return Component.translatable("narrator.select", this.pack.getTitle());
    }

    @Override
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
        int iconY = this.getContentY();
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.pack.iconTexture(),
                this.getContentX(),
                iconY,
                0f,
                0f,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE);
        if (this.showHoverOverlay() && hovered) {
            Identifier actionSprite = this.pack.selected() ? UNSELECT_HIGHLIGHTED_SPRITE : SELECT_HIGHLIGHTED_SPRITE;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, actionSprite, this.getContentX(), iconY, 32, 32);
        }

        if (!this.nameWidget.getMessage().equals(this.pack.getTitle()))
            this.nameWidget.setMessage(this.pack.getTitle());
        if (!this.secondLineWidget
                .getMessage()
                .getContents()
                .equals(this.pack.secondLine().getContents())) {
            this.secondLineWidget.setMessage(
                    ComponentUtils.mergeStyles(this.pack.secondLine(), Style.EMPTY.withColor(-8355712)));
        }

        int textX = this.getContentX() + ICON_SIZE + 2;
        this.nameWidget.setMaxWidth(157);
        this.nameWidget.setPosition(textX, this.getContentY() + 1);
        this.nameWidget.extractRenderState(graphics, mouseX, mouseY, delta);
        this.secondLineWidget.setMaxWidth(157);
        this.secondLineWidget.setPosition(textX, this.getContentY() + 12);
        this.secondLineWidget.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.showHoverOverlay() && this.mouseOverIcon((int) event.x(), (int) event.y())) {
            this.togglePack();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation() && this.showHoverOverlay()) {
            this.togglePack();
            return true;
        }
        return super.keyPressed(event);
    }

    private boolean showHoverOverlay() {
        return !AvailableBannerPacks.LOCAL_PACK_ID.equals(this.pack.packId());
    }

    private boolean mouseOverIcon(int mouseX, int mouseY) {
        int relativeX = mouseX - this.getContentX();
        int relativeY = mouseY - this.getContentY();
        return relativeX >= 0 && relativeX < ICON_SIZE && relativeY >= 0 && relativeY < ICON_SIZE;
    }

    private void togglePack() {
        if (this.pack.selected()) {
            this.screen.movePackToAvailable(this.pack.packId());
        } else {
            this.screen.movePackToSelected(this.pack.packId());
        }
    }

    @Override
    public String getPackId() {
        return this.pack.packId();
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return this.parent.children().stream()
                .anyMatch(entry -> entry.getPackId().equals(this.getPackId()));
    }
}
