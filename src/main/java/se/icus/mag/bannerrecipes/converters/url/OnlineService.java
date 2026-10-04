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
import se.icus.mag.bannerrecipes.util.UrlQuery;

public abstract class OnlineService {
    private final OnlineServiceType serviceType;

    protected OnlineService(OnlineServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public abstract String getServiceName();

    public final OnlineServiceType getServiceType() {
        return serviceType;
    }

    public abstract boolean supports(URI uri);

    public final BannerRecipe fromUrl(URI uri) throws OnlineServiceException {
        checkLinkType(uri);

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

    public final String toUrl(BannerRecipe recipe) throws OnlineServiceException {
        Map<String, Object> parameters = extractUrlParametersFromBannerRecipe(recipe);

        return UrlQuery.withParameters(baseUrl(), parameters);
    }

    protected abstract BannerImportData extractBannerRecipeFromUrl(URI uri) throws OnlineServiceException;

    protected abstract Map<String, Object> extractUrlParametersFromBannerRecipe(BannerRecipe recipe)
            throws OnlineServiceException;

    protected abstract void checkLinkType(URI uri) throws OnlineServiceException.WrongLinkTypeException;

    protected abstract String baseUrl();

    protected boolean checkUrlFormat(URI uri, String domain, String path) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

        if (!(host.equals(domain) || host.endsWith("." + domain))) return false;
        if (!uri.getPath().startsWith(path)) return false;

        return true;
    }

    protected abstract List<Identifier> patterns();

    protected final Identifier getPatternIdFromIndex(int index) {
        if (index < 0 || index >= patterns().size()) return null;
        return patterns().get(index);
    }

    protected final int getPatternIndex(Identifier pattern) {
        return patterns().indexOf(pattern);
    }

    protected int providerIndex(DyeColor color) {
        return 15 - color.getId();
    }

    protected DyeColor fromProviderIndex(int index) throws OnlineServiceException.UrlParsingException {
        if (index < 0 || index > 15)
            throw new OnlineServiceException.UrlParsingException("Invalid color index: " + index);
        return DyeColor.byId(15 - index);
    }

    protected DyeColor fromName(String name) throws OnlineServiceException.UrlParsingException {
        DyeColor color = DyeColor.byName(name, null);
        if (color == null) throw new OnlineServiceException.UrlParsingException("Invalid dye color: " + name);
        return color;
    }

    protected List<Identifier> identifiers(Iterable<String> names) {
        List<Identifier> result = new ArrayList<>();
        for (String name : names) {
            result.add(Identifier.fromNamespaceAndPath("minecraft", name));
        }
        return List.copyOf(result);
    }

    public record BannerImportData(DyeColor bannerColor, List<BannerRecipeLayer> layers) {}
}
