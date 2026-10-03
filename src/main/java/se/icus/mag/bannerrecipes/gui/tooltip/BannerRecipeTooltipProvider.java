/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.bannerrecipes.gui.tooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.bannerrecipes.BannerRecipesMod;
import se.icus.mag.bannerrecipes.recipe.BannerRecipe;
import se.icus.mag.bannerrecipes.recipe.BannerRecipeLayer;
import se.icus.mag.bannerrecipes.util.Lookup;

public class BannerRecipeTooltipProvider {
    private static final int ROW_HEIGHT = 17;
    private static final int TEXT_Y_OFFSET = 5;

    private static final int PREVIEW_WIDTH = 20;
    private static final int PREVIEW_HEIGHT = 40;

    private static final int ITEM_X = 26;
    private static final int PATTERN_X = 42;
    private static final int NUMBER_X = 58;
    private static final int DESCRIPTION_X = 78;

    private static final int PATTERN_WIDTH = 7;
    private static final int PATTERN_HEIGHT = 14;

    private static final int TITLE_GAP = 2;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TOOLTIP_X_OFFSET = 12;
    private static final int TOOLTIP_Y_OFFSET = 12;
    private static final int TOOLTIP_SCREEN_MARGIN = 4;
    private static final int TOOLTIP_BOTTOM_MARGIN = 3;

    private final Minecraft minecraft;
    private final int width;
    private final int height;
    private final int bodyYOffset;

    private final Component title;
    private final BannerFlagModel previewBannerType;
    private final DyeColor previewBaseColor;
    private final BannerPatternLayers previewPatterns;
    private final List<WeavingStep> weavingSteps;

    private BannerRecipeTooltipProvider(
            Minecraft minecraft,
            int width,
            int height,
            int bodyYOffset,
            Component title,
            BannerFlagModel previewBannerType,
            DyeColor previewBaseColor,
            BannerPatternLayers previewPatterns,
            List<WeavingStep> weavingSteps) {
        this.minecraft = minecraft;
        this.width = width;
        this.height = height;
        this.bodyYOffset = bodyYOffset;
        this.title = title;
        this.previewBannerType = previewBannerType;
        this.previewBaseColor = previewBaseColor;
        this.previewPatterns = previewPatterns;
        this.weavingSteps = weavingSteps;
    }

    public static BannerRecipeTooltipProvider from(BannerRecipe recipe) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStack banner = BannerRecipesMod.getItemStack(recipe);
        DyeColor baseColor = DyeColor.byName(recipe.bannerColor(), DyeColor.WHITE);
        Registry<BannerPattern> patternRegistry = BannerRecipesMod.getBannerPatternRegistry(minecraft);
        Component title = Component.literal(recipe.description());
        BannerFlagModel previewBannerType =
                new BannerFlagModel(minecraft.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        BannerPatternLayers previewPatterns = Objects.requireNonNullElseGet(
                banner.get(DataComponents.BANNER_PATTERNS), () -> new BannerPatternLayers.Builder().build());
        List<WeavingStep> weavingSteps = createSteps(recipe, banner, baseColor, patternRegistry);

        int descriptionWidth = weavingSteps.stream()
                .mapToInt(weavingStep -> minecraft.font.width(weavingStep.description()))
                .max()
                .orElse(0);
        int width = Math.max(minecraft.font.width(title), DESCRIPTION_X + descriptionWidth);
        int bodyYOffset = minecraft.font.lineHeight + TITLE_GAP;
        int height = bodyYOffset + Math.max(PREVIEW_HEIGHT, weavingSteps.size() * ROW_HEIGHT - 1);

        return new BannerRecipeTooltipProvider(
                minecraft,
                width,
                height,
                bodyYOffset,
                title,
                previewBannerType,
                baseColor,
                previewPatterns,
                weavingSteps);
    }

