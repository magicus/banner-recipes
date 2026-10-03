/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumMap;
import java.util.Map;
import se.icus.mag.bannerrecipes.converters.url.services.MinecraftToolsOnlineService;
import se.icus.mag.bannerrecipes.converters.url.services.NeedCoolerShoesOnlineService;
import se.icus.mag.bannerrecipes.converters.url.services.PlanetMinecraftOnlineService;
import se.icus.mag.bannerrecipes.converters.url.services.SkinMcOnlineService;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

public final class UrlConverter {
    private static final Map<OnlineServiceType, OnlineService> CONVERTERS = createConverters();

    public static String toUrl(BannerRecipe recipe, OnlineServiceType serviceType) {
        return converter(serviceType).toUrl(recipe);
    }

    public static BannerRecipe fromUrl(String url) throws OnlineService.PageLinkException {
        URI uri = parseUrl(url);

        OnlineService converter = null;
        for (OnlineService candidate : CONVERTERS.values()) {
            if (candidate.supports(uri)) {
                converter = candidate;
                break;
            }
        }
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported banner URL: " + url);
        }
        return converter.fromUrl(uri);
    }

    public static BannerRecipe fromUrl(String url, OnlineServiceType service) throws OnlineService.PageLinkException {
        OnlineService converter = converter(service);
        URI uri = parseUrl(url);
        if (!converter.supports(uri)) {
            throw new IllegalArgumentException("Unsupported " + service + " URL: " + url);
        }
        return converter.fromUrl(uri);
    }

    private static URI parseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        try {
            return new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    private static OnlineService converter(OnlineServiceType service) {
        return CONVERTERS.get(service);
    }

    private static Map<OnlineServiceType, OnlineService> createConverters() {
        Map<OnlineServiceType, OnlineService> converters = new EnumMap<>(OnlineServiceType.class);
        converters.put(OnlineServiceType.PLANET_MINECRAFT, new PlanetMinecraftOnlineService());
        converters.put(OnlineServiceType.SKIN_MC, new SkinMcOnlineService());
        converters.put(OnlineServiceType.MINECRAFT_TOOLS, new MinecraftToolsOnlineService());
        converters.put(OnlineServiceType.NEED_COOLER_SHOES, new NeedCoolerShoesOnlineService());
        return Map.copyOf(converters);
    }
}
