/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.ItemStack;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.weaver.survival.LoomController;
import se.icus.mag.bannerrecipes.weaver.survival.LoomInventory;
import se.icus.mag.bannerrecipes.weaver.survival.WeavingExecutor;
import se.icus.mag.bannerrecipes.weaver.survival.WeavingPlan;

public class SurvivalWeaver implements Weaver {
    private static final int TICK_DELAY = 3;

    private final LoomInventory inventory;
    private final LoomController loomController;

    private WeavingExecutor executor;
    private int ticksUntilNextStep;

    public SurvivalWeaver(LoomMenu menu) {
        this.inventory = new LoomInventory(menu);
        this.loomController = new LoomController(menu, Minecraft.getInstance());
    }

    @Override
    public void weave(BannerRecipe recipe) {
        WeavingPlan plan = WeavingPlan.from(recipe, inventory);

        if (!plan.missingMaterials().isEmpty()) {
            BannerRecipesMod.LOGGER.warn("Auto-weave validation error: missing materials for recipe {}", recipe.id());
            return;
        }

        executor = new WeavingExecutor(plan, loomController);
        ticksUntilNextStep = TICK_DELAY;
        BannerRecipesMod.LOGGER.info(
                "Starting auto-weave for banner with {} layers", recipe.layers().size());
    }

    @Override
    public boolean isCurrentlyWeaving() {
        return executor != null && executor.isActive();
    }

    @Override
    public void tick() {
        if (!isCurrentlyWeaving()) return;

        ticksUntilNextStep--;
        if (ticksUntilNextStep > 0) return;

        ticksUntilNextStep = TICK_DELAY;
        executor.nextStep();
    }

    @Override
    public boolean canWeave(BannerRecipe banner) {
        WeavingPlan plan = WeavingPlan.from(banner, inventory);
        return plan.missingMaterials().isEmpty();
    }

    @Override
    public List<String> getMissingMaterialDescriptions(BannerRecipe banner) {
        WeavingPlan plan = WeavingPlan.from(banner, inventory);
        return plan.missingMaterials().stream()
                .map(missingItem -> new ItemStack(missingItem).getHoverName().getString())
                .toList();
    }
}
