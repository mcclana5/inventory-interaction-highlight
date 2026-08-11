package com.inventoryhighlight;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InventoryHighlightRendererTest {
    private InventoryHighlightRenderer renderer;
    private InventoryHighlightConfig config;
    private ItemManager itemManager;

    private static final Color CYAN_BLUE = new Color(0, 255, 255, 200);

    @Before
    public void setUp() {
        renderer = new InventoryHighlightRenderer();
        config = mock(InventoryHighlightConfig.class);
        itemManager = mock(ItemManager.class);

        when(config.borderWidth()).thenReturn(1);
        when(config.fillOpacity()).thenReturn(65);
    }

    @Test
    public void testIsSpriteBasedStyles() {
        assertTrue(FillStyle.ITEM_SILHOUETTE.isSpriteBased());
        assertTrue(FillStyle.BACKGROUND.isSpriteBased());
        assertFalse(FillStyle.BOX.isSpriteBased());

        assertTrue(OutlineStyle.ITEM_SILHOUETTE.isSpriteBased());
        assertFalse(OutlineStyle.BOX.isSpriteBased());
        assertFalse(OutlineStyle.CORNER_BRACKETS.isSpriteBased());
    }

    @Test
    public void testRenderBoxOutline() {
        when(config.enableOutline()).thenReturn(true);
        when(config.outlineStyle()).thenReturn(OutlineStyle.BOX);
        when(config.enableFill()).thenReturn(false);

        Graphics2D graphics = createMockGraphics();
        Rectangle bounds = new Rectangle(0, 0, 36, 32);

        renderer.renderHighlight(graphics, bounds, 4151, 1, CYAN_BLUE, config, itemManager);

        verify(graphics).drawRect(0, 0, 35, 31);
        verify(graphics).setColor(CYAN_BLUE);
    }

    @Test
    public void testRenderCornerBracketsOutline() {
        when(config.enableOutline()).thenReturn(true);
        when(config.outlineStyle()).thenReturn(OutlineStyle.CORNER_BRACKETS);
        when(config.enableFill()).thenReturn(false);

        Graphics2D graphics = createMockGraphics();
        Rectangle bounds = new Rectangle(0, 0, 36, 32);

        renderer.renderHighlight(graphics, bounds, 4151, 1, CYAN_BLUE, config, itemManager);

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
        Rectangle bounds = new Rectangle(0, 0, 36, 32);

        renderer.renderHighlight(graphics, bounds, 4151, 1, CYAN_BLUE, config, itemManager);

        verify(itemManager).getItemOutline(eq(4151), eq(1), eq(CYAN_BLUE));
        verify(graphics).drawImage(eq(mockOutline), eq(0), eq(0), any());
    }

    @Test
    public void testRenderBoxFill() {
        when(config.enableOutline()).thenReturn(false);
        when(config.enableFill()).thenReturn(true);
        when(config.fillStyle()).thenReturn(FillStyle.BOX);
        when(config.fillOpacity()).thenReturn(100);

        Graphics2D graphics = createMockGraphics();
        Rectangle bounds = new Rectangle(0, 0, 36, 32);

        renderer.renderHighlight(graphics, bounds, 4151, 1, CYAN_BLUE, config, itemManager);

        verify(graphics).fillRect(0, 0, 36, 32);
    }

    @Test
    public void testRenderSilhouetteFill() {
        when(config.enableOutline()).thenReturn(false);
        when(config.enableFill()).thenReturn(true);
        when(config.fillStyle()).thenReturn(FillStyle.ITEM_SILHOUETTE);

        AsyncBufferedImage sampleItemImg = createSampleItemImage();
        when(itemManager.getImage(eq(4151), eq(1), eq(false))).thenReturn(sampleItemImg);

        Graphics2D graphics = createMockGraphics();
        Rectangle bounds = new Rectangle(0, 0, 36, 32);

        renderer.renderHighlight(graphics, bounds, 4151, 1, CYAN_BLUE, config, itemManager);

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
        Rectangle bounds = new Rectangle(0, 0, 36, 32);

        renderer.renderHighlight(graphics, bounds, 4151, 1, CYAN_BLUE, config, itemManager);

        verify(itemManager).getImage(eq(4151), eq(1), eq(false));
        verify(graphics).drawImage(any(BufferedImage.class), eq(0), eq(0), any());
    }

    @Test
    public void testPixelCanvasBoxFillVerification() {
        when(config.enableOutline()).thenReturn(false);
        when(config.enableFill()).thenReturn(true);
        when(config.fillStyle()).thenReturn(FillStyle.BOX);
        when(config.fillOpacity()).thenReturn(200);

        BufferedImage targetCanvas = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D realGraphics = targetCanvas.createGraphics();
        Rectangle bounds = new Rectangle(0, 0, 36, 32);

        renderer.renderHighlight(realGraphics, bounds, 4151, 1, CYAN_BLUE, config, itemManager);
        realGraphics.dispose();

        int centerPixelRgb = targetCanvas.getRGB(18, 16);
        Color renderedColor = new Color(centerPixelRgb, true);

        assertEquals(200, renderedColor.getAlpha());
        assertEquals(CYAN_BLUE.getRed(), renderedColor.getRed());
        assertEquals(CYAN_BLUE.getGreen(), renderedColor.getGreen());
        assertEquals(CYAN_BLUE.getBlue(), renderedColor.getBlue());
    }

    // ==========================================
    // SECTION: TEXT MASKING & CACHING TESTS
    // ==========================================

    @Test
    public void testTextMaskingCapabilityForStackedItems() {
        when(config.enableOutline()).thenReturn(false);
        when(config.enableFill()).thenReturn(true);
        when(config.fillStyle()).thenReturn(FillStyle.ITEM_SILHOUETTE);
        when(config.fillOpacity()).thenReturn(255);

        // Create base item image (solid white 16x12 square at center)
        AsyncBufferedImage baseImg = new AsyncBufferedImage(null, 36, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gBase = baseImg.createGraphics();
        gBase.setColor(Color.WHITE);
        gBase.fillRect(10, 10, 16, 12);
        gBase.dispose();

        // Create text item image (base item + yellow text pixel at x=5, y=5)
        AsyncBufferedImage textImg = new AsyncBufferedImage(null, 36, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gText = textImg.createGraphics();
        gText.drawImage(baseImg, 0, 0, null);
        gText.setColor(Color.YELLOW);
        gText.fillRect(5, 5, 2, 2); // Yellow quantity text at top-left
        gText.dispose();

        when(itemManager.getImage(eq(995), eq(1000), eq(false))).thenReturn(baseImg);
        when(itemManager.getImage(eq(995), eq(1000), eq(true))).thenReturn(textImg);

        BufferedImage targetCanvas = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D realGraphics = targetCanvas.createGraphics();
        Rectangle bounds = new Rectangle(0, 0, 36, 32);

        // Render highlight on 1,000 Coins (quantity > 1)
        renderer.renderHighlight(realGraphics, bounds, 995, 1000, CYAN_BLUE, config, itemManager);
        realGraphics.dispose();

        // Verify text pixel (5,5) is 100% transparent in output (alpha == 0)
        int textPixelRgb = targetCanvas.getRGB(5, 5);
        assertEquals(0, new Color(textPixelRgb, true).getAlpha());

        // Verify body pixel (15,15) contains the solid highlight color (alpha == 255)
        int bodyPixelRgb = targetCanvas.getRGB(15, 15);
        Color bodyColor = new Color(bodyPixelRgb, true);
        assertEquals(255, bodyColor.getAlpha());
        assertEquals(CYAN_BLUE.getRed(), bodyColor.getRed());
    }

    @Test
    public void testSpriteCacheCachingWorkingCorrectly() {
        when(config.enableOutline()).thenReturn(false);
        when(config.enableFill()).thenReturn(true);
        when(config.fillStyle()).thenReturn(FillStyle.ITEM_SILHOUETTE);

        AsyncBufferedImage sampleItemImg = createSampleItemImage();
        when(itemManager.getImage(eq(4151), eq(1), eq(false))).thenReturn(sampleItemImg);

        Graphics2D graphics = createMockGraphics();
        Rectangle bounds = new Rectangle(0, 0, 36, 32);

        // First render -> Cache Miss -> Calls itemManager.getImage ONCE
        renderer.renderHighlight(graphics, bounds, 4151, 1, CYAN_BLUE, config, itemManager);
        verify(itemManager, times(1)).getImage(eq(4151), eq(1), eq(false));

        // Second render -> Cache HIT -> Uses spriteCache, does NOT call
        // itemManager.getImage again!
        renderer.renderHighlight(graphics, bounds, 4151, 1, CYAN_BLUE, config, itemManager);
        verify(itemManager, times(1)).getImage(eq(4151), eq(1), eq(false));
    }

    @Test
    public void testMaskCacheCachingWorkingCorrectly() {
        when(config.enableOutline()).thenReturn(false);
        when(config.enableFill()).thenReturn(true);
        when(config.fillStyle()).thenReturn(FillStyle.ITEM_SILHOUETTE);

        AsyncBufferedImage baseImg = createSampleItemImage();
        AsyncBufferedImage textImg = createSampleItemImage();

        when(itemManager.getImage(eq(995), eq(1000), eq(false))).thenReturn(baseImg);
        when(itemManager.getImage(eq(995), eq(1000), eq(true))).thenReturn(textImg);

        Graphics2D graphics = createMockGraphics();
        Rectangle bounds = new Rectangle(0, 0, 36, 32);

        // Render Silhouette Fill -> Cache Miss for sprite & mask -> Calculates mask
        // once
        renderer.renderHighlight(graphics, bounds, 995, 1000, CYAN_BLUE, config, itemManager);
        verify(itemManager, times(1)).getImage(eq(995), eq(1000), eq(false));

        // Switch to Background Fill with different color -> Cache Miss for sprite, but
        // Cache HIT for maskCache!
        when(config.fillStyle()).thenReturn(FillStyle.BACKGROUND);
        renderer.renderHighlight(graphics, bounds, 995, 1000, Color.RED, config, itemManager);

        // baseImg was NOT fetched again because maskCache served the pre-calculated
        // mask!
        verify(itemManager, times(1)).getImage(eq(995), eq(1000), eq(false));
    }

    private Graphics2D createMockGraphics() {
        Graphics2D g = mock(Graphics2D.class);
        when(g.getColor()).thenReturn(Color.BLACK);
        when(g.getStroke()).thenReturn(new BasicStroke());
        return g;
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
