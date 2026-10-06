/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.converters.command;

import net.minecraft.world.item.DyeColor;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;

final class ItemParser {
    static BannerRecipe read(Cursor cursor) {
        if (cursor.is('{')) {
            // Compatibility with the serialized item form emitted by the
            // previous converter. This is not a fourth historical give style.
            return ComponentsFormat.readSerializedItem(cursor);
        }
        int itemPosition = cursor.position;
        String item = CommandConverter.normalizeId(cursor.readId(), itemPosition);
        if (item.equals("minecraft:banner")) {
            return DamageFormat.read(cursor);
        }
        DyeColor base = CommandConverter.bannerColor(item, itemPosition);
        // A plain colored banner is shared by both newer formats.
        if (cursor.is('[')) {
            return ComponentsFormat.read(cursor, base);
        }
        return NbtFormat.read(cursor, base);
    }
}
