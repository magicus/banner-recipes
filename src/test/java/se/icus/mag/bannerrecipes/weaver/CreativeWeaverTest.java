/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

class CreativeWeaverTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void weavesSwedishFlagIntoInventory() {
        Minecraft minecraft = minecraftWithInventory(mock(Inventory.class));
        BannerRecipe swedishFlag = swedishFlagRecipe();
        ItemStack expectedBanner = mock(ItemStack.class);

        try (MockedStatic<BannerRecipesMod> bannerRecipes = mockStatic(BannerRecipesMod.class);
                MockedStatic<Minecraft> minecraftInstance = mockStatic(Minecraft.class)) {
            bannerRecipes.when(() -> BannerRecipesMod.getItemStack(swedishFlag)).thenReturn(expectedBanner);
            minecraftInstance.when(Minecraft::getInstance).thenReturn(minecraft);

            CreativeWeaver weaver = new CreativeWeaver();
            weaver.weave(swedishFlag);

            verify(minecraft.player.getInventory()).add(expectedBanner);
            assertEquals(2, swedishFlag.layers().size());
        }
    }

    @Test
    void reportsWhatHappensWhenInventoryIsFullOfWoodenShovels() {
        Inventory inventory = mock(Inventory.class);
        ItemStack woodenShovel = mock(ItemStack.class);
        when(woodenShovel.getItem()).thenReturn(Items.WOODEN_SHOVEL);
        when(inventory.getContainerSize()).thenReturn(36);
        when(inventory.getItem(any(Integer.class))).thenReturn(woodenShovel);
        when(inventory.add(any(ItemStack.class))).thenReturn(false);
        Minecraft minecraft = minecraftWithInventory(inventory);
        BannerRecipe swedishFlag = swedishFlagRecipe();
        ItemStack expectedBanner = mock(ItemStack.class);

        try (MockedStatic<BannerRecipesMod> bannerRecipes = mockStatic(BannerRecipesMod.class);
                MockedStatic<Minecraft> minecraftInstance = mockStatic(Minecraft.class)) {
            bannerRecipes.when(() -> BannerRecipesMod.getItemStack(swedishFlag)).thenReturn(expectedBanner);
            minecraftInstance.when(Minecraft::getInstance).thenReturn(minecraft);

            new CreativeWeaver().weave(swedishFlag);

            verify(inventory).add(expectedBanner);
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                assertEquals(Items.WOODEN_SHOVEL, inventory.getItem(slot).getItem());
            }
        }
    }

    private static Minecraft minecraftWithInventory(Inventory inventory) {
        Minecraft minecraft = mock(Minecraft.class);
        LocalPlayer player = mock(LocalPlayer.class);
        when(player.getInventory()).thenReturn(inventory);
        minecraft.player = player;
        minecraft.gameMode = mock(MultiPlayerGameMode.class);
        return minecraft;
    }

    private static BannerRecipe swedishFlagRecipe() {
        return new BannerRecipe(
                "swedish-flag",
                "Swedish flag",
                "test",
                null,
                "flags",
                "blue",
                java.util.List.of(
                        BannerRecipeLayer.of("minecraft:stripe_middle", "yellow"),
                        BannerRecipeLayer.of("minecraft:stripe_center", "yellow")));
    }
}