    private static List<WeavingStep> createSteps(
            BannerRecipe recipe, ItemStack banner, DyeColor baseColor, Registry<BannerPattern> patternRegistry) {
        List<WeavingStep> weavingSteps = new ArrayList<>();

        weavingSteps.add(new WeavingStep(
                new ItemStack(Lookup.getBannerFromDyeColor(baseColor)),
                null,
                Component.empty(),
                Component.translatable(banner.getItem().getDescriptionId())));

        for (int i = 0; i < recipe.layers().size(); i++) {
            BannerRecipeLayer layer = recipe.layers().get(i);

            ItemStack itemStack = new ItemStack(Lookup.getItemFromDyeColor(layer.color()));
            MutableComponent stepNumber = Component.literal((i + 1) + ". ");

            Component patternDescription;
            SpriteId patternSprite;

            Holder.Reference<BannerPattern> pattern =
                    patternRegistry.get(layer.pattern()).orElse(null);
            if (pattern == null) {
                BannerRecipesMod.LOGGER.warn(
                        "Banner recipe {} references unknown pattern {}", recipe.id(), layer.pattern());
                patternDescription = Component.literal("Unknown");
                patternSprite = null;
            } else {
                // e.g. "block.minecraft.banner.creeper.white"
                patternDescription = Component.translatable(
                        pattern.value().translationKey() + "." + layer.color().getName());
                patternSprite = Sheets.getBannerSprite(pattern);
            }

            weavingSteps.add(new WeavingStep(itemStack, patternSprite, stepNumber, patternDescription));
        }

        return List.copyOf(weavingSteps);
    }

    public void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Minecraft minecraft) {
        int x = getTooltipX(mouseX, minecraft.getWindow().getGuiScaledWidth());
        int y = getTooltipY(mouseY, minecraft.getWindow().getGuiScaledHeight());
        int bodyY = y + bodyYOffset;

        TooltipRenderUtil.extractTooltipBackground(graphics, x, y, width, height, null);

        graphics.text(minecraft.font, title, x, y, TEXT_COLOR, false);

        graphics.bannerPattern(
                previewBannerType,
                previewBaseColor,
                previewPatterns,
                x,
                bodyY,
                x + PREVIEW_WIDTH,
                bodyY + PREVIEW_HEIGHT);

        extractWeavingSteps(graphics, x, bodyY);
    }

    private void extractWeavingSteps(GuiGraphicsExtractor graphics, int x, int y) {
        int rowY = y;

        for (WeavingStep weavingStep : weavingSteps) {
            graphics.fakeItem(weavingStep.item(), x + ITEM_X, rowY);

            if (weavingStep.patternSprite() != null) {
                renderPattern(graphics, weavingStep.patternSprite(), x + PATTERN_X, rowY);
            }

            renderStepText(graphics, weavingStep, x, rowY);

            rowY += ROW_HEIGHT;
        }
    }

    private void renderStepText(GuiGraphicsExtractor graphics, WeavingStep weavingStep, int x, int y) {
        int textY = y + TEXT_Y_OFFSET;

        graphics.text(minecraft.font, weavingStep.number(), x + NUMBER_X, textY, TEXT_COLOR, false);
        graphics.text(minecraft.font, weavingStep.description(), x + DESCRIPTION_X, textY, TEXT_COLOR, false);
    }

    private void renderPattern(GuiGraphicsExtractor graphics, SpriteId spriteId, int x, int y) {
        TextureAtlasSprite sprite = graphics.getSprite(spriteId);

        float u0 = sprite.getU0();
        float u1 = u0 + (sprite.getU1() - u0) * 21.0F / 64.0F;

        float vSpan = sprite.getV1() - sprite.getV0();
        float v0 = sprite.getV0() + vSpan / 64.0F;
        float v1 = v0 + vSpan * 40.0F / 64.0F;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 4, y + 1);

        graphics.fill(0, 0, PATTERN_WIDTH, PATTERN_HEIGHT, DyeColor.GRAY.getTextureDiffuseColor());

        graphics.blit(sprite.atlasLocation(), 0, 0, PATTERN_WIDTH, PATTERN_HEIGHT, u0, u1, v0, v1);

        graphics.pose().popMatrix();
    }

    private int getTooltipY(int mouseY, int screenHeight) {
        int y = mouseY - TOOLTIP_Y_OFFSET;
        return y <= (screenHeight - height - TOOLTIP_BOTTOM_MARGIN) ? y : screenHeight - height - TOOLTIP_BOTTOM_MARGIN;
    }

    private int getTooltipX(int mouseX, int screenWidth) {
        int x = mouseX + TOOLTIP_X_OFFSET;
        return x <= (screenWidth - width) ? x : Math.max(x - 24 - width, TOOLTIP_SCREEN_MARGIN);
    }

    private record WeavingStep(ItemStack item, SpriteId patternSprite, Component number, Component description) {}
}
