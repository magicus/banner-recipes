/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.bannerrecipes.converters.url.OnlineService;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceException;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceType;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;
import se.icus.mag.bannerrecipes.util.UrlQuery;

public final class PlanetMinecraftOnlineService extends OnlineService {
    private static final String ALPHABET = "123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0_-";

    private static final List<String> PATTERNS = List.of(
            "border",
            "bricks",
            "circle",
            "creeper",
            "cross",
            "curly_border",
            "diagonal_left",
            "diagonal_right",
            "flower",
            "gradient",
            "half_horizontal",
            "half_vertical",
            "mojang",
            "rhombus",
            "skull",
            "small_stripes",
            "square_bottom_left",
            "square_bottom_right",
            "square_top_left",
            "square_top_right",
            "straight_cross",
            "stripe_bottom",
            "stripe_center",
            "stripe_downleft",
            "stripe_downright",
            "stripe_left",
            "stripe_middle",
            "stripe_right",
            "stripe_top",
            "triangles_bottom",
            "triangles_top",
            "triangle_bottom",
            "triangle_top",
            "diagonal_up_left",
            "diagonal_up_right",
            "gradient_up",
            "half_horizontal_bottom",
            "half_vertical_right",
            "globe",
            "piglin",
            "flow",
            "guster");

    public PlanetMinecraftOnlineService() {
        super(OnlineServiceType.PLANET_MINECRAFT);
    }

    @Override
    public String getServiceName() {
        return "PlanetMinecraft";
    }

    @Override
    public boolean supports(URI uri) {
        return checkUrlFormat(uri, "planetminecraft.com", "/banner/");
    }

    @Override
    protected void checkLinkType(URI uri) throws OnlineServiceException.WrongLinkTypeException {
        if (!uri.toString().matches("^http[s]?://([a-z]+\\.)?planetminecraft.com/banner/\\?e=[a-zA-Z0-9_-]+$")) {
            throw new OnlineServiceException.WrongLinkTypeException("Please use \"Remix Banner\" link instead");
        }
    }

    @Override
    protected BannerImportData extractBannerRecipeFromUrl(URI uri) throws OnlineServiceException {
        Map<String, String> parameters = UrlQuery.parameters(uri);
        // PlanetMinecraft uses e for edit or b for display
        String code = parameters.getOrDefault("b", parameters.get("e"));
        if (code == null || code.isBlank()) {
            throw new OnlineServiceException.UrlParsingException("No banner code found");
        }
        if (code.length() % 2 == 0) {
            throw new OnlineServiceException.UrlParsingException("Invalid banner code length: " + code.length());
        }
        int baseIndex = decode(code.charAt(0));
        DyeColor bannerColor = fromProviderIndex(baseIndex);

        List<BannerRecipeLayer> layers = new ArrayList<>();
        for (int i = 1; i < code.length(); i += 2) {
            DyeColor layerColor = fromProviderIndex(decode(code.charAt(i)));
            int patternIndex = decode(code.charAt(i + 1));
            if (patternIndex < 0 || patternIndex >= patterns().size()) {
                throw new OnlineServiceException.UrlParsingException("Unsupported pattern");
            }
            Identifier pattern = getPatternIdFromIndex(patternIndex);
            if (pattern == null) {
                throw new OnlineServiceException.UrlParsingException("Unsupported pattern");
            }
            layers.add(new BannerRecipeLayer(pattern, layerColor));
        }

        return new BannerImportData(bannerColor, layers);
    }

    @Override
    protected Map<String, Object> extractUrlParametersFromBannerRecipe(BannerRecipe recipe)
            throws OnlineServiceException {
        Map<String, Object> parameters = new LinkedHashMap<>();
        StringBuilder code = new StringBuilder();
        code.append(encode(providerIndex(fromName(recipe.bannerColor()))));

        for (BannerRecipeLayer layer : recipe.layers()) {
            int pattern = getPatternIndex(layer.pattern());
            if (pattern < 0) {
                throw new OnlineServiceException.UrlParsingException("Unsupported pattern: " + layer.pattern());
            }
            code.append(encode(providerIndex(layer.color())));
            code.append(encode(pattern));
        }

        parameters.put("e", code.toString());
        return parameters;
    }

    @Override
    protected String baseUrl() {
        return "https://www.planetminecraft.com/banner/";
    }

    @Override
    protected List<Identifier> patterns() {
        List<Identifier> result = new ArrayList<>(identifiers(PATTERNS));
        result.add(0, null);
        result.add(1, null);
        return Collections.unmodifiableList(result);
    }

    private static int decode(char value) throws OnlineServiceException.UrlParsingException {
        int index = ALPHABET.indexOf(value);
        if (index < 0) {
            throw new OnlineServiceException.UrlParsingException("Invalid code character: " + value);
        }
        return index;
    }

    private static char encode(int value) throws OnlineServiceException.UrlParsingException {
        if (value < 0 || value >= ALPHABET.length()) {
            throw new OnlineServiceException.UrlParsingException("Invalid code value: " + value);
        }
        return ALPHABET.charAt(value);
    }
}
