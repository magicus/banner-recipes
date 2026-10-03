/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.gui.ScreenExtension;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        if (!((Object) this instanceof LoomScreen)) return;

        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        extension.removed();
        BannerRecipesMod.getManager().removeExtension();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickEnd(CallbackInfo ci) {
        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        extension.tick();
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void onExtractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!((Object) this instanceof LoomScreen)) return;

        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        extension.extractTooltip(graphics, mouseX, mouseY);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof LoomScreen)) return;

        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        if (extension.keyPressed(event)) {
            cir.setReturnValue(true);
        }
    }
}
