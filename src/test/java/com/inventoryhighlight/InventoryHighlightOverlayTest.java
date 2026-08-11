package com.inventoryhighlight;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InventoryHighlightOverlayTest {
    private Client client;
    private Menu menu;
    private InventoryHighlightConfig config;
    private ItemManager itemManager;
    private InventoryHighlightRenderer renderer;
    private InventoryHighlightOverlay overlay;

    private static final Color CYAN_BLUE = new Color(0, 255, 255, 200);
    private static final Color VIBRANT_RED = new Color(255, 50, 50, 200);

    @Before
    public void setUp() {
        client = mock(Client.class);
        menu = mock(Menu.class);
        config = mock(InventoryHighlightConfig.class);
        itemManager = mock(ItemManager.class);
        renderer = mock(InventoryHighlightRenderer.class);

        // Configure default mock behaviors
        when(client.getMenu()).thenReturn(menu);
        when(config.hoverColor()).thenReturn(CYAN_BLUE);
        when(config.dropColor()).thenReturn(VIBRANT_RED);
        when(config.highlightHover()).thenReturn(true);
        when(config.highlightClick()).thenReturn(true);
        when(config.enableSelectionFlash()).thenReturn(true);

        overlay = new InventoryHighlightOverlay(client, config, itemManager, renderer);
    }

    // ==========================================
    // SECTION 1: SHIFT-DROP & COLOR SELECTION TESTS
    // ==========================================

    @Test
    public void testShiftDropColorSelectedWhenTopMenuOptionIsDrop() {
        when(config.enableDropHighlight()).thenReturn(true);

        MenuEntry dropEntry = mock(MenuEntry.class);
        when(dropEntry.getOption()).thenReturn("Drop");
        when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{dropEntry});

        Color activeColor = overlay.getActiveHighlightColor();
        assertEquals(VIBRANT_RED, activeColor);
    }

    @Test
    public void testHoverColorSelectedWhenTopMenuOptionIsNotDrop() {
        when(config.enableDropHighlight()).thenReturn(true);

        MenuEntry examineEntry = mock(MenuEntry.class);
        when(examineEntry.getOption()).thenReturn("Examine");
        when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{examineEntry});

        Color activeColor = overlay.getActiveHighlightColor();
        assertEquals(CYAN_BLUE, activeColor);
    }

    @Test
    public void testHoverColorSelectedWhenDropHighlightDisabled() {
        when(config.enableDropHighlight()).thenReturn(false);

        MenuEntry dropEntry = mock(MenuEntry.class);
        when(dropEntry.getOption()).thenReturn("Drop");
        when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{dropEntry});

        Color activeColor = overlay.getActiveHighlightColor();
        assertEquals(CYAN_BLUE, activeColor);
    }

    // ==========================================
    // SECTION 2: CONTROLLER & EVENT HANDLING TESTS
    // ==========================================

    @Test
    public void testHoverDelegatesToRenderer() {
        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        // Verify overlay delegates to renderer.renderHighlight
        verify(renderer).renderHighlight(eq(graphics), eq(new Rectangle(0, 0, 36, 32)), eq(4151), eq(1), eq(CYAN_BLUE), eq(config), eq(itemManager));
    }

    @Test
    public void testClickFeedbackInsetsBoundsAndBoostsAlpha() {
        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        // Simulate active mouse press down (Button 1)
        when(client.getMouseCurrentButton()).thenReturn(1);

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        // 1px inset bounds (0, 0, 36, 32) -> click bounds (1, 1, 34, 30)
        Rectangle expectedClickBounds = new Rectangle(1, 1, 34, 30);

        ArgumentCaptor<Color> colorCaptor = ArgumentCaptor.forClass(Color.class);
        verify(renderer).renderHighlight(eq(graphics), eq(expectedClickBounds), eq(4151), eq(1), colorCaptor.capture(), eq(config), eq(itemManager));

        // Alpha boosted from 200 to 200 + 70 = 270 (clamped to 255)
        assertEquals(255, colorCaptor.getValue().getAlpha());
    }

    @Test
    public void testSelectionFlashRendersWhenSelected() {
        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);
        Widget widget = itemWidget.getWidget();

        // Simulate item selected ("Use Item -> ...")
        when(client.isWidgetSelected()).thenReturn(true);
        when(client.getSelectedWidget()).thenReturn(widget);

        // Fire game tick to set tick start timestamp
        overlay.onGameTick(new GameTick());

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        // Selection flash delegates to renderer.renderSelectionHighlight
        verify(renderer).renderSelectionHighlight(eq(graphics), eq(new Rectangle(0, 0, 36, 32)), eq(4151), eq(1), eq(CYAN_BLUE), eq(config), eq(itemManager));
    }

    @Test
    public void testDraggingWidgetSuppressesHighlightOnOtherSlots() {
        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        Widget draggedWidget = mock(Widget.class);
        when(draggedWidget.getIndex()).thenReturn(5); // Dragging slot #5

        // Hovering over slot #0 while dragging slot #5
        when(client.getMouseCurrentButton()).thenReturn(1);
        when(client.isDraggingWidget()).thenReturn(true);
        when(client.getDraggedWidget()).thenReturn(draggedWidget);

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        // Drawing is suppressed on slot #0 while slot #5 is dragged
        verify(renderer, never()).renderHighlight(any(), any(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    public void testHighlightHoverDisabledSuppressesOverlay() {
        when(config.highlightHover()).thenReturn(false);

        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        // No drawing occurs when highlightHover is disabled
        verify(renderer, never()).renderHighlight(any(), any(), anyInt(), anyInt(), any(), any(), any());
    }

    // ==========================================
    // HELPER MOCK METHODS
    // ==========================================

    private Graphics2D createMockGraphics() {
        Graphics2D g = mock(Graphics2D.class);
        when(g.getColor()).thenReturn(Color.BLACK);
        when(g.getStroke()).thenReturn(new BasicStroke());
        return g;
    }

    private WidgetItem createMockWidgetItem(int x, int y, int width, int height) {
        WidgetItem itemWidget = mock(WidgetItem.class);
        Widget widget = mock(Widget.class);
        Rectangle bounds = new Rectangle(x, y, width, height);

        when(itemWidget.getWidget()).thenReturn(widget);
        when(itemWidget.getCanvasBounds()).thenReturn(bounds);
        when(itemWidget.getQuantity()).thenReturn(1);
        when(widget.getIndex()).thenReturn(0);

        Point mousePos = new Point(x + width / 2, y + height / 2);
        when(client.getMouseCanvasPosition()).thenReturn(mousePos);
        when(client.getMouseCurrentButton()).thenReturn(0);

        return itemWidget;
    }
}
