/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeCategory;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

public class BannerRecipesMod implements ClientModInitializer {
    public static final String MOD_ID = "banner-recipes";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final BannerRecipesManager MANAGER = new BannerRecipesManager();

    public static Item getBannerFromDyeColor(DyeColor bannerColorEnum) {
        return switch (bannerColorEnum) {
            case WHITE -> Items.BANNER.white();
            case ORANGE -> Items.BANNER.orange();
            case MAGENTA -> Items.BANNER.magenta();
            case LIGHT_BLUE -> Items.BANNER.lightBlue();
            case YELLOW -> Items.BANNER.yellow();
            case LIME -> Items.BANNER.lime();
            case PINK -> Items.BANNER.pink();
            case GRAY -> Items.BANNER.gray();
            case LIGHT_GRAY -> Items.BANNER.lightGray();
            case CYAN -> Items.BANNER.cyan();
            case PURPLE -> Items.BANNER.purple();
            case BLUE -> Items.BANNER.blue();
            case BROWN -> Items.BANNER.brown();
            case GREEN -> Items.BANNER.green();
            case RED -> Items.BANNER.red();
            case BLACK -> Items.BANNER.black();
        };
    }

    public static ItemStack getItemStack(BannerRecipe recipe) {
        Registry<BannerPattern> registry = getBannerPatternRegistry(Minecraft.getInstance());
        Item baseBannerItem = getBannerFromDyeColor(DyeColor.byName(recipe.bannerColor(), DyeColor.WHITE));
        ItemStack stack = new ItemStack(baseBannerItem);

        if (recipe.layers().isEmpty()) return stack;

        try {
            BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
            for (BannerRecipeLayer layer : recipe.layers()) {
                try {
                    Identifier patternId = layer.pattern();

                    Optional<Holder.Reference<BannerPattern>> entry = registry.get(patternId);
                    if (entry.isEmpty()) {
                        LOGGER.debug("Pattern not found in registry: {}", patternId);
                        continue;
                    }

                    builder.add(entry.get(), layer.color());
                } catch (RuntimeException e) {
                    LOGGER.debug("Error processing banner pattern: {}", layer.pattern(), e);
                }
            }
            stack.set(DataComponents.BANNER_PATTERNS, builder.build());
        } catch (RuntimeException e) {
            LOGGER.debug("Error creating banner patterns component", e);
        }

        return stack;
    }

    public static ItemStack getItemStack(BannerRecipeCategory category) {
        String iconItemId = category.iconItemId();
        if (iconItemId != null && !iconItemId.isBlank()) {
            try {
                RegistryAccess registryAccess =
                        Minecraft.getInstance().getConnection().registryAccess();
                ItemParser itemParser = new ItemParser(registryAccess);

                return itemParser.parse(new StringReader(iconItemId)).createItemStack(1);
            } catch (RuntimeException | CommandSyntaxException e) {
                LOGGER.warn("Failed to parse category icon as vanilla item input: {}", iconItemId, e);
            }
        }
        return new ItemStack(Items.AIR);
    }

    @Override
    public void onInitializeClient() {
        BannerRecipesMod.LOGGER.info("Initializing BannerRecipesMod");
    }

    public static BannerRecipesManager getManager() {
        return MANAGER;
    }

    public static void recipeSelected(BannerRecipe recipe, boolean autoCraft) {}

    public static Registry<BannerPattern> getBannerPatternRegistry(Minecraft mc) {
        Registry<BannerPattern> registry =
                mc.level.registryAccess().lookup(Registries.BANNER_PATTERN).orElse(null);
        if (registry == null) {
            LOGGER.error("BannerPattern registry is unavailable — this should never happen");
            throw new IllegalStateException("BannerPattern registry is unavailable");
        }
        return registry;
    }

    public static boolean isCraftable(BannerRecipe bannerRecipe) {
        // FIXME: for testing, only black banners are craftable.
        return bannerRecipe.bannerColor().equals("black");
    }

    public static boolean matchesSearch(BannerRecipe recipe, String searchTarget) {
        return recipe.description().toLowerCase(Locale.ROOT).contains(searchTarget.toLowerCase(Locale.ROOT));
    }
}
