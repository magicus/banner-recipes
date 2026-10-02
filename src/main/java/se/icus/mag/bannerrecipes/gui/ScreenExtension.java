/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;

public interface ScreenExtension {
    default void init() {}

    default void removed() {}

    default void tick() {}

    default void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}

    default void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    default boolean keyPressed(KeyEvent event) {
        return false;
    }

    default boolean keyReleased(KeyEvent event) {
        return false;
    }

    default boolean charTyped(CharacterEvent event) {
        return false;
    }

    default boolean preeditUpdated(PreeditEvent event) {
        return false;
    }

    default boolean isInputCaptured() {
        return false;
    }

    default boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    default boolean mouseReleased(MouseButtonEvent event) {
        return false;
    }

    default boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }
}
