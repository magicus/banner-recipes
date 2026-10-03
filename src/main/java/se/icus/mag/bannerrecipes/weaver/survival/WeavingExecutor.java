/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver.survival;

import java.util.Deque;
import se.icus.mag.bannerrecipes.BannerRecipesMod;

public final class WeavingExecutor {
    private final LoomController loom;
    private final Deque<WeavingStep> pendingSteps;

    public WeavingExecutor(WeavingPlan plan, LoomController loom) {
        this.loom = loom;
        this.pendingSteps = plan.pendingSteps();
    }

    public boolean isActive() {
        return !pendingSteps.isEmpty();
    }

    public void nextStep() {
        if (!isActive()) return;

        try {
            if (pendingSteps.getFirst().perform(loom) == WeavingStep.StepResult.DONE) {
                pendingSteps.removeFirst();
                if (pendingSteps.isEmpty()) {
                    BannerRecipesMod.LOGGER.info("Auto-weave complete!");
                }
            }
        } catch (WeavingStep.WeavingException exception) {
            pendingSteps.clear();
            BannerRecipesMod.LOGGER.warn("Auto-weave error: {}", exception.getMessage());
        }
    }
}
