/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.bannerpack;

import se.icus.mag.bannerrecipes.storage.BannerPackId;

public record BannerPackMetadata(BannerPackId id, String description, String author, String url) {}
