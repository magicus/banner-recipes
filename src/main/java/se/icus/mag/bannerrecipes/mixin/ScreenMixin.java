/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.gui.ScreenExtension;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "isInputCaptured", at = @At("HEAD"), cancellable = true)
    private void onIsInputCaptured(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof LoomScreen)) return;

        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        if (extension.isInputCaptured()) {
            cir.setReturnValue(true);
        }
    }
}
