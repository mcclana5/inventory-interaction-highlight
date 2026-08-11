package com.inventoryhighlight;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;

@Slf4j
@Singleton
public class InventoryHighlightRenderer {
    private static final int CACHE_MAX_SIZE = 150;
    private static final int CACHE_EXPIRATION_MINUTES = 10;
    private static final int MAX_TEXT_SCAN_HEIGHT = 12;

    // High-performance Guava sprite cache (caches rendered fills and outlines)
    private final Cache<String, BufferedImage> spriteCache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .expireAfterAccess(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
            .build();

    // High-performance Guava mask cache (caches 2D text pixel masks on cache
    // misses)
    private final Cache<String, boolean[][]> maskCache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .expireAfterAccess(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
            .build();

    public static boolean isSpriteBased(FillStyle style) {
        return style != null && style.isSpriteBased();
    }

    public static boolean isSpriteBased(OutlineStyle style) {
        return style != null && style.isSpriteBased();
    }

    public void renderHighlight(Graphics2D graphics, Rectangle bounds, int itemId, int quantity,
            Color color, InventoryHighlightConfig config, ItemManager itemManager) {
        Stroke oldStroke = graphics.getStroke();
        Color oldColor = graphics.getColor();

        // 1. Render Fill (Background Layer)
        if (config.enableFill()) {
            renderFill(graphics, bounds, itemId, quantity, color, config.fillStyle(), config.fillOpacity(),
                    itemManager);
        }

        // 2. Render Outline (Border Layer on top of Fill)
        if (config.enableOutline()) {
            renderOutline(graphics, bounds, itemId, quantity, color, config.outlineStyle(), config.borderWidth(),
                    itemManager);
        }

        graphics.setStroke(oldStroke);
        graphics.setColor(oldColor);
    }

    public void renderSelectionHighlight(Graphics2D graphics, Rectangle bounds, int itemId, int quantity,
            Color hoverColor, InventoryHighlightConfig config, ItemManager itemManager) {
        if (hoverColor == null) {
            return;
        }

        int opacity = config.enableFill() ? Math.min(255, Math.max(0, config.fillOpacity())) : 65;
        Color fillColor = new Color(hoverColor.getRed(), hoverColor.getGreen(), hoverColor.getBlue(), opacity);

        // Selection Flash ALWAYS uses Background Only Fill (zero outlines to avoid 3D
        // mesh conflicts)
        renderSpriteFill(graphics, bounds, itemId, quantity, fillColor, FillStyle.BACKGROUND, itemManager);
    }

    private void renderFill(Graphics2D graphics, Rectangle bounds, int itemId, int quantity, Color baseColor,
            FillStyle style, int rawOpacity, ItemManager itemManager) {
        if (style == null) {
            return;
        }

        int opacity = Math.min(255, Math.max(0, rawOpacity));
        Color fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), opacity);

        if (style.isSpriteBased()) {
            renderSpriteFill(graphics, bounds, itemId, quantity, fillColor, style, itemManager);
            return;
        }

        switch (style) {
            case BOX:
                graphics.setColor(fillColor);
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                break;

            default:
                log.warn("Unhandled vector fill style: {}", style);
                break;
        }
    }

    private void renderSpriteFill(Graphics2D graphics, Rectangle bounds, int itemId, int quantity,
            Color fillColor, FillStyle style, ItemManager itemManager) {
        String key = style.name().toLowerCase() + "_" + itemId + "_" + quantity + "_" + fillColor.getRGB();
        BufferedImage cached = spriteCache.getIfPresent(key);

        if (cached == null) {
            cached = generateSpriteFill(itemId, quantity, fillColor, style, itemManager);
            if (cached != null) {
                spriteCache.put(key, cached);
            }
        }

        if (cached != null) {
            graphics.drawImage(cached, bounds.x, bounds.y, null);
        }
    }

    private BufferedImage generateSpriteFill(int itemId, int quantity, Color fillColor, FillStyle style,
            ItemManager itemManager) {
        ItemComposition itemComp = itemManager.getItemComposition(itemId);
        boolean isStack = quantity > 1 || (itemComp != null && itemComp.isStackable());

        BufferedImage itemImg = isStack
                ? itemManager.getImage(itemId, quantity, true)
                : itemManager.getImage(itemId, 1, false);

        if (itemImg == null) {
            return null;
        }

        AlphaComposite composite;
        switch (style) {
            case ITEM_SILHOUETTE:
                composite = AlphaComposite.DstIn;
                break;

            case BACKGROUND:
                composite = AlphaComposite.DstOut;
                break;

            default:
                log.warn("Unhandled sprite fill style: {}", style);
                return null;
        }

        int width = itemImg.getWidth();
        int height = itemImg.getHeight();
        BufferedImage buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buffer.createGraphics();

        g.setColor(fillColor);
        g.fillRect(0, 0, width, height);
        g.setComposite(composite);
        g.drawImage(itemImg, 0, 0, null);
        g.dispose();

        applyTextMaskIfStack(buffer, isStack, itemId, quantity, itemManager);

        return buffer;
    }

    private void renderOutline(Graphics2D graphics, Rectangle bounds, int itemId, int quantity, Color color,
            OutlineStyle style, int rawWidth, ItemManager itemManager) {
        if (style == null) {
            return;
        }

        int borderWidth = Math.max(1, rawWidth);

        switch (style) {
            case ITEM_SILHOUETTE:
                renderSilhouetteOutline(graphics, bounds, itemId, quantity, color, borderWidth, itemManager);
                break;

            case CORNER_BRACKETS:
                renderCornerBrackets(graphics, bounds, color, borderWidth);
                break;

            case BOX:
                renderBoxOutline(graphics, bounds, color, borderWidth);
                break;

            default:
                log.warn("Unhandled outline style: {}", style);
                break;
        }
    }

