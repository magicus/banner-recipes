/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

public abstract class OnlineService {
    private final OnlineServiceType serviceType;

    protected OnlineService(OnlineServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public abstract String getServiceName();

    public final OnlineServiceType getServiceType() {
        return serviceType;
    }

    public abstract boolean supports(URI uri) throws PageLinkException;

    public final BannerRecipe fromUrl(URI uri) {
        BannerImportData data = extractBannerRecipeFromUrl(uri);

        return new BannerRecipe(
                "url_import",
                "Imported from " + getServiceName(),
                null,
                uri.toString(),
                "misc",
                data.bannerColor().getName(),
                data.layers());
    }

    public final String toUrl(BannerRecipe recipe) {
        Map<String, Object> parameters = extractUrlParametersFromBannerRecipe(recipe);

        return UrlQuery.withParameters(baseUrl(), parameters);
    }

    protected abstract BannerImportData extractBannerRecipeFromUrl(URI uri);

    protected abstract Map<String, Object> extractUrlParametersFromBannerRecipe(BannerRecipe recipe);

    protected abstract String baseUrl();

    protected static String host(URI uri) {
        return uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
    }

    protected static boolean hostIs(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }

    protected abstract List<Identifier> patterns();

    protected final Identifier getPatternIdFromIndex(int index) {
        if (index < 0 || index >= patterns().size()) return null;
        return patterns().get(index);
    }

    protected final int getPatternIndex(Identifier pattern) {
        return patterns().indexOf(pattern);
    }

    protected List<Identifier> identifiers(Iterable<String> names) {
        List<Identifier> result = new ArrayList<>();
        for (String name : names) {
            result.add(Identifier.fromNamespaceAndPath("minecraft", name));
        }
        return List.copyOf(result);
    }

    public record BannerImportData(DyeColor bannerColor, List<BannerRecipeLayer> layers) {}

    public static class PageLinkException extends Exception {
        public PageLinkException(String message) {
            super(message);
        }
    }
}
