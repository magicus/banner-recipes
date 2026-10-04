/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import se.icus.mag.bannerrecipes.converters.url.services.MinecraftToolsOnlineService;
import se.icus.mag.bannerrecipes.converters.url.services.NeedCoolerShoesOnlineService;
import se.icus.mag.bannerrecipes.converters.url.services.PlanetMinecraftOnlineService;
import se.icus.mag.bannerrecipes.converters.url.services.SkinMcOnlineService;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

public final class UrlConverter {
    private static final Map<OnlineServiceType, OnlineService> CONVERTERS = Map.of(
            OnlineServiceType.PLANET_MINECRAFT,
            new PlanetMinecraftOnlineService(),
            OnlineServiceType.SKIN_MC,
            new SkinMcOnlineService(),
            OnlineServiceType.MINECRAFT_TOOLS,
            new MinecraftToolsOnlineService(),
            OnlineServiceType.NEED_COOLER_SHOES,
            new NeedCoolerShoesOnlineService());

    public static OnlineServiceType getServiceTypeForUrl(URI uri) {
        return CONVERTERS.values().stream()
                .filter(service -> service.supports(uri))
                .findFirst()
                .map(OnlineService::getServiceType)
                .orElse(null);
    }

    public static BannerRecipe fromUrl(String url, OnlineServiceType service) throws OnlineServiceException {
        if (url == null || url.isBlank()) {
            throw new OnlineServiceException.UrlParsingException("URL cannot be empty");
        }

        OnlineService converter = CONVERTERS.get(service);

        try {
            URI uri = new URI(url.trim());
            if (!converter.supports(uri)) {
                throw new OnlineServiceException.UnsupportedUrlException(
                        "Unsupported " + converter.getServiceName() + " URL: " + url);
            }
            return converter.fromUrl(uri);
        } catch (URISyntaxException e) {
            throw new OnlineServiceException.UrlParsingException("Invalid URL: " + url);
        }
    }

    public static BannerRecipe fromUrl(String url) throws OnlineServiceException {
        OnlineServiceType serviceType = getServiceTypeForUrl(URI.create(url));
        if (serviceType == null) {
            throw new OnlineServiceException.UnsupportedUrlException("Unsupported URL: " + url);
        }
        return fromUrl(url, serviceType);
    }

    public static String toUrl(BannerRecipe recipe, OnlineServiceType serviceType) throws OnlineServiceException {
        return CONVERTERS.get(serviceType).toUrl(recipe);
    }
}
