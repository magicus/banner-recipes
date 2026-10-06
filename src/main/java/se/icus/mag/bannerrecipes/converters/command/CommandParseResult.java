/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

/**
 * errorPosition is a zero-based UTF-16 offset into the original input.
 */
public record CommandParseResult(BannerRecipe recipe, String errorMessage, Integer errorPosition) {}
