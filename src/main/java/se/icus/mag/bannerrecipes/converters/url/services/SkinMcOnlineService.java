/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url.services;

import java.net.URI;
import java.util.regex.Pattern;
import se.icus.mag.bannerrecipes.converters.url.OnlineService;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceType;

public final class SkinMcOnlineService extends SkinMcCompatibleOnlineService {
    private static final Pattern PAGE_LINK_PATTERN = Pattern.compile("^/banner/(?!editor(?:/|$))[^/]+(?:/.*)?$");

    public SkinMcOnlineService() {
        super(OnlineServiceType.SKIN_MC);
    }

    @Override
    public boolean supports(URI uri) throws OnlineService.PageLinkException {
        if (hostIs(host(uri), "skinmc.net")
                && PAGE_LINK_PATTERN.matcher(uri.getPath()).matches()) {
            throw new OnlineService.PageLinkException("Please Use \"Edit design\" link instead");
        }
        return hostIs(host(uri), "skinmc.net") && "/banner/editor".equals(uri.getPath());
    }

    @Override
    public String baseUrl() {
        return "https://skinmc.net/banner/editor";
    }

    @Override
    public String getServiceName() {
        return "SkinMC";
    }
}
