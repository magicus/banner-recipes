/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;
import se.icus.mag.bannerrecipes.util.Lookup;

class SurvivalWeaverTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                .build(VanillaRegistries.createWorldLookup())
                .forEach(pending -> pending.apply());
    }

    @Test
    void actuallyWeavesSwedishFlagThroughRealLoomMenu() {
        BannerRecipe recipe = swedishFlag();
        TestLoom testLoom = new TestLoom();
        testLoom.addMaterial(0, Lookup.getBannerFromDyeColor(DyeColor.BLUE), 1);
        testLoom.addMaterial(1, Lookup.getItemFromDyeColor(DyeColor.YELLOW), 2);

        weaveToCompletion(testLoom, recipe);

        ItemStack result = findBanner(testLoom.inventory, Lookup.getBannerFromDyeColor(DyeColor.BLUE));
        assertNotNull(result);
        assertEquals(
                2,
                result.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers()
                        .size(),
                () -> result.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .toString());
    }

    @Test
    void actuallyWeavesCreeperBannerAndReturnsPatternItem() {
        BannerRecipe recipe = creeperBanner();
        TestLoom testLoom = new TestLoom();
        testLoom.addMaterial(0, Lookup.getBannerFromDyeColor(DyeColor.GREEN), 1);
        testLoom.addMaterial(1, Lookup.getItemFromDyeColor(DyeColor.BLACK), 1);
        testLoom.addMaterial(2, Items.CREEPER_BANNER_PATTERN, 1);

        weaveToCompletion(testLoom, recipe);

        ItemStack result = findBanner(testLoom.inventory, Lookup.getBannerFromDyeColor(DyeColor.GREEN));
        assertNotNull(result);
        assertEquals(
                Identifier.fromNamespaceAndPath("minecraft", "creeper"),
                result.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers()
                        .getFirst()
                        .pattern()
                        .unwrapKey()
                        .orElseThrow()
                        .identifier());
        assertTrue(hasItem(testLoom.inventory, Items.CREEPER_BANNER_PATTERN));
    }

    private static void weaveToCompletion(TestLoom testLoom, BannerRecipe recipe) {
        try (MockedStatic<Minecraft> minecraftInstance = mockStatic(Minecraft.class)) {
            minecraftInstance.when(Minecraft::getInstance).thenReturn(testLoom.minecraft);
            SurvivalWeaver weaver = new SurvivalWeaver(testLoom.menu);
            assertTrue(weaver.canWeave(recipe));
            weaver.weave(recipe);

            for (int tick = 0; tick < 300 && weaver.isCurrentlyWeaving(); tick++) {
                weaver.tick();
            }

            assertFalse(weaver.isCurrentlyWeaving());
        }
    }

    private static ItemStack findBanner(Inventory inventory, Item banner) {
        ItemStack blank = null;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(banner) && !stack.isEmpty()) {
                if (!stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers()
                        .isEmpty()) return stack;
                blank = stack;
            }
        }
        return blank;
    }

    private static boolean hasItem(Inventory inventory, Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(item)) return true;
        }
        return false;
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

    private static final class TestLoom {
        private final LocalPlayer player;
        private final Inventory inventory;
        private final LoomMenu menu;
        private final Minecraft minecraft;
        private final MultiPlayerGameMode gameMode;
        private final HolderLookup.RegistryLookup<BannerPattern> patternLookup;

        private TestLoom() {
            player = mock(LocalPlayer.class);
            Level level = mock(Level.class);
            when(player.level()).thenReturn(level);
            when(level.enabledFeatures()).thenReturn(FeatureFlagSet.of());
            RegistryAccess registryAccess = mock(RegistryAccess.class);
            when(player.registryAccess()).thenReturn(registryAccess);
            patternLookup = VanillaRegistries.createWorldLookup().lookupOrThrow(Registries.BANNER_PATTERN);
            Registry<BannerPattern> bannerPatterns = mock(Registry.class);
            List<Holder<BannerPattern>> selectablePatterns = List.of(
                    patternLookup.getOrThrow(ResourceKey.create(
                            Registries.BANNER_PATTERN, Identifier.fromNamespaceAndPath("minecraft", "stripe_center"))),
                    patternLookup.getOrThrow(ResourceKey.create(
                            Registries.BANNER_PATTERN, Identifier.fromNamespaceAndPath("minecraft", "stripe_middle"))),
                    patternLookup.getOrThrow(ResourceKey.create(
                            Registries.BANNER_PATTERN, Identifier.fromNamespaceAndPath("minecraft", "creeper"))));
            HolderSet.Named<BannerPattern> selectablePatternTag = mock(HolderSet.Named.class);
            when(selectablePatternTag.iterator()).thenAnswer(invocation -> selectablePatterns.iterator());
            doAnswer(invocation -> Optional.of(selectablePatternTag))
                    .when(bannerPatterns)
                    .get(any(TagKey.class));
            when(registryAccess.lookupOrThrow(Registries.BANNER_PATTERN)).thenReturn(bannerPatterns);
            inventory = new Inventory(player, new EntityEquipment());
            menu = new LoomMenu(1, inventory);
            player.containerMenu = menu;
            minecraft = mock(Minecraft.class);
            gameMode = mock(MultiPlayerGameMode.class);
            minecraft.player = player;
            minecraft.gameMode = gameMode;
            doAnswer(invocation -> {
                        handleContainerInput(
                                invocation.getArgument(1), invocation.getArgument(2), invocation.getArgument(3));
                        return null;
                    })
                    .when(gameMode)
                    .handleContainerInput(anyInt(), anyInt(), anyInt(), any(ContainerInput.class), any());
            doAnswer(invocation -> {
                        menu.clickMenuButton(player, invocation.getArgument(1));
                        return null;
                    })
                    .when(gameMode)
                    .handleInventoryButtonClick(anyInt(), anyInt());
        }

        private ItemStack carried = ItemStack.EMPTY;

        private void handleContainerInput(int slotIndex, int button, ContainerInput input) {
            if (input == ContainerInput.QUICK_MOVE) {
                ItemStack moving = menu.getSlot(slotIndex).getItem();
                menu.getSlot(slotIndex).set(ItemStack.EMPTY);
                if (!moving.isEmpty()) inventory.add(moving);
                return;
            }

            if (input != ContainerInput.PICKUP) return;

            if (carried.isEmpty()) {
                carried = menu.getSlot(slotIndex).remove(button == 0 ? Integer.MAX_VALUE : 1);
            } else if (button == 1) {
                menu.getSlot(slotIndex).set(carried.copyWithCount(1));
                carried.shrink(1);
            } else {
                menu.getSlot(slotIndex).set(carried);
                carried = ItemStack.EMPTY;
            }
        }

        private void addMaterial(int slot, Item item, int count) {
            ItemStack stack = new ItemStack(item, count);
            if (item == Items.CREEPER_BANNER_PATTERN) {
                Holder<BannerPattern> creeper = patternLookup.getOrThrow(ResourceKey.create(
                        Registries.BANNER_PATTERN, Identifier.fromNamespaceAndPath("minecraft", "creeper")));
                stack.set(DataComponents.PROVIDES_BANNER_PATTERNS, HolderSet.direct(creeper));
            }
            inventory.setItem(slot, stack);
        }
    }
}
