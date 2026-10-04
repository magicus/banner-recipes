/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.url;

public abstract class OnlineServiceException extends Exception {
    protected OnlineServiceException(String message) {
        super(message);
    }

    public static class WrongLinkTypeException extends OnlineServiceException {
        public WrongLinkTypeException(String message) {
            super(message);
        }
    }

    public static class UrlParsingException extends OnlineServiceException {
        public UrlParsingException(String message) {
            super(message);
        }
    }

    public static class UnsupportedUrlException extends OnlineServiceException {
        public UnsupportedUrlException(String message) {
            super(message);
        }
    }
}
