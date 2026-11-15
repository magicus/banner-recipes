/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import se.icus.mag.bannerrecipes.gui.ChildScreen;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @WrapOperation(
            method = "setScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"))
    private void wrapRemoveScreen(Screen oldScreen, Operation<Void> original, Screen newScreen) {
        // Opening a child screen: the parent stays alive until the child is closed
        if (newScreen instanceof ChildScreen child && child.getParentScreen() == oldScreen) {
            return;
        }

        original.call(oldScreen);

        // Leaving a child for something other than its parent: the suspended parents must be removed too
        Screen current = oldScreen;
        while (current instanceof ChildScreen child) {
            Screen parent = child.getParentScreen();
            if (parent == null || parent == newScreen) {
                break;
            }
            original.call(parent);
            current = parent;
        }
    }
}
