package com.inventoryhighlight;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InventoryHighlightIgnoredItemsTest {
    private Client client;
    private InventoryHighlightConfig config;
    private ItemManager itemManager;
    private InventoryHighlightRenderer renderer;
    private InventoryHighlightOverlay overlay;

    @Before
    public void setUp() {
        client = mock(Client.class);
        config = mock(InventoryHighlightConfig.class);
        itemManager = mock(ItemManager.class);
        renderer = mock(InventoryHighlightRenderer.class);
        overlay = new InventoryHighlightOverlay(client, config, itemManager, renderer);
    }

    @Test
    public void testIsItemIgnoredExactMatch() {
        when(config.ignoredItems()).thenReturn("Coins, Shark, Lobsters");
        overlay.updateIgnoredItemsCache();

        assertTrue(overlay.isItemIgnored("Coins"));
        assertTrue(overlay.isItemIgnored("coins"));
        assertTrue(overlay.isItemIgnored("SHARK"));
        assertFalse(overlay.isItemIgnored("Dragon dagger"));
    }

    @Test
    public void testIsItemIgnoredWildcards() {
        when(config.ignoredItems()).thenReturn("*bones, Rune *, * dagger, Grimy ranarr weed");
        overlay.updateIgnoredItemsCache();

        assertTrue(overlay.isItemIgnored("Bones"));
        assertTrue(overlay.isItemIgnored("Dragon bones"));
        assertTrue(overlay.isItemIgnored("Big bones"));
        assertTrue(overlay.isItemIgnored("Rune scimitar"));
        assertTrue(overlay.isItemIgnored("Rune platebody"));
        assertTrue(overlay.isItemIgnored("Dragon dagger"));
        assertTrue(overlay.isItemIgnored("Grimy ranarr weed"));
        assertFalse(overlay.isItemIgnored("Coins"));
    }

    @Test
    public void testIsItemIgnoredEmptyList() {
        when(config.ignoredItems()).thenReturn("   , , ");
        overlay.updateIgnoredItemsCache();
        assertFalse(overlay.isItemIgnored("Coins"));
    }

    @Test
    public void testRenderItemOverlaySuppressesIgnoredItem() {
        when(config.enableIgnoredItems()).thenReturn(true);
        when(config.ignoredItems()).thenReturn("Coins");
        overlay.updateIgnoredItemsCache();
        when(config.highlightHover()).thenReturn(true);

        int itemId = 995;
        ItemComposition itemComp = mock(ItemComposition.class);
        when(itemComp.getName()).thenReturn("Coins");
        when(itemManager.getItemComposition(itemId)).thenReturn(itemComp);

        Widget widget = mock(Widget.class);
        when(widget.getParentId()).thenReturn(100);
        WidgetItem itemWidget = mock(WidgetItem.class);
        when(itemWidget.getWidget()).thenReturn(widget);

        Graphics2D graphics = mock(Graphics2D.class);

        overlay.renderItemOverlay(graphics, itemId, itemWidget);

        verify(renderer, never()).renderHighlight(any(), any(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    public void testRenderItemOverlayRendersNonIgnoredItem() {
        when(config.enableIgnoredItems()).thenReturn(true);
        when(config.ignoredItems()).thenReturn("Coins");
        overlay.updateIgnoredItemsCache();
        when(config.highlightHover()).thenReturn(true);
        when(config.hoverColor()).thenReturn(new java.awt.Color(0, 255, 255, 200));

        int itemId = 4151;
        ItemComposition itemComp = mock(ItemComposition.class);
        when(itemComp.getName()).thenReturn("Abyssal whip");
        when(itemManager.getItemComposition(itemId)).thenReturn(itemComp);

        Widget widget = mock(Widget.class);
        when(widget.getParentId()).thenReturn(100);
        when(widget.getIndex()).thenReturn(0);
        WidgetItem itemWidget = mock(WidgetItem.class);
        when(itemWidget.getWidget()).thenReturn(widget);
        Rectangle bounds = new Rectangle(10, 10, 32, 32);
        when(itemWidget.getCanvasBounds()).thenReturn(bounds);

        Point mousePoint = new Point(15, 15);
        when(client.getMouseCanvasPosition()).thenReturn(mousePoint);

        Graphics2D graphics = mock(Graphics2D.class);

        overlay.renderItemOverlay(graphics, itemId, itemWidget);

        verify(renderer).renderHighlight(any(), any(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    public void testOnConfigChangedUpdatesIgnoredItemsCache() {
        when(config.ignoredItems()).thenReturn("Coins");
        overlay.updateIgnoredItemsCache();
        assertTrue(overlay.isItemIgnored("Coins"));

        // Simulate config change event
        when(config.ignoredItems()).thenReturn("Shark");
        ConfigChanged event = new ConfigChanged();
        event.setGroup("inventoryhighlight");
        event.setKey("ignoredItems");
        overlay.onConfigChanged(event);

        assertFalse(overlay.isItemIgnored("Coins"));
        assertTrue(overlay.isItemIgnored("Shark"));
    }
}
