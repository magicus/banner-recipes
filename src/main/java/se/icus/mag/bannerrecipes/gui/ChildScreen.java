/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui;

import net.minecraft.client.gui.screens.Screen;

/**
 * A screen opened on top of a parent screen, which is shown again when the child is closed.
 *
 * <p>While a child is shown, its parent is not {@link Screen#removed() removed}, so the parent keeps its state
 * (and, for container screens, its menu contents). The parent is removed when the child is replaced by anything
 * but the parent.
 */
public interface ChildScreen {
    /** The parent screen, or {@code null} if the child was opened from the game itself. */
    Screen getParentScreen();
}
