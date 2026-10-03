/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

public final class WeavingGuide {
    private final LoomMenu menu;
    private BannerRecipe activeRecipe;
    private ItemStack lastBannerSlotItem;
    private int currentProgress = -1;

    public WeavingGuide(LoomMenu menu) {
        this.menu = menu;
        lastBannerSlotItem = menu.getBannerSlot().getItem();
        updateProgress(lastBannerSlotItem);
    }

    public void updateActiveRecipe(BannerRecipe recipe) {
        this.activeRecipe = recipe;
        updateProgress(menu.getBannerSlot().getItem());
    }

    public int currentProgress() {
        return currentProgress;
    }

    public void tick() {
        ItemStack currentBannerSlotItem = menu.getBannerSlot().getItem();
        if (currentBannerSlotItem == lastBannerSlotItem) return;

        lastBannerSlotItem = currentBannerSlotItem;

        updateProgress(currentBannerSlotItem);
    }

    private void updateProgress(ItemStack currentBannerSlotItem) {
        if (activeRecipe == null
                || currentBannerSlotItem.isEmpty()
                || !(currentBannerSlotItem.getItem() instanceof BannerItem bannerItem)) {
            currentProgress = -1;
            return;
        }

        DyeColor baseColor = DyeColor.byName(activeRecipe.bannerColor(), DyeColor.WHITE);
        if (bannerItem.getColor() != baseColor) {
            currentProgress = -1;
            return;
        }

        BannerPatternLayers patterns = currentBannerSlotItem.get(DataComponents.BANNER_PATTERNS);
        List<BannerPatternLayers.Layer> currentLayers = patterns == null ? List.of() : patterns.layers();
        List<BannerRecipeLayer> recipeLayers = activeRecipe.layers();
        if (currentLayers.size() >= recipeLayers.size()) {
            currentProgress = -1;
            return;
        }

        for (int i = 0; i < currentLayers.size(); i++) {
            BannerPatternLayers.Layer current = currentLayers.get(i);
            BannerRecipeLayer expected = recipeLayers.get(i);
            if (current.color() != expected.color()) {
                currentProgress = -1;
                return;
            }

            String currentId = current.pattern()
                    .unwrapKey()
                    .map(key -> key.identifier().toString())
                    .orElse(null);
            if (currentId == null) {
                currentProgress = -1;
                return;
            }

            String expectedId = expected.pattern().toString();
            if (!currentId.equals(expectedId)) {
                currentProgress = -1;
                return;
            }
        }

        currentProgress = currentLayers.size();
    }
}
