/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

final class CommandEnvelope {
    static void read(Cursor cursor) {
        boolean slash = cursor.take('/');
        int commandStart = cursor.position;
        String command = cursor.peekWord();
        if (!command.equals("give") && !command.equals("minecraft:give")) {
            if (slash) {
                throw cursor.failure("Expected /give", commandStart);
            }
            return;
        }
        cursor.position += command.length();
        cursor.requireWhitespace();
        // A selector may contain quoted spaces, nested SNBT and commas.
        // Validate its framing; target resolution belongs to Minecraft.
        int targetStart = cursor.position;
        if (cursor.take('@')) {
            if (cursor.atEnd() || "paresn".indexOf(cursor.current()) < 0) {
                throw cursor.failure("Invalid target selector", targetStart);
            }
            cursor.position++;
            if (cursor.is('[')) {
                cursor.readBalanced();
            }
        } else {
            String target = cursor.readWord();
            if (!target.matches("[A-Za-z0-9_]{1,16}|[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) {
                throw cursor.failure("Invalid player name or UUID", targetStart);
            }
        }
        cursor.requireWhitespace();
    }
}
