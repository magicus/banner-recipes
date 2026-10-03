/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver.survival;

import java.util.List;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;

public final class LoomController {
    private final LoomMenu menu;
    private final Minecraft minecraft;
    private final LoomInventory inventory;

    public LoomController(LoomMenu menu, Minecraft minecraft) {
        this.menu = menu;
        this.minecraft = minecraft;
        this.inventory = new LoomInventory(menu);
    }

    public boolean placeBlankBanner(Item item) {
        if (!inventory.banner().isEmpty()) return true;

        OptionalInt source = inventory.findBlankBannerSlot(item);
        if (source.isEmpty()) return false;

        return moveOne(source.getAsInt(), menu.getBannerSlot());
    }

    public boolean placeDye(Item item) {
        if (inventory.dye().is(item)) return true;

        if (!inventory.dye().isEmpty()) {
            quickMove(menu.getDyeSlot());

            if (!inventory.dye().isEmpty()) return false;
        }

        OptionalInt source = inventory.findInventorySlot(item);
        if (source.isEmpty()) return false;

        return moveOne(source.getAsInt(), menu.getDyeSlot());
    }

    public boolean placePatternItem(Item item) {
        if (inventory.pattern().is(item)) return true;
        if (!clearPatternItem()) return false;

        OptionalInt source = inventory.findInventorySlot(item);
        if (source.isEmpty()) return false;

        return moveOne(source.getAsInt(), menu.getPatternSlot());
    }

    public boolean clearPatternItem() {
        if (inventory.pattern().isEmpty()) return true;

        quickMove(menu.getPatternSlot());
        return inventory.pattern().isEmpty();
    }

    public OptionalInt selectablePatternIndex(Identifier pattern) {
        List<Holder<BannerPattern>> selectablePatterns = menu.getSelectablePatterns();

        for (int index = 0; index < selectablePatterns.size(); index++) {
            Holder<BannerPattern> holder = selectablePatterns.get(index);
            boolean matches = holder.unwrapKey()
                    .filter(key -> key.identifier().equals(pattern))
                    .isPresent();

            if (matches) return OptionalInt.of(index);
        }
        return OptionalInt.empty();
    }

    public void selectPattern(int index) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
    }

    public boolean hasResult() {
        return !inventory.result().isEmpty();
    }

    public void takeResult(boolean finalLayer) {
        if (finalLayer) {
            click(menu.getResultSlot(), 0, ContainerInput.QUICK_MOVE);
            movePatternItemToInventory();
        } else {
            click(menu.getResultSlot(), 0, ContainerInput.PICKUP);
            movePatternItemToInventory();
            if (!inventory.banner().isEmpty()) {
                quickMove(menu.getBannerSlot());
            }
            click(menu.getBannerSlot(), 0, ContainerInput.PICKUP);
        }
    }

    private boolean moveOne(int sourceIndex, Slot destination) {
        ItemStack source = menu.getSlot(sourceIndex).getItem();
        if (source.isEmpty()) return false;

        click(sourceIndex, 0, ContainerInput.PICKUP);
        click(destination, 1, ContainerInput.PICKUP);
        click(sourceIndex, 0, ContainerInput.PICKUP);
        return true;
    }

    private void movePatternItemToInventory() {
        if (!inventory.pattern().isEmpty()) {
            click(menu.getPatternSlot(), 0, ContainerInput.QUICK_MOVE);
        }
    }

    private void quickMove(Slot slot) {
        click(slot, 0, ContainerInput.QUICK_MOVE);
    }

    private void click(Slot slot, int button, ContainerInput input) {
        click(slot.index, button, input);
    }

    private void click(int slot, int button, ContainerInput input) {
        minecraft.gameMode.handleContainerInput(menu.containerId, slot, button, input, minecraft.player);
    }
}
