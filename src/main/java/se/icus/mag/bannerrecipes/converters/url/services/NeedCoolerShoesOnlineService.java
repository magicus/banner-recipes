/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url.services;

import java.net.URI;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceException;
import se.icus.mag.bannerrecipes.converters.url.OnlineServiceType;

public final class NeedCoolerShoesOnlineService extends SkinMcCompatibleOnlineService {
    public NeedCoolerShoesOnlineService() {
        super(OnlineServiceType.NEED_COOLER_SHOES);
    }

    @Override
    public boolean supports(URI uri) {
        if (checkUrlFormat(uri, "needcoolershoes.com", "/banner")) return true;
        if (checkUrlFormat(uri, "ncrs.skin", "/b")) return true;
        return false;
    }

    @Override
    protected void checkLinkType(URI uri) throws OnlineServiceException.WrongLinkTypeException {
        if (!uri.toString().matches("^http[s]?://([a-z]+\\.)?needcoolershoes.com/banner\\?=[a-zA-Z0-9_-]+$")
                && !uri.toString().matches("^http[s]?://([a-z]+\\.)?ncrs.skin/b\\?=[a-zA-Z0-9_-]+$")) {
            throw new OnlineServiceException.WrongLinkTypeException("Please use \"Open in Editor\" link instead");
        }
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
