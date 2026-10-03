/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class UrlQuery {
    public static Map<String, String> parameters(URI uri) {
        Map<String, String> parameters = new LinkedHashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) return parameters;

        for (String part : query.split("&", -1)) {
            String[] pieces = part.split("=", 2);
            String key = decode(pieces[0]);
            String value = pieces.length == 2 ? decode(pieces[1]) : "";
            parameters.put(key, value);
        }
        return parameters;
    }

    public static String withParameters(String baseUrl, Map<String, ?> parameters) {
        if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("Base URL cannot be empty");
        if (parameters == null || parameters.isEmpty()) return baseUrl;

        StringJoiner query = new StringJoiner("&");
        for (Map.Entry<String, ?> parameter : parameters.entrySet()) {
            query.add(encode(parameter.getKey()) + "=" + encode(String.valueOf(parameter.getValue())));
        }
        return baseUrl + (baseUrl.contains("?") ? "&" : "?") + query;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
