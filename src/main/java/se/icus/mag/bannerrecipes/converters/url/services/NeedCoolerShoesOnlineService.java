/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url.services;

import java.net.URI;
import java.util.regex.Pattern;
import se.icus.mag.bannerrecipes.converters.url.OnlineService;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceType;

public final class NeedCoolerShoesOnlineService extends SkinMcCompatibleOnlineService {
    private static final Pattern PAGE_LINK_PATTERN = Pattern.compile("^/banners?/[^/]+(?:/.*)?$");

    public NeedCoolerShoesOnlineService() {
        super(OnlineServiceType.NEED_COOLER_SHOES);
    }

    @Override
    public boolean supports(URI uri) throws OnlineService.PageLinkException {
        String host = host(uri);
        if ((hostIs(host, "needcoolershoes.com") || host.equals("ncrs.skin"))
                && PAGE_LINK_PATTERN.matcher(uri.getPath()).matches()) {
            throw new OnlineService.PageLinkException("Please use \"Open in Editor\" link instead");
        }
        return (hostIs(host, "needcoolershoes.com") && "/banner".equals(uri.getPath()))
                || (host.equals("ncrs.skin") && "/b".equals(uri.getPath()));
    }

    @Override
    public String baseUrl() {
        return "https://needcoolershoes.com/banner";
    }

    @Override
    public String getServiceName() {
        return "NeedCoolerShoes";
    }
}
