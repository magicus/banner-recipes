/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes;

import java.util.List;

public final class Config {
    public static final String REPO_INDEX_URL =
            "https://raw.githubusercontent.com/magicus/banner-recipe-database/refs/heads/main/bannerpack-index-v1.json";
    public static final List<String> AUTO_INSTALL_PACK_IDS = List.of("categories", "numbers");
    public static final boolean ACTIVATE_ON_DOWNLOAD = true;

    private Config() {}
}
