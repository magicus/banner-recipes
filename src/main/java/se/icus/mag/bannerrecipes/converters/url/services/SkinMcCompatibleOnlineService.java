/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.bannerrecipes.converters.url.ColorCodec;
import se.icus.mag.bannerrecipes.converters.url.OnlineService;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceType;
import se.icus.mag.bannerrecipes.converters.url.UrlQuery;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

public abstract class SkinMcCompatibleOnlineService extends OnlineService {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/";
    private static final List<String> PATTERNS = List.of(
            "base",
            "square_bottom_left",
            "border",
            "square_bottom_right",
            "bricks",
            "stripe_bottom",
            "triangle_bottom",
            "triangles_bottom",
            "curly_border",
            "cross",
            "creeper",
            "stripe_center",
            "stripe_downleft",
            "stripe_downright",
            "flower",
            "gradient",
            "half_horizontal",
            "diagonal_left",
            "stripe_left",
            "circle",
            "mojang",
            "rhombus",
            "stripe_middle",
            "diagonal_up_right",
            "stripe_right",
            "straight_cross",
            "skull",
            "small_stripes",
            "square_top_left",
            "square_top_right",
            "stripe_top",
            "triangle_top",
            "triangles_top",
            "half_vertical",
            "diagonal_up_left",
            "diagonal_right",
            "gradient_up",
            "half_horizontal_bottom",
            "half_vertical_right",
            "globe",
            "piglin",
            "flow",
            "guster");

    protected SkinMcCompatibleOnlineService(OnlineServiceType serviceType) {
        super(serviceType);
    }

    @Override
    protected BannerImportData extractBannerRecipeFromUrl(URI uri) {
        String code = UrlQuery.parameters(uri).get("");
        if (code == null || code.isBlank()) {
            code = UrlQuery.parameters(uri).values().stream()
                    .filter(value -> !value.isBlank())
                    .findFirst()
                    .orElse("");
        }
        if (code.isBlank()) throw new IllegalArgumentException("Banner code cannot be empty");
        if (code.length() % 2 != 0) throw new IllegalArgumentException("Banner code must have an even length");

        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < code.length(); i += 2) {
            pairs.add(decodePair(code.substring(i, i + 2)));
        }
        Pair base = pairs.getFirst();
        DyeColor bannerColor = ColorCodec.fromProviderIndex(base.colorIndex());

        List<BannerRecipeLayer> layers = new ArrayList<>();
        for (Pair pair : pairs.subList(1, pairs.size())) {
            Identifier pattern = getPatternIdFromIndex(pair.patternIndex());
            layers.add(new BannerRecipeLayer(pattern, ColorCodec.fromProviderIndex(pair.colorIndex())));
        }

        return new BannerImportData(bannerColor, layers);
    }

    @Override
    protected Map<String, Object> extractUrlParametersFromBannerRecipe(BannerRecipe recipe) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        StringBuilder code = new StringBuilder(
                encodePair(new Pair(0, ColorCodec.providerIndex(ColorCodec.fromName(recipe.bannerColor())))));

        for (BannerRecipeLayer layer : recipe.layers()) {
            int pattern = getPatternIndex(layer.pattern());
            if (pattern < 0) throw new IllegalArgumentException("Unsupported SkinMC pattern: " + layer.pattern());
            code.append(encodePair(new Pair(pattern, ColorCodec.providerIndex(layer.color()))));
        }

        parameters.put("", code.toString());
        return parameters;
    }

    @Override
    protected List<Identifier> patterns() {
        return identifiers(PATTERNS);
    }

    private Pair decodePair(String pair) {
        int high = ALPHABET.indexOf(pair.charAt(0));
        int low = ALPHABET.indexOf(pair.charAt(1));
        if (high < 0 || low < 0) throw new IllegalArgumentException("Invalid banner code character");

        int patternIndex = ((high >> 4) << 6) | low;
        if (patternIndex >= patterns().size()) {
            throw new IllegalArgumentException("Unsupported banner pattern index: " + patternIndex);
        }
        return new Pair(patternIndex, high & 15);
    }

    private static String encodePair(Pair pair) {
        if (pair.patternIndex() < 0 || pair.patternIndex() >= 256 || pair.colorIndex() < 0 || pair.colorIndex() > 15) {
            throw new IllegalArgumentException("Invalid SkinMC encoding values");
        }
        int high = ((pair.patternIndex() >> 6) << 4) | pair.colorIndex();
        int low = pair.patternIndex() & 63;
        return "" + ALPHABET.charAt(high) + ALPHABET.charAt(low);
    }

    private record Pair(int patternIndex, int colorIndex) {}
}
