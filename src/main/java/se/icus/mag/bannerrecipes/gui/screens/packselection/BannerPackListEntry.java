/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.screens.packselection;

import net.minecraft.client.gui.components.ObjectSelectionList;
import se.icus.mag.bannerrecipes.storage.BannerPackId;

public abstract class BannerPackListEntry extends ObjectSelectionList.Entry<BannerPackListEntry> {
    protected abstract BannerPackId getPackId();
}
