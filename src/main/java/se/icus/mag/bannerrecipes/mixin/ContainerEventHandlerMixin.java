/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.mixin;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.gui.ScreenExtension;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void charTypedStart(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!(this instanceof LoomScreen)) return;

        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        if (extension.charTyped(event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyReleased", at = @At("HEAD"), cancellable = true)
    private void onKeyReleased(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof LoomScreen)) return;

        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        if (extension.keyReleased(event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "preeditUpdated", at = @At("HEAD"), cancellable = true)
    private void onPreeditUpdated(PreeditEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof LoomScreen)) return;

        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        if (extension.preeditUpdated(event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void mouseDraggedStart(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (!(this instanceof LoomScreen)) return;

        ScreenExtension extension = BannerRecipesMod.getManager().getExtension();
        if (extension == null) return;

        if (extension.mouseDragged(event, dx, dy)) {
            cir.setReturnValue(true);
        }
    }
}
