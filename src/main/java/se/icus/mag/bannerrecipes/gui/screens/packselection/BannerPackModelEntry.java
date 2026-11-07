/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.screens.packselection;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import se.icus.mag.bannerrecipes.storage.BannerPackId;

public record BannerPackModelEntry(
        BannerPackId packId,
        String title,
        Component description,
        Component secondLine,
        Identifier iconTexture,
        boolean selected) {
    public BannerPackModelEntry withSelected(boolean selected) {
        return new BannerPackModelEntry(
                this.packId, this.title, this.description, this.secondLine, this.iconTexture, selected);
    }

    public Component getTitle() {
        return Component.literal(this.title);
    }
}
