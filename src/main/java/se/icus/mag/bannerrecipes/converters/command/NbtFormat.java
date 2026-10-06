/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

/**
 * 1.13-1.20.4: colored item ID, attached NBT; dye IDs run white -> black.
 */
final class NbtFormat {
    static BannerRecipe read(Cursor cursor, DyeColor base) {
        int tagPosition = cursor.position;
        CompoundTag block = new CompoundTag();
        if (cursor.is('{')) {
            CompoundTag item = Snbt.compound(cursor.readBalanced());
            block = CommandConverter.optionalCompound(item, "BlockEntityTag", tagPosition);
        }
        cursor.readOptionalCount();
        cursor.requireEnd();
        return CommandConverter.recipe(
                base, CommandConverter.legacyLayers(block, false, tagPosition), CommandConverter.DEFAULT_DESCRIPTION);
    }
}
