/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;

public class DelegatingScreenExtension implements ScreenExtension {
    private List<ScreenExtension> widgets = new ArrayList<>();

    public void addWidget(ScreenExtension widget) {
        this.widgets.add(widget);
    }

    public void removeWidget(ScreenExtension widget) {
        this.widgets.remove(widget);
    }

    @Override
    public void init() {
        for (ScreenExtension widget : widgets) {
            widget.init();
        }
    }

    @Override
    public void removed() {
        for (ScreenExtension widget : widgets) {
            widget.removed();
        }
    }

    @Override
    public void tick() {
        for (ScreenExtension widget : widgets) {
            widget.tick();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        for (ScreenExtension widget : widgets) {
            widget.extractBackground(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (ScreenExtension widget : widgets) {
            widget.extractTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        for (ScreenExtension widget : widgets) {
            if (widget.keyPressed(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        for (ScreenExtension widget : widgets) {
            if (widget.keyReleased(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        for (ScreenExtension widget : widgets) {
            if (widget.charTyped(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean preeditUpdated(PreeditEvent event) {
        for (ScreenExtension widget : widgets) {
            if (widget.preeditUpdated(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isInputCaptured() {
        for (ScreenExtension widget : widgets) {
            if (widget.isInputCaptured()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (ScreenExtension widget : widgets) {
            if (widget.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (ScreenExtension widget : widgets) {
            if (widget.mouseReleased(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        for (ScreenExtension widget : widgets) {
            if (widget.mouseDragged(event, dx, dy)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (ScreenExtension widget : widgets) {
            if (widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true;
            }
        }
        return false;
    }
}
