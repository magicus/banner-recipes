/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver.survival;

@FunctionalInterface
public interface WeavingStep {
    StepResult perform(LoomController loom) throws WeavingException;

    enum StepResult {
        DONE,
        WAITING
    }

    final class WeavingException extends Exception {
        public WeavingException(String message) {
            super(message);
        }
    }
}
