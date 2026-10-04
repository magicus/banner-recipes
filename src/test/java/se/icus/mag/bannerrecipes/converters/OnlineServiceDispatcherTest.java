/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceException;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceType;
import se.icus.mag.bannerrecipes.converters.url.UrlConverter;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

class OnlineServiceDispatcherTest {
    @Test
    void testServiceMinecraftToolsUrl() throws OnlineServiceException {
        String url =
                "https://minecraft.tools/en/banner.php?color_id_0=0&shape_id_1=28&color_id_1=10&shape_id_2=28&color_id_2=0&shape_id_3=27&color_id_3=2&shape_id_4=28&color_id_4=0&shape_id_5=39&color_id_5=10";
        BannerRecipe recipe = UrlConverter.fromUrl(url);

        assertEquals("black", recipe.bannerColor());
        assertEquals(
                List.of(
                        BannerRecipeLayer.of("minecraft:bricks", "lime"),
                        BannerRecipeLayer.of("minecraft:bricks", "black"),
                        BannerRecipeLayer.of("minecraft:small_stripes", "green"),
                        BannerRecipeLayer.of("minecraft:bricks", "black"),
                        BannerRecipeLayer.of("minecraft:globe", "lime")),
                recipe.layers());
        assertEquals(url, recipe.url());
        assertEquals(url, UrlConverter.toUrl(recipe, OnlineServiceType.MINECRAFT_TOOLS));
    }

    @Test
    void testServiceNeedCoolerShoesUrls() throws OnlineServiceException {
        String directUrl = "https://needcoolershoes.com/banner?=ealleNhEehppai";
        String shortUrl = "https://ncrs.skin/b?=ealleNhEehppai";
        BannerRecipe direct = UrlConverter.fromUrl(directUrl);
        BannerRecipe shortRecipe = UrlConverter.fromUrl(shortUrl);

        assertEquals("blue", direct.bannerColor());
        assertEquals("Imported from NeedCoolerShoes", direct.description());
        assertEquals(
                List.of(
                        BannerRecipeLayer.of("minecraft:stripe_center", "yellow"),
                        BannerRecipeLayer.of("minecraft:globe", "blue"),
                        BannerRecipeLayer.of("minecraft:stripe_top", "light_gray"),
                        BannerRecipeLayer.of("minecraft:triangles_bottom", "blue"),
                        BannerRecipeLayer.of("minecraft:gradient", "white"),
                        BannerRecipeLayer.of("minecraft:curly_border", "black")),
                direct.layers());
        assertEquals(directUrl, direct.url());
        assertEquals(shortUrl, shortRecipe.url());
        assertEquals(direct.layers(), shortRecipe.layers());
        assertEquals(directUrl, UrlConverter.toUrl(direct, OnlineServiceType.NEED_COOLER_SHOES));
    }

    @Test
    void testServicePlanetMinecraftUrl() throws OnlineServiceException {
        String url = "https://www.planetminecraft.com/banner/?e=2c729cmcf28";
        BannerRecipe recipe = UrlConverter.fromUrl(url);

        assertEquals("red", recipe.bannerColor());
        assertEquals(
                List.of(
                        BannerRecipeLayer.of("minecraft:cross", "yellow"),
                        BannerRecipeLayer.of("minecraft:diagonal_left", "red"),
                        BannerRecipeLayer.of("minecraft:square_top_right", "yellow"),
                        BannerRecipeLayer.of("minecraft:mojang", "yellow"),
                        BannerRecipeLayer.of("minecraft:curly_border", "red")),
                recipe.layers());
        assertEquals(url, recipe.url());
        assertEquals(url, UrlConverter.toUrl(recipe, OnlineServiceType.PLANET_MINECRAFT));
    }

