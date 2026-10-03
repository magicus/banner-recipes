/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.bannerpack.repo;

public record PackUpdateStatus(RemotePackEntry remoteEntry, InstallStatus status) {
    public enum InstallStatus {
        NOT_INSTALLED,
        INSTALLED_UP_TO_DATE,
        UPDATE_AVAILABLE,
        CONFLICT_UNMANAGED
    }
}
