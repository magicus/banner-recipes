/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url.services;

import java.net.URI;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceException;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceType;

public final class SkinMcOnlineService extends SkinMcCompatibleOnlineService {
    public SkinMcOnlineService() {
        super(OnlineServiceType.SKIN_MC);
    }

    @Override
    public boolean supports(URI uri) {
        return checkUrlFormat(uri, "skinmc.net", "/banner/");
    }

    @Override
    protected void checkLinkType(URI uri) throws OnlineServiceException.WrongLinkTypeException {
        if (!uri.toString().matches("^http[s]?://([a-z]+\\.)?skinmc.net/banner/editor\\?=[a-zA-Z0-9_-]+$")) {
            throw new OnlineServiceException.WrongLinkTypeException("Please Use \"Edit design\" link instead");
        }
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
