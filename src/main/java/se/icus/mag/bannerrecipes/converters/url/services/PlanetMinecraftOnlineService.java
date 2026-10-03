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
import java.util.regex.Pattern;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.bannerrecipes.converters.url.ColorCodec;
import se.icus.mag.bannerrecipes.converters.url.OnlineService;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceType;
import se.icus.mag.bannerrecipes.converters.url.UrlQuery;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

public final class PlanetMinecraftOnlineService extends OnlineService {
    private static final String ALPHABET = "123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0_-";
    private static final Pattern PAGE_LINK_PATTERN = Pattern.compile("^/banner/[^/]+(?:/.*)?$");
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
    public boolean supports(URI uri) throws OnlineService.PageLinkException {
        if (hostIs(host(uri), "planetminecraft.com")
                && PAGE_LINK_PATTERN.matcher(uri.getPath()).matches()) {
            throw new OnlineService.PageLinkException("Please use \"Remix Banner\" link instead");
        }
        return hostIs(host(uri), "planetminecraft.com") && "/banner/".equals(uri.getPath());
    }

    @Override
    protected BannerImportData extractBannerRecipeFromUrl(URI uri) {
        Map<String, String> parameters = UrlQuery.parameters(uri);
        String code = parameters.get("b");
        if (code == null) code = parameters.get("e");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("No PlanetMinecraft banner code found");
        if (code.isEmpty() || code.length() % 2 == 0) {
            throw new IllegalArgumentException("Invalid PlanetMinecraft banner code length: " + code.length());
        }
        int baseIndex = decode(code.charAt(0));
        DyeColor bannerColor = ColorCodec.fromProviderIndex(baseIndex);

        List<BannerRecipeLayer> layers = new ArrayList<>();
        for (int i = 1; i < code.length(); i += 2) {
            DyeColor layerColor = ColorCodec.fromProviderIndex(decode(code.charAt(i)));
            int patternIndex = decode(code.charAt(i + 1));
            if (patternIndex < 0 || patternIndex >= patterns().size()) {
                throw new IllegalArgumentException("Unsupported PlanetMinecraft pattern");
            }
            Identifier pattern = getPatternIdFromIndex(patternIndex);
            if (pattern == null) throw new IllegalArgumentException("Unsupported PlanetMinecraft pattern");
            layers.add(new BannerRecipeLayer(pattern, layerColor));
        }

        return new BannerImportData(bannerColor, layers);
    }

    @Override
    protected Map<String, Object> extractUrlParametersFromBannerRecipe(BannerRecipe recipe) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        StringBuilder code = new StringBuilder();
        code.append(encode(ColorCodec.providerIndex(ColorCodec.fromName(recipe.bannerColor()))));

        for (BannerRecipeLayer layer : recipe.layers()) {
            int pattern = getPatternIndex(layer.pattern());
            if (pattern < 0)
                throw new IllegalArgumentException("Unsupported PlanetMinecraft pattern: " + layer.pattern());
            code.append(encode(ColorCodec.providerIndex(layer.color())));
            code.append(encode(pattern));
        }

        parameters.put("b", code.toString());
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

    private static int decode(char value) {
        int index = ALPHABET.indexOf(value);
        if (index < 0) throw new IllegalArgumentException("Invalid PlanetMinecraft code character: " + value);
        return index;
    }

    private static char encode(int value) {
        if (value < 0 || value >= ALPHABET.length()) {
            throw new IllegalArgumentException("Invalid PlanetMinecraft code value: " + value);
        }
        return ALPHABET.charAt(value);
    }
}
