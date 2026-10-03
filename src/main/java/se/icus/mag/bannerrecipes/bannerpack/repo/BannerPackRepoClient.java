/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.bannerpack.repo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class BannerPackRepoClient {
    private static final Gson GSON = new GsonBuilder().create();
    private final URI indexUri;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public BannerPackRepoClient(String indexUrl) {
        indexUri = httpsUri(indexUrl);
    }

    public RemoteRepoIndex fetchIndex() throws IOException {
        HttpRequest request = HttpRequest.newBuilder(indexUri)
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "banner-recipes")
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = send(httpClient, request, HttpResponse.BodyHandlers.ofString());
        if (!"https".equalsIgnoreCase(response.uri().getScheme())) {
            throw new IOException("Banner-pack index redirected away from HTTPS");
        }
        if (response.statusCode() != 200) throw new IOException("Repository returned HTTP " + response.statusCode());
        try {
            RemoteRepoIndex index = GSON.fromJson(response.body(), RemoteRepoIndex.class);
            if (index == null || index.packs() == null) throw new IOException("Repository index has no packs list");
            if (index.repoVersion() != 1) throw new IOException("Unsupported repository index version");
            return index;
        } catch (JsonSyntaxException e) {
            throw new IOException("Failed to parse banner-pack repository index", e);
        }
    }

    public void downloadZip(String downloadUrl, Path target) throws IOException {
        URI uri = httpsUri(downloadUrl);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(2))
                .header("User-Agent", "banner-recipes")
                .GET()
                .build();
        HttpResponse<Path> response;
        try {
            response = send(httpClient, request, HttpResponse.BodyHandlers.ofFile(target));
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }
        if (response.statusCode() != 200) {
            Files.deleteIfExists(target);
            throw new IOException("Pack download returned HTTP " + response.statusCode());
        }
        if (!"https".equalsIgnoreCase(response.uri().getScheme())) {
            Files.deleteIfExists(target);
            throw new IOException("Banner-pack download redirected away from HTTPS");
        }
    }

    private static URI httpsUri(String value) {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("Banner-pack repository URLs must use HTTPS");
        }
        return uri;
    }

    private static <T> HttpResponse<T> send(HttpClient client, HttpRequest request, HttpResponse.BodyHandler<T> handler)
            throws IOException {
        try {
            return client.send(request, handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Banner-pack repository request was interrupted", e);
        }
    }
}
