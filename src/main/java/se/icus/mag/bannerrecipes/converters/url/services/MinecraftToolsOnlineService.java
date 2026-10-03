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
import se.icus.mag.bannerrecipes.converters.url.ColorCodec;
import se.icus.mag.bannerrecipes.converters.url.OnlineService;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceType;
import se.icus.mag.bannerrecipes.converters.url.UrlQuery;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;

public final class MinecraftToolsOnlineService extends OnlineService {
    private static final List<String> PATTERNS = List.of(
            "circle",
            "square_bottom_left",
            "square_bottom_right",
            "square_top_left",
            "square_top_right",
            "half_horizontal",
            "stripe_bottom",
            "stripe_top",
            "half_vertical",
            "stripe_left",
            "stripe_center",
            "stripe_right",
            "stripe_middle",
            "straight_cross",
            "stripe_downleft",
            "stripe_downright",
            "cross",
            "diagonal_left",
            "diagonal_right",
            "triangle_top",
            "triangle_bottom",
            "rhombus",
            "triangles_top",
            "triangles_bottom",
            "curly_border",
            "border",
            "small_stripes",
            "bricks",
            "gradient",
            "creeper",
            "skull",
            "flower",
            "mojang",
            "diagonal_up_left",
            "diagonal_up_right",
            "gradient_up",
            "half_horizontal_bottom",
            "half_vertical_right",
            "globe",
            "piglin");

    public MinecraftToolsOnlineService() {
        super(OnlineServiceType.MINECRAFT_TOOLS);
    }

    @Override
    public String getServiceName() {
        return "MinecraftTools";
    }

    @Override
    public boolean supports(URI uri) {
        return hostIs(host(uri), "minecraft.tools") && "/en/banner.php".equals(uri.getPath());
    }

    @Override
    protected BannerImportData extractBannerRecipeFromUrl(URI uri) {
        Map<String, String> parameters = UrlQuery.parameters(uri);
        int baseIndex = integer(parameters, "color_id_0");
        DyeColor bannerColor = ColorCodec.fromProviderIndex(baseIndex);

        ArrayList<BannerRecipeLayer> layers = new ArrayList<BannerRecipeLayer>();
        for (int index = 1; parameters.containsKey("shape_id_" + index); index++) {
            int shape = integer(parameters, "shape_id_" + index);
            int color = integer(parameters, "color_id_" + index);
            if (shape < 0 || shape >= patterns().size()) {
                throw new IllegalArgumentException("Invalid minecraft.tools pattern index: " + shape);
            }
            Identifier pattern = getPatternIdFromIndex(shape);
            if (pattern == null) continue;
            layers.add(new BannerRecipeLayer(pattern, ColorCodec.fromProviderIndex(color)));
        }

        return new BannerImportData(bannerColor, layers);
    }

    @Override
    protected Map<String, Object> extractUrlParametersFromBannerRecipe(BannerRecipe recipe) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("color_id_0", ColorCodec.providerIndex(ColorCodec.fromName(recipe.bannerColor())));

        for (int i = 0; i < recipe.layers().size(); i++) {
            BannerRecipeLayer layer = recipe.layers().get(i);
            int shape = getPatternIndex(layer.pattern());
            if (shape < 0)
                throw new IllegalArgumentException("Unsupported minecraft.tools pattern: " + layer.pattern());
            int index = i + 1;
            parameters.put("shape_id_" + index, shape);
            parameters.put("color_id_" + index, ColorCodec.providerIndex(layer.color()));
        }

        return parameters;
    }

    @Override
    protected String baseUrl() {
        return "https://minecraft.tools/en/banner.php";
    }

    @Override
    protected List<Identifier> patterns() {
        List<Identifier> result = new ArrayList<>(identifiers(PATTERNS));
        result.addFirst(null);
        return Collections.unmodifiableList(result);
    }

    private static int integer(Map<String, String> parameters, String name) {
        String value = parameters.get(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing parameter: " + name);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer parameter: " + name, e);
        }
    }
}
