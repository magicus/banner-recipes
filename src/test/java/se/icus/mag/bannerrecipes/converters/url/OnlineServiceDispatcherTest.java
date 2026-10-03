/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

class OnlineServiceDispatcherTest {
    @Test
    void importsKnownSkinMcUrl() throws OnlineService.PageLinkException {
        String url = "https://skinmc.net/banner/editor?=paalpwpEac";
        BannerRecipe recipe = UrlConverter.fromUrl(url);

        assertEquals("white", recipe.bannerColor());
        assertEquals(4, recipe.layers().size());
        assertEquals(url, recipe.url());
    }

    @Test
    void importsKnownMinecraftToolsUrl() throws OnlineService.PageLinkException {
        String url = "https://minecraft.tools/en/banner.php?color_id_0=4&shape_id_1=28&color_id_1=15";
        BannerRecipe recipe = UrlConverter.fromUrl(url);

        assertEquals("blue", recipe.bannerColor());
        assertEquals(
                List.of(new BannerRecipeLayer(Identifier.fromNamespaceAndPath("minecraft", "bricks"), DyeColor.WHITE)),
                recipe.layers());
        assertEquals(url, recipe.url());
    }

    @Test
    void importsKnownPlanetMinecraftUrl() throws OnlineService.PageLinkException {
        String url = "https://www.planetminecraft.com/banner/?e=2c729cmcf28";
        BannerRecipe recipe = UrlConverter.fromUrl(url);

        assertEquals("red", recipe.bannerColor());
        assertEquals(5, recipe.layers().size());
        assertEquals(url, recipe.url());
    }

    @Test
    void importsKnownNeedCoolerShoesUrls() throws OnlineService.PageLinkException {
        String directUrl = "https://needcoolershoes.com/banner?=ealleNhEehppai";
        String shortUrl = "https://ncrs.skin/b?=ealleNhEehppai";
        BannerRecipe direct = UrlConverter.fromUrl(directUrl);
        BannerRecipe shortRecipe = UrlConverter.fromUrl(shortUrl);

        assertEquals("blue", direct.bannerColor());
        assertEquals("Imported from NeedCoolerShoes", direct.description());
        assertEquals(directUrl, direct.url());
        assertEquals(shortUrl, shortRecipe.url());
        assertEquals(direct.layers(), shortRecipe.layers());
    }

    @Test
    void rejectsUnsupportedUrls() {
        assertThrows(IllegalArgumentException.class, () -> UrlConverter.fromUrl("https://unknown.com/banner?code=xyz"));
    }

    @Test
    void rejectsProviderPageLinksThatDoNotContainEditorCodes() {
        OnlineService.PageLinkException planetMinecraft = assertThrows(
                OnlineService.PageLinkException.class,
                () -> UrlConverter.fromUrl("https://www.planetminecraft.com/banner/union-sovietic-flag/"));
        OnlineService.PageLinkException needCoolerShoes = assertThrows(
                OnlineService.PageLinkException.class,
                () -> UrlConverter.fromUrl("https://needcoolershoes.com/banners/8961/~ghost"));
        OnlineService.PageLinkException skinMc = assertThrows(
                OnlineService.PageLinkException.class,
                () -> UrlConverter.fromUrl("https://skinmc.net/banner/18d1d0c1-08fd-46a6-abfd-1766ebc47f26"));

        assertEquals("Please use \"Remix Banner\" link instead", planetMinecraft.getMessage());
        assertEquals("Please use \"Open in Editor\" link instead", needCoolerShoes.getMessage());
        assertEquals("Please Use \"Edit design\" link instead", skinMc.getMessage());
    }

    @Test
    void exportsAndImportsAllServices() throws OnlineService.PageLinkException {
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
