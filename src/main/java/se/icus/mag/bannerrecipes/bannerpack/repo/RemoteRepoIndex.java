/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.bannerpack.repo;

import java.util.List;

public record RemoteRepoIndex(int repoVersion, List<RemotePackEntry> packs) {}
