/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

/**
 * 1.8-1.12: count and damage precede NBT; dye IDs run black -> white.
 */
final class DamageFormat {
    static BannerRecipe read(Cursor cursor) {
        int damage = 0;
        int damagePosition = cursor.position;
        CompoundTag itemTag = new CompoundTag();
        boolean separated = cursor.skipWhitespace();
        if (!cursor.atEnd() && !cursor.is('{')) {
            if (!separated) {
                throw cursor.failure("Expected whitespace");
            }
            cursor.readInteger(1, Integer.MAX_VALUE, "count");
            separated = cursor.skipWhitespace();
            if (!cursor.atEnd() && !cursor.is('{')) {
                if (!separated) {
                    throw cursor.failure("Expected whitespace");
                }
                damagePosition = cursor.position;
                damage = cursor.readInteger(0, 15, "banner damage");
                separated = cursor.skipWhitespace();
            }
        }
        int tagPosition = cursor.position;
        if (cursor.is('{')) {
            if (!separated) {
                throw cursor.failure("Expected whitespace before legacy NBT");
            }
            itemTag = Snbt.compound(cursor.readBalanced());
        }
        cursor.requireEnd();
        CompoundTag block = CommandConverter.optionalCompound(itemTag, "BlockEntityTag", tagPosition);
        // Accept historical generator output carrying Base in BlockEntityTag.
        // Base uses the same reversed dye numbering as damage and Color.
        if (block.contains("Base")) {
            damage = CommandConverter.integer(block.get("Base"), "Base", tagPosition);
            damagePosition = tagPosition;
        }
        DyeColor base = CommandConverter.numericColor(damage, true, damagePosition);
        return CommandConverter.recipe(
                base, CommandConverter.legacyLayers(block, true, tagPosition), CommandConverter.DEFAULT_DESCRIPTION);
    }
}
