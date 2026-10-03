/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.storage;

public record InstalledPackState(String packId, String fileName, String sourceUrl, String sha256, String installedAt) {}
