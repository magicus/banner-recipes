/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;
import se.icus.mag.bannerrecipes.util.Lookup;
import se.icus.mag.bannerrecipes.weaver.survival.LoomController;
import se.icus.mag.bannerrecipes.weaver.survival.LoomInventory;
import se.icus.mag.bannerrecipes.weaver.survival.WeavingPlan;
import se.icus.mag.bannerrecipes.weaver.survival.WeavingSteps;

class SurvivalWeavingPlanTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void weavesSwedishFlagWithAStackOfEveryMaterial() {
        assertCanWeave(swedishFlag(), true, 64, 64);
    }

    @Test
    void weavesSwedishFlagWithExactlyTheRequiredMaterials() {
        assertCanWeave(swedishFlag(), true, 2, 1);
    }

    @Test
    void weavesSwedishFlagWithAnExtraYellowDye() {
        assertCanWeave(swedishFlag(), true, 3, 1);
    }

    @Test
    void cannotWeaveSwedishFlagWithOneYellowDyeMissing() {
        WeavingPlan plan = planFor(swedishFlag(), true, 1, 1);

        assertFalse(plan.missingMaterials().isEmpty());
        assertTrue(plan.missingMaterials().contains(yellowDye()));
    }

    @Test
    void weavesCreeperBannerAndKeepsThePatternRequirement() {
        WeavingPlan plan = planFor(creeperBanner(), true, 1, 1);

        assertTrue(plan.missingMaterials().isEmpty());
        assertTrue(plan.pendingSteps().stream().anyMatch(step -> step instanceof WeavingSteps.PlacePatternItem));
        assertTrue(plan.pendingSteps().peekLast() instanceof WeavingSteps.TakeResult);
    }

    @Test
    void cannotWeaveCreeperBannerWithoutGreenBanner() {
        WeavingPlan plan = planFor(creeperBanner(), false, 1, 1);

        assertTrue(plan.missingMaterials().contains(greenBanner()));
    }

    @Test
    void cannotWeaveCreeperBannerWithoutCreeperPattern() {
        WeavingPlan plan = planFor(creeperBanner(), true, 1, 0);

        assertTrue(plan.missingMaterials().contains(Items.CREEPER_BANNER_PATTERN));
    }

    @Test
    void finalResultStepReturnsTheCreeperPatternToInventory() throws Exception {
        LoomController loom = mock(LoomController.class);
        WeavingSteps.TakeResult takeResult = new WeavingSteps.TakeResult(true);

        takeResult.perform(loom);

        verify(loom).takeResult(true);
    }

    @Test
    void takingFinalResultQuickMovesCreeperPatternBackToInventory() {
        Minecraft minecraft = mock(Minecraft.class);
        MultiPlayerGameMode gameMode = mock(MultiPlayerGameMode.class);
        minecraft.gameMode = gameMode;

        LoomMenu menu = mock(LoomMenu.class);
        Slot resultSlot = mock(Slot.class);
        Slot patternSlot = mock(Slot.class);
        resultSlot.index = 3;
        patternSlot.index = 2;
        when(menu.getResultSlot()).thenReturn(resultSlot);
        when(menu.getPatternSlot()).thenReturn(patternSlot);
        when(patternSlot.getItem()).thenReturn(mock(ItemStack.class));

        new LoomController(menu, minecraft).takeResult(true);

        verify(gameMode)
                .handleContainerInput(
                        eq(menu.containerId), eq(resultSlot.index), eq(0), eq(ContainerInput.QUICK_MOVE), isNull());
        verify(gameMode)
                .handleContainerInput(
                        eq(menu.containerId), eq(patternSlot.index), eq(0), eq(ContainerInput.QUICK_MOVE), isNull());
    }

    private static void assertCanWeave(BannerRecipe recipe, boolean hasBanner, int dyeCount, int patternCount) {
        assertTrue(planFor(recipe, hasBanner, dyeCount, patternCount)
                .missingMaterials()
                .isEmpty());
    }

    private static WeavingPlan planFor(BannerRecipe recipe, boolean hasBanner, int dyeCount, int patternCount) {
        LoomInventory inventory = mock(LoomInventory.class);
        when(inventory.banner()).thenReturn(ItemStack.EMPTY);
        when(inventory.dye()).thenReturn(ItemStack.EMPTY);
        when(inventory.pattern()).thenReturn(ItemStack.EMPTY);
        when(inventory.hasBlankBanner(anyBanner(recipe))).thenReturn(hasBanner);
        when(inventory.availableInputCount(yellowDye())).thenReturn(dyeCount);
        when(inventory.availableInputCount(Lookup.getItemFromDyeColor(DyeColor.BLACK)))
                .thenReturn(dyeCount);
        when(inventory.availableInputCount(Items.CREEPER_BANNER_PATTERN)).thenReturn(patternCount);
        return WeavingPlan.from(recipe, inventory);
    }

    private static Item anyBanner(BannerRecipe recipe) {
        return Lookup.getBannerFromDyeColor(DyeColor.byName(recipe.bannerColor(), DyeColor.WHITE));
    }

    private static Item yellowDye() {
        return Lookup.getItemFromDyeColor(DyeColor.YELLOW);
    }

    private static Item greenBanner() {
        return Lookup.getBannerFromDyeColor(DyeColor.GREEN);
    }

    private static BannerRecipe swedishFlag() {
        return new BannerRecipe(
                "swedish-flag",
                "Swedish flag",
                "test",
                null,
                "flags",
                "blue",
                List.of(
                        BannerRecipeLayer.of("minecraft:stripe_center", "yellow"),
                        BannerRecipeLayer.of("minecraft:stripe_middle", "yellow")));
    }

    private static BannerRecipe creeperBanner() {
        return new BannerRecipe(
                "creeper-banner",
                "Creeper banner",
                "test",
                null,
                "decorative",
                "green",
                List.of(new BannerRecipeLayer(
                        Identifier.fromNamespaceAndPath("minecraft", "creeper"), DyeColor.BLACK)));
    }
}
