/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.LoomMenu;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

public interface Weaver {
    static Weaver getWeaver(LoomMenu menu) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player.hasInfiniteMaterials()) {
            return new CreativeWeaver();
        } else {
            // return new SurvivalWeaver(menu);
            return new CreativeWeaver();
        }
    }

    void weave(BannerRecipe recipe);

    boolean isCurrentlyWeaving();

    void tick();

    boolean canWeave(BannerRecipe banner);

    List<String> getMissingMaterialDescriptions(BannerRecipe banner);
}
