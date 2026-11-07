/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.storage;

public record BannerPackId(String id) {
    public static BannerPackId of(String id) {
        return new BannerPackId(id);
    }
}
