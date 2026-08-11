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
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public class InventoryHighlightOverlay extends WidgetItemOverlay {
    private static final long FLASH_HALF_TICK_MS = 300L;
    private static final int CLICK_PRESS_ALPHA_BOOST = 70;
    private static final int CLICK_PRESS_INSET_PX = 1;
    private static final int CACHE_MAX_SIZE = 150;
    private static final int CACHE_EXPIRATION_MINUTES = 10;
    private static final int DEFAULT_SELECTION_OPACITY = 65;

    private final Client client;
    private final InventoryHighlightConfig config;
    private final ItemManager itemManager;

    private long lastGameTickTime = 0;

    // High-performance Guava sprite cache (eliminates per-frame BufferedImage
    // memory allocations)
    private final Cache<String, BufferedImage> spriteCache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .expireAfterAccess(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
            .build();

    @Inject
    public InventoryHighlightOverlay(Client client, InventoryHighlightConfig config, ItemManager itemManager) {
        this.client = client;
        this.config = config;
        this.itemManager = itemManager;
        showOnInventory();
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        lastGameTickTime = System.currentTimeMillis();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget) {
        if (itemWidget == null || itemWidget.getWidget() == null) {
            return;
        }

        // Check if mouse button is actively pressed down
        boolean isMouseDown = client.getMouseCurrentButton() != 0;
        Widget draggedWidget = client.getDraggedWidget();

        // 1. Permanent drag and fast mouse swipe suppression
        if (isMouseDown && (client.isDraggingWidget() || draggedWidget != null)) {
            if (draggedWidget == null || draggedWidget.getIndex() != itemWidget.getWidget().getIndex()) {
                return;
            }
        }

        Rectangle bounds = itemWidget.getCanvasBounds();
        if (bounds == null) {
            return;
        }

        // 2. Selection state highlight ("Use Item -> ...")
        boolean isSelected = client.isWidgetSelected()
                && client.getSelectedWidget() != null
                && client.getSelectedWidget().equals(itemWidget.getWidget());

        if (isSelected) {
            if (config.enableSelectionFlash()) {
                // Game Tick Synchronized Flash Cycle: ON for first 300ms of game tick, OFF for
                // second 300ms
                long timeInTick = lastGameTickTime > 0 ? (System.currentTimeMillis() - lastGameTickTime) : 0;
                boolean flashVisible = (timeInTick < FLASH_HALF_TICK_MS);

                if (flashVisible) {
                    renderSelectionHighlight(graphics, bounds, itemId, itemWidget.getQuantity());
                }
            }
            return;
        }

        // 3. Hover and active click highlight
        if (!config.highlightHover()) {
            return;
        }

        Point mousePt = client.getMouseCanvasPosition();
        if (mousePt == null || !bounds.contains(mousePt.getX(), mousePt.getY())) {
            return;
        }

        Color activeColor = getActiveHighlightColor();
        if (activeColor == null) {
            return;
        }

        if (isMouseDown && config.highlightClick()) {
            // 1-pixel inset bounds for tactile "button press" click effect
            Rectangle clickBounds = new Rectangle(
                    bounds.x + CLICK_PRESS_INSET_PX,
                    bounds.y + CLICK_PRESS_INSET_PX,
                    Math.max(1, bounds.width - (CLICK_PRESS_INSET_PX * 2)),
                    Math.max(1, bounds.height - (CLICK_PRESS_INSET_PX * 2)));

            // Boost alpha for brighter/more solid click press color
            int pressAlpha = Math.min(255, activeColor.getAlpha() + CLICK_PRESS_ALPHA_BOOST);
            Color pressColor = new Color(activeColor.getRed(), activeColor.getGreen(), activeColor.getBlue(),
                    pressAlpha);

            renderHoverHighlight(graphics, clickBounds, itemId, itemWidget.getQuantity(), pressColor);
        } else {
            renderHoverHighlight(graphics, bounds, itemId, itemWidget.getQuantity(), activeColor);
        }
    }

    Color getActiveHighlightColor() {
        return (isDropAction() && config.enableDropHighlight())
                ? config.dropColor()
                : config.hoverColor();
    }

    boolean isDropAction() {
        Menu menu = client.getMenu();
        if (menu != null) {
            MenuEntry[] menuEntries = menu.getMenuEntries();
            if (menuEntries != null && menuEntries.length > 0) {
                MenuEntry top = menuEntries[menuEntries.length - 1];
                return top != null && "Drop".equalsIgnoreCase(top.getOption());
            }
        }
        return false;
    }

    private void renderSelectionHighlight(Graphics2D graphics, Rectangle bounds, int itemId, int quantity) {
        Color hoverColor = config.hoverColor();
        if (hoverColor == null) {
            return;
        }

        int opacity = config.enableFill() ? Math.min(255, Math.max(0, config.fillOpacity()))
                : DEFAULT_SELECTION_OPACITY;
        Color fillColor = new Color(hoverColor.getRed(), hoverColor.getGreen(), hoverColor.getBlue(), opacity);

        // Selection Flash ALWAYS uses Background Only Fill (zero outlines to avoid 3D
        // mesh conflicts)
        BufferedImage bgFilled = getBackgroundOnlyFilledSprite(itemId, quantity, fillColor);
        if (bgFilled != null) {
            graphics.drawImage(bgFilled, bounds.x, bounds.y, null);
        } else {
            graphics.setColor(fillColor);
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    private void renderHoverHighlight(Graphics2D graphics, Rectangle bounds, int itemId, int quantity, Color color) {
        Stroke oldStroke = graphics.getStroke();
        Color oldColor = graphics.getColor();

        // 1. Fill Rendering
        if (config.enableFill()) {
            renderFill(graphics, bounds, itemId, quantity, color, config.fillStyle(), config.fillOpacity());
        }

        // 2. Outline Rendering
        if (config.enableOutline()) {
            renderOutline(graphics, bounds, itemId, quantity, color, config.outlineStyle(), config.borderWidth());
        }

        graphics.setStroke(oldStroke);
        graphics.setColor(oldColor);
    }

    private void renderFill(Graphics2D graphics, Rectangle bounds, int itemId, int quantity, Color baseColor,
            FillStyle style, int rawOpacity) {
        int opacity = Math.min(255, Math.max(0, rawOpacity));
        Color fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), opacity);

        switch (style) {
            case ITEM_SILHOUETTE:
                BufferedImage spriteFilled = getFilledItemSprite(itemId, quantity, fillColor);
                if (spriteFilled != null) {
                    graphics.drawImage(spriteFilled, bounds.x, bounds.y, null);
                }
                break;

            case BACKGROUND:
                BufferedImage bgFilled = getBackgroundOnlyFilledSprite(itemId, quantity, fillColor);
                if (bgFilled != null) {
                    graphics.drawImage(bgFilled, bounds.x, bounds.y, null);
                }
                break;

            case BOX:
            default:
                graphics.setColor(fillColor);
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
        }
    }

    private void renderOutline(Graphics2D graphics, Rectangle bounds, int itemId, int quantity, Color color,
            OutlineStyle style, int rawWidth) {
        int borderWidth = Math.max(1, rawWidth);

        switch (style) {
            case ITEM_SILHOUETTE:
                renderSilhouetteOutline(graphics, bounds, itemId, quantity, color, borderWidth);
                break;

            case CORNER_BRACKETS:
                renderCornerBrackets(graphics, bounds, color, borderWidth);
                break;

            case BOX:
            default:
                renderBoxOutline(graphics, bounds, color, borderWidth);
                break;
        }
    }

    private void renderSilhouetteOutline(Graphics2D graphics, Rectangle bounds, int itemId, int quantity, Color color,
            int borderWidth) {
        BufferedImage outline = itemManager.getItemOutline(itemId, quantity, color);
        if (outline != null) {
            graphics.drawImage(outline, bounds.x, bounds.y, null);
            for (int b = 1; b < borderWidth; b++) {
                graphics.drawImage(outline, bounds.x - b, bounds.y, null);
                graphics.drawImage(outline, bounds.x + b, bounds.y, null);
                graphics.drawImage(outline, bounds.x, bounds.y - b, null);
                graphics.drawImage(outline, bounds.x, bounds.y + b, null);
            }
        }
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

    private BufferedImage getFilledItemSprite(int itemId, int quantity, Color fillColor) {
        String key = "silhouette_" + itemId + "_" + quantity + "_" + fillColor.getRGB();
        BufferedImage cached = spriteCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }

        BufferedImage itemImg = itemManager.getImage(itemId, quantity, false);
        if (itemImg == null) {
            return null;
        }

        int width = itemImg.getWidth();
        int height = itemImg.getHeight();
        BufferedImage filled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = filled.createGraphics();

        g.drawImage(itemImg, 0, 0, null);
        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(fillColor);
        g.fillRect(0, 0, width, height);
        g.dispose();

        spriteCache.put(key, filled);
        return filled;
    }

    private BufferedImage getBackgroundOnlyFilledSprite(int itemId, int quantity, Color fillColor) {
        String key = "bg_" + itemId + "_" + quantity + "_" + fillColor.getRGB();
        BufferedImage cached = spriteCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }

        BufferedImage itemImg = itemManager.getImage(itemId, quantity, false);
        if (itemImg == null) {
            return null;
        }

        int width = itemImg.getWidth();
        int height = itemImg.getHeight();
        BufferedImage buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buffer.createGraphics();

        g.setColor(fillColor);
        g.fillRect(0, 0, width, height);
        g.setComposite(AlphaComposite.DstOut);
        g.drawImage(itemImg, 0, 0, null);
        g.dispose();

        spriteCache.put(key, buffer);
        return buffer;
    }
}
