/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.widgets.panel;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import se.icus.mag.bannerrecipes.BannerRecipesMod;

public class RecipePanelSearch {
    private static final Component SEARCH_HINT =
            Component.translatable("gui.recipebook.search_hint").withStyle(EditBox.SEARCH_HINT_STYLE);

    private EditBox searchBox;
    private String lastSearchText;
    private ScreenRectangle magnifierIconBox;
    private boolean ignoreNextTypedChar;

    public void init(int leftPos, int topPos) {
        searchBox = new EditBox(
                Minecraft.getInstance().font,
                leftPos + 25,
                topPos + 13,
                81,
                9 + 5,
                Component.translatable("itemGroup.search"));
        searchBox.setMaxLength(50);
        searchBox.setVisible(true);
        searchBox.setTextColor(-1);
        searchBox.setHint(SEARCH_HINT);

        magnifierIconBox = ScreenRectangle.of(
                ScreenAxis.HORIZONTAL,
                leftPos + 8,
                searchBox.getY(),
                searchBox.getX() - leftPos,
                searchBox.getHeight());

        lastSearchText = "";
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        searchBox.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    public boolean keyPressed(KeyEvent event) {
        ignoreNextTypedChar = false;

        if (searchBox.keyPressed(event)) {
            updateSearchText();
            return true;
        }

        if (searchBox.capturesInput() && !event.isEscape()) {
            return true;
        }

        if (Minecraft.getInstance().options.keyChat.matches(event) && !searchBox.isFocused()) {
            ignoreNextTypedChar = true;
            searchBox.setFocused(true);
            return true;
        }

        return false;
    }

    public boolean keyReleased() {
        ignoreNextTypedChar = false;

        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        if (ignoreNextTypedChar) return false;

        if (searchBox.charTyped(event)) {
            updateSearchText();
            return true;
        }

        return false;
    }

    private void updateSearchText() {
        String searchText = searchBox.getValue().toLowerCase(Locale.ROOT);
        if (!searchText.equals(lastSearchText)) {
            lastSearchText = searchText;

            BannerRecipesMod.getManager().getPanelView().setSearchText(searchText);
        }
    }

    public boolean preeditUpdated(PreeditEvent event) {
        if (ignoreNextTypedChar) return false;

        if (searchBox.preeditUpdated(event)) {
            return true;
        }

        return false;
    }

    public boolean capturesInput() {
        return searchBox.capturesInput();
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (searchBox.mouseClicked(event, doubleClick)
                || magnifierIconBox.containsPoint(Mth.floor(event.x()), Mth.floor(event.y()))) {
            searchBox.setFocused(true);
            return true;
        }

        searchBox.setFocused(false);
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!searchBox.isFocused()) return false;

        return searchBox.mouseDragged(event, dx, dy);
    }
}
