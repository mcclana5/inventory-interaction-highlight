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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InventoryHighlightOverlayTest {
    private Client client;
    private Menu menu;
    private InventoryHighlightConfig config;
    private ItemManager itemManager;
    private InventoryHighlightOverlay overlay;

    private static final Color CYAN_BLUE = new Color(0, 255, 255, 200);
    private static final Color VIBRANT_RED = new Color(255, 50, 50, 200);

    @Before
    public void setUp() {
        client = mock(Client.class);
        menu = mock(Menu.class);
        config = mock(InventoryHighlightConfig.class);
        itemManager = mock(ItemManager.class);

        // Configure default mock behaviors
        when(client.getMenu()).thenReturn(menu);
        when(config.hoverColor()).thenReturn(CYAN_BLUE);
        when(config.dropColor()).thenReturn(VIBRANT_RED);
        when(config.highlightHover()).thenReturn(true);
        when(config.highlightClick()).thenReturn(true);
        when(config.enableSelectionFlash()).thenReturn(true);
        when(config.borderWidth()).thenReturn(1);
        when(config.fillOpacity()).thenReturn(65);

        overlay = new InventoryHighlightOverlay(client, config, itemManager);
    }

    // ==========================================
    // SECTION 1: SHIFT-DROP & COLOR TESTS
    // ==========================================

    @Test
    public void testShiftDropColorSelectedWhenTopMenuOptionIsDrop() {
        when(config.enableDropHighlight()).thenReturn(true);

        MenuEntry dropEntry = mock(MenuEntry.class);
        when(dropEntry.getOption()).thenReturn("Drop");
        when(menu.getMenuEntries()).thenReturn(new MenuEntry[] { dropEntry });

        Color activeColor = overlay.getActiveHighlightColor();
        assertEquals(VIBRANT_RED, activeColor);
    }

    @Test
    public void testHoverColorSelectedWhenTopMenuOptionIsNotDrop() {
        when(config.enableDropHighlight()).thenReturn(true);

        MenuEntry examineEntry = mock(MenuEntry.class);
        when(examineEntry.getOption()).thenReturn("Examine");
        when(menu.getMenuEntries()).thenReturn(new MenuEntry[] { examineEntry });

        Color activeColor = overlay.getActiveHighlightColor();
        assertEquals(CYAN_BLUE, activeColor);
    }

    @Test
    public void testHoverColorSelectedWhenDropHighlightDisabled() {
        when(config.enableDropHighlight()).thenReturn(false);

        MenuEntry dropEntry = mock(MenuEntry.class);
        when(dropEntry.getOption()).thenReturn("Drop");
        when(menu.getMenuEntries()).thenReturn(new MenuEntry[] { dropEntry });

        Color activeColor = overlay.getActiveHighlightColor();
        assertEquals(CYAN_BLUE, activeColor);
    }

    // ==========================================
    // SECTION 2: OUTLINE DRAWING VERIFICATION TESTS
    // ==========================================

    @Test
    public void testRenderBoxOutline() {
        when(config.enableOutline()).thenReturn(true);
        when(config.outlineStyle()).thenReturn(OutlineStyle.BOX);
        when(config.enableFill()).thenReturn(false);

        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        // Verify drawRect was called to draw the box outline
        verify(graphics).drawRect(0, 0, 35, 31);
        verify(graphics).setColor(CYAN_BLUE);
    }

    @Test
    public void testRenderCornerBracketsOutline() {
        when(config.enableOutline()).thenReturn(true);
        when(config.outlineStyle()).thenReturn(OutlineStyle.CORNER_BRACKETS);
        when(config.enableFill()).thenReturn(false);

        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        // Corner brackets draw 2 lines per corner = 8 lines total
        verify(graphics, times(8)).drawLine(anyInt(), anyInt(), anyInt(), anyInt());
        verify(graphics).setColor(CYAN_BLUE);
    }

    @Test
    public void testRenderSilhouetteOutline() {
        when(config.enableOutline()).thenReturn(true);
        when(config.outlineStyle()).thenReturn(OutlineStyle.ITEM_SILHOUETTE);
        when(config.enableFill()).thenReturn(false);

        BufferedImage mockOutline = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
        when(itemManager.getItemOutline(eq(4151), eq(1), eq(CYAN_BLUE))).thenReturn(mockOutline);

        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        // Verify outline was requested from ItemManager and drawn via drawImage
        verify(itemManager).getItemOutline(eq(4151), eq(1), eq(CYAN_BLUE));
        verify(graphics).drawImage(eq(mockOutline), eq(0), eq(0), any());
    }

    // ==========================================
    // SECTION 3: FILL DRAWING VERIFICATION TESTS
    // ==========================================

    @Test
    public void testRenderBoxFill() {
        when(config.enableOutline()).thenReturn(false);
        when(config.enableFill()).thenReturn(true);
        when(config.fillStyle()).thenReturn(FillStyle.BOX);
        when(config.fillOpacity()).thenReturn(100);

        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        // Verify fillRect was called with item bounds
        verify(graphics).fillRect(0, 0, 36, 32);

        // Verify fill color set with opacity 100
        ArgumentCaptor<Color> colorCaptor = ArgumentCaptor.forClass(Color.class);
        verify(graphics, atLeastOnce()).setColor(colorCaptor.capture());

        boolean foundOpacityColor = colorCaptor.getAllValues().stream()
                .anyMatch(c -> c != null && c.getAlpha() == 100);
        assertTrue(foundOpacityColor);
    }

    @Test
    public void testRenderSilhouetteFill() {
        when(config.enableOutline()).thenReturn(false);
        when(config.enableFill()).thenReturn(true);
        when(config.fillStyle()).thenReturn(FillStyle.ITEM_SILHOUETTE);

        AsyncBufferedImage sampleItemImg = createSampleItemImage();
        when(itemManager.getImage(eq(4151), eq(1), eq(false))).thenReturn(sampleItemImg);

        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        verify(itemManager).getImage(eq(4151), eq(1), eq(false));
        verify(graphics).drawImage(any(BufferedImage.class), eq(0), eq(0), any());
    }

    @Test
    public void testRenderBackgroundFill() {
        when(config.enableOutline()).thenReturn(false);
        when(config.enableFill()).thenReturn(true);
        when(config.fillStyle()).thenReturn(FillStyle.BACKGROUND);

        AsyncBufferedImage sampleItemImg = createSampleItemImage();
        when(itemManager.getImage(eq(4151), eq(1), eq(false))).thenReturn(sampleItemImg);

        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        verify(itemManager).getImage(eq(4151), eq(1), eq(false));
        verify(graphics).drawImage(any(BufferedImage.class), eq(0), eq(0), any());
    }

    // ==========================================
    // SECTION 4: CLICK FEEDBACK & INTERACTION TESTS
    // ==========================================

    @Test
    public void testClickFeedbackInsetsBoundsAndBoostsAlpha() {
        when(config.enableOutline()).thenReturn(true);
        when(config.outlineStyle()).thenReturn(OutlineStyle.BOX);
        when(config.enableFill()).thenReturn(false);

        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        // Simulate active mouse press down (Button 1)
        when(client.getMouseCurrentButton()).thenReturn(1);

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        // 1px inset of bounds (0, 0, 36, 32) -> inset bounds (1, 1, 34, 30) ->
        // drawRect(1, 1, 33, 29)
        verify(graphics).drawRect(1, 1, 33, 29);

        // Alpha boosted from 200 to 200 + 70 = 270 (clamped to 255)
        ArgumentCaptor<Color> colorCaptor = ArgumentCaptor.forClass(Color.class);
        verify(graphics, atLeastOnce()).setColor(colorCaptor.capture());

        boolean foundBoostedColor = colorCaptor.getAllValues().stream()
                .anyMatch(c -> c != null && c.getAlpha() == 255);
        assertTrue(foundBoostedColor);
    }

    @Test
    public void testSelectionFlashRendersWhenSelected() {
        AsyncBufferedImage sampleItemImg = createSampleItemImage();
        when(itemManager.getImage(eq(4151), eq(1), eq(false))).thenReturn(sampleItemImg);

        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);
        Widget widget = itemWidget.getWidget();

        // Simulate item selected ("Use Item -> ...")
        when(client.isWidgetSelected()).thenReturn(true);
        when(client.getSelectedWidget()).thenReturn(widget);

        // Fire game tick to set tick start timestamp
        overlay.onGameTick(new GameTick());

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        // Selection flash renders background-only fill image
        verify(graphics).drawImage(any(BufferedImage.class), eq(0), eq(0), any());
    }

    @Test
    public void testDraggingWidgetSuppressesHighlightOnOtherSlots() {
        when(config.enableOutline()).thenReturn(true);
        when(config.outlineStyle()).thenReturn(OutlineStyle.BOX);

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
        verify(graphics, never()).drawRect(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void testHighlightHoverDisabledSuppressesOverlay() {
        when(config.highlightHover()).thenReturn(false);
        when(config.enableOutline()).thenReturn(true);

        Graphics2D graphics = createMockGraphics();
        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        overlay.renderItemOverlay(graphics, 4151, itemWidget);

        // No drawing occurs when highlightHover is disabled
        verify(graphics, never()).drawRect(anyInt(), anyInt(), anyInt(), anyInt());
    }

    // ==========================================
    // SECTION 5: PIXEL CANVAS VERIFICATION TEST
    // ==========================================

    @Test
    public void testPixelCanvasBoxFillVerification() {
        when(config.enableOutline()).thenReturn(false);
        when(config.enableFill()).thenReturn(true);
        when(config.fillStyle()).thenReturn(FillStyle.BOX);
        when(config.fillOpacity()).thenReturn(200);

        BufferedImage targetCanvas = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D realGraphics = targetCanvas.createGraphics();

        WidgetItem itemWidget = createMockWidgetItem(0, 0, 36, 32);

        overlay.renderItemOverlay(realGraphics, 4151, itemWidget);
        realGraphics.dispose();

        int centerPixelRgb = targetCanvas.getRGB(18, 16);
        Color renderedColor = new Color(centerPixelRgb, true);

        assertEquals(200, renderedColor.getAlpha());
        assertEquals(CYAN_BLUE.getRed(), renderedColor.getRed());
        assertEquals(CYAN_BLUE.getGreen(), renderedColor.getGreen());
        assertEquals(CYAN_BLUE.getBlue(), renderedColor.getBlue());
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

    private AsyncBufferedImage createSampleItemImage() {
        AsyncBufferedImage img = new AsyncBufferedImage(null, 36, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(10, 10, 16, 12);
        g.dispose();
        return img;
    }
}