    private void renderSilhouetteOutline(Graphics2D graphics, Rectangle bounds, int itemId, int quantity,
            Color color, int borderWidth, ItemManager itemManager) {
        BufferedImage outline = getSilhouetteOutline(itemId, quantity, color, borderWidth, itemManager);
        if (outline != null) {
            graphics.drawImage(outline, bounds.x, bounds.y, null);
        }
    }

    private BufferedImage getSilhouetteOutline(int itemId, int quantity, Color color, int borderWidth,
            ItemManager itemManager) {
        String key = "outline_silhouette_" + itemId + "_" + quantity + "_" + borderWidth + "_" + color.getRGB();
        BufferedImage cached = spriteCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }

        BufferedImage baseOutline = itemManager.getItemOutline(itemId, quantity, color);
        if (baseOutline == null) {
            return null;
        }

        ItemComposition itemComp = itemManager.getItemComposition(itemId);
        boolean isStack = quantity > 1 || (itemComp != null && itemComp.isStackable());

        if (borderWidth == 1 && !isStack) {
            spriteCache.put(key, baseOutline);
            return baseOutline;
        }

        int width = baseOutline.getWidth();
        int height = baseOutline.getHeight();
        BufferedImage outlineBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = outlineBuffer.createGraphics();

        g.drawImage(baseOutline, 0, 0, null);
        for (int b = 1; b < borderWidth; b++) {
            g.drawImage(baseOutline, -b, 0, null);
            g.drawImage(baseOutline, b, 0, null);
            g.drawImage(baseOutline, 0, -b, null);
            g.drawImage(baseOutline, 0, b, null);
        }
        g.dispose();

        applyTextMaskIfStack(outlineBuffer, isStack, itemId, quantity, itemManager);

        spriteCache.put(key, outlineBuffer);
        return outlineBuffer;
    }

    private void renderCornerBrackets(Graphics2D graphics, Rectangle bounds, Color color, int borderWidth) {
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(borderWidth));
        int len = Math.min(8, Math.min(bounds.width, bounds.height) / 3);

        int x = bounds.x;
        int y = bounds.y;
        int right = x + bounds.width - 1;
        int bottom = y + bounds.height - 1;

        // Top-Left Corner
        graphics.drawLine(x, y, x + len, y);
        graphics.drawLine(x, y, x, y + len);

        // Top-Right Corner
        graphics.drawLine(right, y, right - len, y);
        graphics.drawLine(right, y, right, y + len);

        // Bottom-Left Corner
        graphics.drawLine(x, bottom, x + len, bottom);
        graphics.drawLine(x, bottom, x, bottom - len);

        // Bottom-Right Corner
        graphics.drawLine(right, bottom, right - len, bottom);
        graphics.drawLine(right, bottom, right, bottom - len);
    }

    private void renderBoxOutline(Graphics2D graphics, Rectangle bounds, Color color, int borderWidth) {
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(borderWidth));
        graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
    }

    private void applyTextMaskIfStack(BufferedImage sprite, boolean isStack, int itemId, int quantity,
            ItemManager itemManager) {
        if (isStack && sprite != null) {
            boolean[][] textPixelMask = getTextPixelMask(itemId, quantity, itemManager);
            if (textPixelMask != null) {
                applyTextMask(sprite, sprite.getWidth(), sprite.getHeight(), textPixelMask);
            }
        }
    }

    private boolean[][] getTextPixelMask(int itemId, int quantity, ItemManager itemManager) {
        String maskKey = "mask_" + itemId + "_" + quantity;
        boolean[][] cachedMask = maskCache.getIfPresent(maskKey);
        if (cachedMask != null) {
            return cachedMask;
        }

        BufferedImage baseImg = itemManager.getImage(itemId, quantity, false);
        BufferedImage textImg = itemManager.getImage(itemId, quantity, true);

        if (baseImg == null || textImg == null) {
            return null;
        }

        int width = textImg.getWidth();
        int height = textImg.getHeight();

        if (baseImg.getWidth() != width || baseImg.getHeight() != height) {
            return null;
        }

        boolean[][] mask = new boolean[height][width];
        int scanHeight = Math.min(MAX_TEXT_SCAN_HEIGHT, height);

        for (int y = 0; y < scanHeight; y++) {
            for (int x = 0; x < width; x++) {
                if (textImg.getRGB(x, y) != baseImg.getRGB(x, y)) {
                    mask[y][x] = true; // Mark pixel as text pixel
                }
            }
        }

        maskCache.put(maskKey, mask);
        return mask;
    }

    private void applyTextMask(BufferedImage sprite, int width, int height, boolean[][] textPixelMask) {
        int scanHeight = Math.min(MAX_TEXT_SCAN_HEIGHT, Math.min(height, textPixelMask.length));
        for (int y = 0; y < scanHeight; y++) {
            int scanWidth = Math.min(width, textPixelMask[y].length);
            for (int x = 0; x < scanWidth; x++) {
                if (textPixelMask[y][x]) {
                    sprite.setRGB(x, y, 0); // Make text character pixel transparent for all fill and outline modes
                }
            }
        }
    }
}
