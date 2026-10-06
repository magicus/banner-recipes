/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import java.util.ArrayDeque;

/**
 * A bounded cursor for delimiters and offsets, not a replacement SNBT parser.
 */
final class Cursor {
    private final String input;
    int position;

    Cursor(String input) {
        this.input = input;
    }

    boolean atEnd() {
        return position == input.length();
    }

    char current() {
        return input.charAt(position);
    }

    boolean is(char value) {
        return !atEnd() && current() == value;
    }

    boolean take(char value) {
        if (!is(value)) {
            return false;
        }
        position++;
        return true;
    }

    void expect(char value) {
        if (!take(value)) {
            throw failure("Expected '" + value + "'");
        }
    }

    boolean skipWhitespace() {
        int start = position;
        while (!atEnd() && Character.isWhitespace(current())) {
            position++;
        }
        return position != start;
    }

    void requireWhitespace() {
        if (!skipWhitespace()) {
            throw failure("Expected whitespace");
        }
    }

    void requireEnd() {
        skipWhitespace();
        if (!atEnd()) {
            throw failure("Unexpected trailing input");
        }
    }

    String peekWord() {
        int end = position;
        while (end < input.length() && !Character.isWhitespace(input.charAt(end))) {
            end++;
        }
        return input.substring(position, end);
    }

    String readWord() {
        String word = peekWord();
        if (word.isEmpty()) {
            throw failure("Expected a word");
        }
        position += word.length();
        return word;
    }

    String readId() {
        int start = position;
        while (!atEnd()
                && (current() >= 'a' && current() <= 'z'
                        || current() >= '0' && current() <= '9'
                        || "_./:-".indexOf(current()) >= 0)) {
            position++;
        }
        if (position == start) {
            throw failure("Expected resource ID");
        }
        return input.substring(start, position);
    }

    int readInteger(int minimum, int maximum, String label) {
        int start = position;
        String token = readWord();
        try {
            if (!token.matches("[0-9]+")) {
                throw new NumberFormatException();
            }
            int value = Integer.parseInt(token);
            if (value < minimum || value > maximum) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw failure("Invalid " + label + ": " + token, start);
        }
    }

    void readOptionalCount() {
        boolean separated = skipWhitespace();
        if (atEnd()) {
            return;
        }
        if (!separated) {
            throw failure("Expected whitespace before count");
        }
        readInteger(1, Integer.MAX_VALUE, "count");
    }

    Slice readBalanced() {
        int start = position;
        if (!is('{') && !is('[') && !is('(')) {
            throw failure("Expected opening delimiter");
        }
        ArrayDeque<Character> closers = new ArrayDeque<>();
        do {
            char character = current();
            if (character == '\'' || character == '"') {
                readQuoted();
                continue;
            }
            position++;
            switch (character) {
                case '{' -> closers.push('}');
                case '[' -> closers.push(']');
                case '(' -> closers.push(')');
                case '}', ']', ')' -> {
                    if (closers.isEmpty() || closers.pop() != character) {
                        throw failure("Mismatched closing delimiter", position - 1);
                    }
                }
                default -> {}
            }
            if (closers.size() > CommandConverter.MAX_NESTING) {
                throw failure("Nesting is too deep");
            }
            if (closers.isEmpty()) {
                return new Slice(input.substring(start, position), start);
            }
        } while (!atEnd());
        throw failure("Unclosed delimiter", position);
    }

    void readQuoted() {
        int start = position;
        char quote = current();
        position++;
        while (!atEnd()) {
            char character = current();
            position++;
            if (character == quote) {
                return;
            }
            if (character == '\\') {
                if (atEnd()) {
                    throw failure("Unfinished escape sequence");
                }
                position++;
            }
        }
        throw failure("Unclosed string", start);
    }

    Slice readComponentValue() {
        int start = position;
        while (!atEnd() && !is(',') && !is(']')) {
            if (is('{') || is('[') || is('(')) {
                readBalanced();
            } else if (is('"') || is('\'')) {
                readQuoted();
            } else if (is('}') || is(')')) {
                throw failure("Unexpected closing delimiter");
            } else {
                position++;
            }
        }
        int end = position;
        while (end > start && Character.isWhitespace(input.charAt(end - 1))) {
            end--;
        }
        if (end == start) {
            throw failure("Expected component value", start);
        }
        return new Slice(input.substring(start, end), start);
    }

    ParseFailure failure(String message) {
        return failure(message, position);
    }

    ParseFailure failure(String message, int offset) {
        return new ParseFailure(message, offset);
    }
}
