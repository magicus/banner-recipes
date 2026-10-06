/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

final class ParseFailure extends IllegalArgumentException {
    final int position;

    ParseFailure(String message, int position) {
        super(message);
        this.position = position;
    }
}
