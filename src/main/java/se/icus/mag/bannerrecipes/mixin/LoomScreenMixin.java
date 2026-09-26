/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.gui.ScreenExtension;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void initEnd(CallbackInfo ci) {
        BannerRecipesMod.getManager().createExtension((LoomScreen) (Object) this);

        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        extension.init();
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void extractBackgroundEnd(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        extension.extractBackground(graphics, mouseX, mouseY, delta);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mouseClickedStart(
            MouseButtonEvent mouseButtonEvent, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        if (extension.mouseClicked(mouseButtonEvent)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void mouseReleasedStart(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        if (extension.mouseReleased(mouseButtonEvent)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void mouseScrolledStart(
            double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        if (extension.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            cir.setReturnValue(true);
        }
    }
}
