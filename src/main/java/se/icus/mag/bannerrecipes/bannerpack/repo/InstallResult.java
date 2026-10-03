/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.bannerpack.repo;

public enum InstallResult {
    SUCCESS_NEW,
    SUCCESS_UPDATED,
    CONFLICT_UNMANAGED,
    HASH_MISMATCH,
    DOWNLOAD_FAILED
}
