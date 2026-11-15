/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A one pixel high horizontal line, which takes part in layouts like any other widget. */
public class SeparatorWidget extends AbstractWidget {
    private final int color;

    public SeparatorWidget(int width, int color) {
        super(0, 0, width, 1, Component.empty());
        this.color = color;
        this.active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
