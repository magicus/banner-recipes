/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class DelegatingScreenExtension implements ScreenExtension {
    private List<ScreenExtension> widgets;

    public DelegatingScreenExtension(List<ScreenExtension> widgets) {
        this.widgets = widgets;
    }

    public void init() {
        for (ScreenExtension widget : widgets) {
            widget.init();
        }
    }

    public void removed() {
        for (ScreenExtension widget : widgets) {
            widget.removed();
        }
    }

    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        for (ScreenExtension widget : widgets) {
            widget.extractBackground(context, mouseX, mouseY, delta);
        }
    }

    public boolean keyPressed(KeyEvent event) {
        for (ScreenExtension widget : widgets) {
            if (widget.keyPressed(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        for (ScreenExtension widget : widgets) {
            if (widget.charTyped(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        for (ScreenExtension widget : widgets) {
            if (widget.mouseClicked(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        for (ScreenExtension widget : widgets) {
            if (widget.mouseReleased(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        for (ScreenExtension widget : widgets) {
            if (widget.mouseDragged(event, dx, dy)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (ScreenExtension widget : widgets) {
            if (widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true;
            }
        }
        return false;
    }
}
