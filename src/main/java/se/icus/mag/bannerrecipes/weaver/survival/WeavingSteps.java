/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver.survival;

import java.util.OptionalInt;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class WeavingSteps {
    public static final class PlaceBanner implements WeavingStep {
        private final Item bannerItem;

        public PlaceBanner(Item bannerItem) {
            this.bannerItem = bannerItem;
        }

        @Override
        public StepResult perform(LoomController loom) throws WeavingException {
            if (!loom.placeBlankBanner(bannerItem)) {
                throw new WeavingException("Cannot find banner in inventory");
            }
            return StepResult.DONE;
        }
    }

    public static final class PlaceDye implements WeavingStep {
        private final Item dyeItem;
        private final String colorName;

        public PlaceDye(Item dyeItem, String colorName) {
            this.dyeItem = dyeItem;
            this.colorName = colorName;
        }

        @Override
        public StepResult perform(LoomController loom) throws WeavingException {
            if (!loom.placeDye(dyeItem)) {
                throw new WeavingException("Cannot find " + colorName + " dye");
            }
            return StepResult.DONE;
        }
    }

    public static final class PlacePatternItem implements WeavingStep {
        private final Item patternItem;

        public PlacePatternItem(Item patternItem) {
            this.patternItem = patternItem;
        }

        @Override
        public StepResult perform(LoomController loom) throws WeavingException {
            if (!loom.placePatternItem(patternItem)) {
                throw new WeavingException("Cannot find required pattern item");
            }
            return StepResult.DONE;
        }
    }

    public static final class RemovePatternItem implements WeavingStep {
        @Override
        public StepResult perform(LoomController loom) throws WeavingException {
            if (!loom.clearPatternItem()) {
                throw new WeavingException("Cannot return pattern item to inventory");
            }
            return StepResult.DONE;
        }
    }

    public static final class SelectPattern implements WeavingStep {
        private final Identifier pattern;

        public SelectPattern(Identifier pattern) {
            this.pattern = pattern;
        }

        @Override
        public StepResult perform(LoomController loom) throws WeavingException {
            OptionalInt patternIndex = loom.selectablePatternIndex(pattern);
            if (patternIndex.isEmpty()) {
                throw new WeavingException("Pattern not found: " + pattern);
            }
            loom.selectPattern(patternIndex.getAsInt());
            return StepResult.DONE;
        }
    }

    public static final class WaitForResult implements WeavingStep {
        @Override
        public StepResult perform(LoomController loom) {
            return loom.hasResult() ? StepResult.DONE : StepResult.WAITING;
        }
    }

    public static final class TakeResult implements WeavingStep {
        private final boolean finalLayer;

        public TakeResult(boolean finalLayer) {
            this.finalLayer = finalLayer;
        }

        @Override
        public StepResult perform(LoomController loom) {
            loom.takeResult(finalLayer);
            return StepResult.DONE;
        }
    }
}
