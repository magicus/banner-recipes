/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver.survival;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;
import se.icus.mag.bannerrecipes.util.Lookup;

public final class WeavingPlan {
    private final Deque<WeavingStep> pendingSteps;
    private final List<Item> missingMaterials;

    private WeavingPlan(Deque<WeavingStep> pendingSteps, List<Item> missingMaterials) {
        this.pendingSteps = pendingSteps;
        this.missingMaterials = List.copyOf(missingMaterials);
    }

    public static WeavingPlan from(BannerRecipe recipe, LoomInventory inventory) {
        Deque<WeavingStep> steps = new ArrayDeque<>();
        List<Item> missing = new ArrayList<>();

        DyeColor bannerColor = DyeColor.byName(recipe.bannerColor(), DyeColor.WHITE);
        Item bannerItem = Lookup.getBannerFromDyeColor(bannerColor);
        steps.addLast(new WeavingSteps.PlaceBanner(bannerItem));
        if (inventory.banner().isEmpty() && !inventory.hasBlankBanner(bannerItem)) {
            missing.add(bannerItem);
        }

        Map<Item, Integer> remainingDyes = new HashMap<>();
        Set<Item> checkedPatternItems = new HashSet<>();

        for (int index = 0; index < recipe.layers().size(); index++) {
            BannerRecipeLayer layer = recipe.layers().get(index);
            boolean finalLayer = (index == recipe.layers().size() - 1);

            Item dyeItem = Lookup.getItemFromDyeColor(layer.color());
            Item patternItem = Lookup.getItemFromPattern(layer.pattern());

            addLayerSteps(steps, layer, dyeItem, patternItem, finalLayer);
            addMissingDye(missing, remainingDyes, inventory, dyeItem);
            addMissingPatternItem(missing, checkedPatternItems, inventory, patternItem);
        }

        return new WeavingPlan(steps, missing);
    }

    public Deque<WeavingStep> pendingSteps() {
        return pendingSteps;
    }

    public List<Item> missingMaterials() {
        return missingMaterials;
    }

    private static void addLayerSteps(
            Deque<WeavingStep> steps, BannerRecipeLayer layer, Item dyeItem, Item patternItem, boolean finalLayer) {
        steps.addLast(new WeavingSteps.PlaceDye(dyeItem, layer.color().getName()));
        if (patternItem == null) {
            steps.addLast(new WeavingSteps.RemovePatternItem());
        } else {
            steps.addLast(new WeavingSteps.PlacePatternItem(patternItem));
        }
        steps.addLast(new WeavingSteps.SelectPattern(layer.pattern()));
        steps.addLast(new WeavingSteps.WaitForResult());
        steps.addLast(new WeavingSteps.TakeResult(finalLayer));
    }

    private static void addMissingDye(
            List<Item> missing, Map<Item, Integer> remainingDyes, LoomInventory inventory, Item dyeItem) {
        int remaining = remainingDyes.computeIfAbsent(dyeItem, inventory::availableInputCount);
        if (remaining > 0) {
            remainingDyes.put(dyeItem, remaining - 1);
        } else {
            missing.add(dyeItem);
        }
    }

    private static void addMissingPatternItem(
            List<Item> missing, Set<Item> checkedPatternItems, LoomInventory inventory, Item patternItem) {
        if (patternItem != null
                && checkedPatternItems.add(patternItem)
                && inventory.availableInputCount(patternItem) == 0) {
            missing.add(patternItem);
        }
    }
}
