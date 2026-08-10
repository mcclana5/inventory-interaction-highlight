package com.inventoryhighlight;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
    name = "Inventory Interaction Highlight",
    description = "Highlight inventory items during hover, active click, and selection states",
    tags = {"inventory", "item", "hover", "click", "highlight", "overlay", "selection"}
)
public class InventoryHighlightPlugin extends Plugin {
    @Inject
    private OverlayManager overlayManager;

    @Inject
    private EventBus eventBus;

    @Inject
    private InventoryHighlightOverlay overlay;

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
        eventBus.register(overlay);
        log.info("Inventory Interaction Highlight plugin started successfully.");
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(overlay);
        overlayManager.remove(overlay);
        log.info("Inventory Interaction Highlight plugin stopped.");
    }

    @Provides
    InventoryHighlightConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(InventoryHighlightConfig.class);
    }
}
