/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

final class Snbt {
    static CompoundTag compound(Slice slice) {
        try {
            return TagParser.parseCompoundFully(slice.text());
        } catch (CommandSyntaxException exception) {
            throw syntaxError(exception, slice.start(), 0, slice.text().length());
        }
    }

    static Tag value(Slice slice) {
        String prefix = "{value:";
        try {
            return TagParser.parseCompoundFully(prefix + slice.text() + "}").get("value");
        } catch (CommandSyntaxException exception) {
            throw syntaxError(
                    exception, slice.start(), prefix.length(), slice.text().length());
        }
    }

    private static ParseFailure syntaxError(CommandSyntaxException exception, int start, int prefix, int length) {
        int local = exception.getCursor() < 0 ? 0 : exception.getCursor() - prefix;
        return new ParseFailure(exception.getRawMessage().getString(), start + Math.max(0, Math.min(local, length)));
    }
}
