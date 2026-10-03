/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.weaver.survival;

import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import se.icus.mag.bannerrecipes.util.BannerUtils;

public final class LoomInventory {
    private final LoomMenu menu;

    public LoomInventory(LoomMenu menu) {
        this.menu = menu;
    }

    public ItemStack banner() {
        return menu.getBannerSlot().getItem();
    }

    public ItemStack dye() {
        return menu.getDyeSlot().getItem();
    }

    public ItemStack pattern() {
        return menu.getPatternSlot().getItem();
    }

    public ItemStack result() {
        return menu.getResultSlot().getItem();
    }

    public int availableInputCount(Item item) {
        int count = inventoryCount(item);
        if (dye().is(item)) count += dye().getCount();
        if (pattern().is(item)) count += pattern().getCount();
        return count;
    }

    public boolean hasBlankBanner(Item item) {
        return findBlankBannerSlot(item).isPresent();
    }

    public OptionalInt findInventorySlot(Item item) {
        return findInventorySlot(stack -> stack.is(item));
    }

    public OptionalInt findBlankBannerSlot(Item item) {
        return findInventorySlot(stack -> stack.is(item) && BannerUtils.isBlankBanner(stack));
    }

    private int inventoryCount(Item item) {
        return getSlotStream()
                .filter(slot -> slot.stack().is(item))
                .mapToInt(slot -> slot.stack().getCount())
                .sum();
    }

    private OptionalInt findInventorySlot(Predicate<ItemStack> predicate) {
        return getSlotStream()
                .filter(slot -> predicate.test(slot.stack()))
                .mapToInt(InventorySlot::index)
                .findFirst();
    }

    private Stream<InventorySlot> getSlotStream() {
        int startSlot = menu.getResultSlot().index + 1;
        return IntStream.range(startSlot, menu.slots.size())
                .mapToObj(index -> new InventorySlot(index, menu.getSlot(index).getItem()));
    }

    private record InventorySlot(int index, ItemStack stack) {}
}
