/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.screens.newpackselection;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

public class FilesDropManager {
    private final NewPackSelectionScreen parent;
    private final PackSelectionManager packSelectionManager;

    public FilesDropManager(NewPackSelectionScreen parent) {
        this.parent = parent;
        this.packSelectionManager = parent.packSelectionManager;
    }

    public void onFilesDrop(List<Path> files) {
        ConfirmScreen confirmScreen = new ConfirmScreen(
                confirmed -> {
                    if (!confirmed) return;

                    tryCopyPacks(files);
                },
                Component.translatable("pack.dropConfirm"),
                Component.literal(getFileNames(files)));
        Minecraft.getInstance().gui.setScreen(confirmScreen);
    }

    private void tryCopyPacks(List<Path> droppedFiles) {
        List<Path> properPacks = packSelectionManager.getVerifiedPacks(droppedFiles);
        if (!properPacks.isEmpty()) {
            boolean success = packSelectionManager.copyPacks(properPacks);

            if (!success) {
                SystemToast.onPackCopyFailure(Minecraft.getInstance(), "");
            }

            parent.reload();
        }

        if (properPacks.size() == droppedFiles.size()) {
            Minecraft.getInstance().gui.setScreen(parent);
        } else {
            List<Path> leftoverPacks =
                    droppedFiles.stream().filter(p -> !properPacks.contains(p)).toList();
            AlertScreen alertScreen = new AlertScreen(
                    () -> Minecraft.getInstance().gui.setScreen(parent),
                    Component.translatable("pack.dropRejected.title"),
                    Component.translatable("pack.dropRejected.message", getFileNames(leftoverPacks)));
            Minecraft.getInstance().gui.setScreen(alertScreen);
        }
    }

    private String getFileNames(List<Path> files) {
        return files.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", "));
    }
}
