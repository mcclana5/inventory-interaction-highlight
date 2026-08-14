package com.inventoryhighlight;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public class InventoryHighlightOverlay extends WidgetItemOverlay {
    private static final long FLASH_HALF_TICK_MS = 300L;
    private static final int CLICK_PRESS_ALPHA_BOOST = 70;
    private static final int CLICK_PRESS_INSET_PX = 1;

    private final Client client;
    private final InventoryHighlightConfig config;
    private final ItemManager itemManager;
    private final InventoryHighlightRenderer renderer;

    private long lastGameTickTime = 0;

    @Inject
    public InventoryHighlightOverlay(Client client, InventoryHighlightConfig config, ItemManager itemManager,
                                    InventoryHighlightRenderer renderer) {
        this.client = client;
        this.config = config;
        this.itemManager = itemManager;
        this.renderer = renderer;
        showOnInventory();
        showOnBank();
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

        boolean isBankItem = isBankWidget(itemWidget.getWidget());

        // Check Bank Interface configuration settings
        if (isBankItem) {
            if (!config.highlightBank()) {
                return;
            }
            boolean isPlaceholder = itemWidget.getQuantity() == 0;
            if (isPlaceholder && !config.highlightBankPlaceholders()) {
                return;
            }
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
                // Game Tick Synchronized Flash Cycle: ON for first 300ms of game tick, OFF for second 300ms
                long timeInTick = lastGameTickTime > 0 ? (System.currentTimeMillis() - lastGameTickTime) : 0;
                boolean flashVisible = (timeInTick < FLASH_HALF_TICK_MS);

                if (flashVisible) {
                    renderer.renderSelectionHighlight(graphics, bounds, itemId, itemWidget.getQuantity(),
                        config.hoverColor(), config, itemManager);
                }
            }
            return;
        }

        // 3. Right-click menu locking and suppression while menu remains open
        if (client.isMenuOpen()) {
            Menu menu = client.getMenu();
            if (menu != null) {
                MenuEntry[] entries = menu.getMenuEntries();
                if (entries != null && entries.length > 0) {
                    int targetSlot = getRightClickedInventorySlot(entries, itemWidget.getWidget().getParentId());
                    if (targetSlot >= 0) {
                        if (itemWidget.getWidget().getIndex() == targetSlot) {
                            Color activeColor = getActiveHighlightColor();
                            if (activeColor != null) {
                                renderer.renderHighlight(graphics, bounds, itemId, itemWidget.getQuantity(), activeColor, config, itemManager);
                            }
                        }
                        return; // Suppress hover highlight on all other inventory/bank slots while menu is open
                    }
                }
            }
            return; // Suppress hover highlight while any non-inventory/bank menu is open
        }

        // 4. Hover and active click highlight
        if (!isBankItem && !config.highlightHover()) {
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
                Math.max(1, bounds.height - (CLICK_PRESS_INSET_PX * 2))
            );

            // Boost alpha for brighter/more solid click press color
            int pressAlpha = Math.min(255, activeColor.getAlpha() + CLICK_PRESS_ALPHA_BOOST);
            Color pressColor = new Color(activeColor.getRed(), activeColor.getGreen(), activeColor.getBlue(), pressAlpha);

            renderer.renderHighlight(graphics, clickBounds, itemId, itemWidget.getQuantity(), pressColor, config, itemManager);
        } else {
            renderer.renderHighlight(graphics, bounds, itemId, itemWidget.getQuantity(), activeColor, config, itemManager);
        }
    }

    private boolean isBankWidget(Widget widget) {
        if (widget == null) {
            return false;
        }
        int parentId = widget.getParentId();
        return parentId == ComponentID.BANK_ITEM_CONTAINER || parentId == ComponentID.BANK_INVENTORY_ITEM_CONTAINER;
    }

    private int getRightClickedInventorySlot(MenuEntry[] entries, int containerParentId) {
        for (int i = entries.length - 1; i >= 0; i--) {
            MenuEntry entry = entries[i];
            if (entry != null && entry.getWidget() != null && entry.getWidget().getId() == containerParentId) {
                return entry.getParam0(); // param0 is item slot index
            }
        }
        return -1;
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
}
