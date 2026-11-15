/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

/** Helpers for reporting syntax error positions relative to the original, unmodified user input. */
public final class ParseErrors {
    private ParseErrors() {}

    /**
     * Rebases an error from a parser that was given a part of {@code fullInput}, starting at {@code offset}, so
     * that its cursor refers to a position in {@code fullInput}.
     */
    public static CommandSyntaxException rebase(CommandSyntaxException e, int offset, String fullInput) {
        if (e.getCursor() < 0 || offset == 0) {
            return e;
        }
        return new CommandSyntaxException(e.getType(), e.getRawMessage(), fullInput, e.getCursor() + offset);
    }

    public static int leadingWhitespace(String input) {
        return input.length() - input.stripLeading().length();
    }
}