    @Test
    void testServiceSkinMcUrl() throws OnlineServiceException {
        String url = "https://skinmc.net/banner/editor?=eaaBazmoptaiaA";
        BannerRecipe recipe = UrlConverter.fromUrl(url);

        assertEquals("blue", recipe.bannerColor());
        assertEquals(
                List.of(
                        BannerRecipeLayer.of("minecraft:small_stripes", "black"),
                        BannerRecipeLayer.of("minecraft:straight_cross", "black"),
                        BannerRecipeLayer.of("minecraft:flower", "light_blue"),
                        BannerRecipeLayer.of("minecraft:circle", "white"),
                        BannerRecipeLayer.of("minecraft:curly_border", "black"),
                        BannerRecipeLayer.of("minecraft:skull", "black")),
                recipe.layers());
        assertEquals(url, recipe.url());
        assertEquals(url, UrlConverter.toUrl(recipe, OnlineServiceType.SKIN_MC));
    }

    @Test
    void ignoresMinecraftToolsUrlFragment() throws OnlineServiceException {
        String withFragment =
                "https://minecraft.tools/en/banner.php?color_id_0=2&shape_id_1=11&color_id_1=15&shape_id_2=13&color_id_2=2&shape_id_3=22&color_id_3=0#crafting";
        String withoutFragment =
                "https://minecraft.tools/en/banner.php?color_id_0=2&shape_id_1=11&color_id_1=15&shape_id_2=13&color_id_2=2&shape_id_3=22&color_id_3=0";

        BannerRecipe withFragmentRecipe = UrlConverter.fromUrl(withFragment);
        BannerRecipe withoutFragmentRecipe = UrlConverter.fromUrl(withoutFragment);

        assertEquals(withoutFragmentRecipe.bannerColor(), withFragmentRecipe.bannerColor());
        assertEquals(withoutFragmentRecipe.layers(), withFragmentRecipe.layers());
    }

    @Test
    void rejectsWrongLinkType() {
        OnlineServiceException.WrongLinkTypeException planetMinecraft = assertThrows(
                OnlineServiceException.WrongLinkTypeException.class,
                () -> UrlConverter.fromUrl("https://www.planetminecraft.com/banner/union-sovietic-flag/"));
        OnlineServiceException.WrongLinkTypeException needCoolerShoes = assertThrows(
                OnlineServiceException.WrongLinkTypeException.class,
                () -> UrlConverter.fromUrl("https://needcoolershoes.com/banners/8961/~ghost"));
        OnlineServiceException.WrongLinkTypeException needCoolerShoesShort = assertThrows(
                OnlineServiceException.WrongLinkTypeException.class,
                () -> UrlConverter.fromUrl("https://ncrs.skin/b/11525/~my-banner"));
        OnlineServiceException.WrongLinkTypeException skinMc = assertThrows(
                OnlineServiceException.WrongLinkTypeException.class,
                () -> UrlConverter.fromUrl("https://skinmc.net/banner/18d1d0c1-08fd-46a6-abfd-1766ebc47f26"));

        assertEquals("Please use \"Remix Banner\" link instead", planetMinecraft.getMessage());
        assertEquals("Please use \"Open in Editor\" link instead", needCoolerShoes.getMessage());
        assertEquals("Please use \"Open in Editor\" link instead", needCoolerShoesShort.getMessage());
        assertEquals("Please Use \"Edit design\" link instead", skinMc.getMessage());
    }

    @Test
    void rejectsUnsupportedUrls() {
        assertThrows(
                OnlineServiceException.UnsupportedUrlException.class,
                () -> UrlConverter.fromUrl("https://unknown.com/banner?code=xyz"));
    }

    @Test
    void exportsAndImportsAllServices() throws OnlineServiceException {
        BannerRecipe recipe = new BannerRecipe(
                "id",
                "Test",
                null,
                null,
                "misc",
                "blue",
                List.of(new BannerRecipeLayer(
                        Identifier.fromNamespaceAndPath("minecraft", "stripe_center"), DyeColor.YELLOW)));

        for (OnlineServiceType service : OnlineServiceType.values()) {
            String url = UrlConverter.toUrl(recipe, service);
            assertEquals(recipe.bannerColor(), UrlConverter.fromUrl(url).bannerColor());
            assertEquals(recipe.layers(), UrlConverter.fromUrl(url).layers());
        }
    }
}
