package com.inventoryhighlight;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("inventoryhighlight")
public interface InventoryHighlightConfig extends Config {
    @ConfigSection(name = "General Settings", description = "General color settings", position = 1)
    String generalSection = "generalSection";

    @ConfigSection(name = "Hover Settings", description = "Hover highlight toggle, outlines, and fill appearance", position = 2)
    String hoverSection = "hoverSection";

    @ConfigSection(name = "Interaction Settings", description = "Configuration for active click press feedback, item selection flashing, and drop action highlighting", position = 3)
    String interactionSection = "interactionSection";

    @ConfigSection(name = "Bank Interface Settings", description = "Configuration for highlighting items inside the bank vault and bank inventory panel", position = 4)
    String bankSection = "bankSection";

    // GENERAL SETTINGS
    @Alpha
    @ConfigItem(keyName = "hoverColor", name = "Highlight Color", description = "Main color used for hover highlights, active click feedback, and selection flashing", section = generalSection, position = 1)
    default Color hoverColor() {
        return new Color(0, 255, 255, 200); // Cyan Blue
    }

    // HOVER SETTINGS (Toggle + Outline + Fill)
    @ConfigItem(keyName = "highlightHover", name = "Enable Hover Highlight", description = "Toggles highlight overlay when hovering over inventory items", section = hoverSection, position = 1)
    default boolean highlightHover() {
        return true;
    }

    @ConfigItem(keyName = "enableOutline", name = "Enable Outline", description = "Toggles rendering of the outline border", section = hoverSection, position = 2)
    default boolean enableOutline() {
        return true;
    }

    @ConfigItem(keyName = "outlineStyle", name = "Outline Style", description = "Visual style of the outline border (Box, Corner Brackets, or Item Silhouette)", section = hoverSection, position = 3)
    default OutlineStyle outlineStyle() {
        return OutlineStyle.ITEM_SILHOUETTE;
    }

    @Range(min = 1, max = 5)
    @ConfigItem(keyName = "borderWidth", name = "Outline Width", description = "Thickness of the outline border in pixels (1 - 5)", section = hoverSection, position = 4)
    default int borderWidth() {
        return 1;
    }

    @ConfigItem(keyName = "enableFill", name = "Enable Fill", description = "Toggles rendering of the background fill", section = hoverSection, position = 5)
    default boolean enableFill() {
        return false;
    }

    @ConfigItem(keyName = "fillStyle", name = "Fill Style", description = "Visual style of the background fill (Box, Background Only, or Item Silhouette)", section = hoverSection, position = 6)
    default FillStyle fillStyle() {
        return FillStyle.ITEM_SILHOUETTE;
    }

    @Range(min = 10, max = 255)
    @ConfigItem(keyName = "fillOpacity", name = "Fill Opacity", description = "Opacity level for the background fill (10 - 255)", section = hoverSection, position = 7)
    default int fillOpacity() {
        return 65;
    }

    // INTERACTION SETTINGS (Click, Selection, & Drop)
    @ConfigItem(keyName = "highlightClick", name = "Enable Click Press Feedback", description = "Slightly insets (1px) and brightens the hover highlight during mouse press down for tactile click feedback", section = interactionSection, position = 1)
    default boolean highlightClick() {
        return true;
    }

    @ConfigItem(keyName = "enableSelectionFlash", name = "Enable Selection Flash", description = "Rhythmically flashes a background-only fill on and off once per game tick when an item is selected ('Use')", section = interactionSection, position = 2)
    default boolean enableSelectionFlash() {
        return true;
    }

    @ConfigItem(keyName = "enableDropHighlight", name = "Enable Drop Highlight", description = "Changes the highlight color when the default left-click action is 'Drop' (e.g. while holding Shift)", section = interactionSection, position = 3)
    default boolean enableDropHighlight() {
        return false;
    }

    @Alpha
    @ConfigItem(keyName = "dropColor", name = "Drop Color", description = "Color of the highlight overlay when the default left-click action is 'Drop'", section = interactionSection, position = 4)
    default Color dropColor() {
        return new Color(255, 50, 50, 200); // Vibrant Red
    }

    // BANK INTERFACE SETTINGS
    @ConfigItem(keyName = "highlightBank", name = "Enable Bank Highlight", description = "Toggles highlight overlay when hovering over items in the main bank vault or bank inventory panel", section = bankSection, position = 1)
    default boolean highlightBank() {
        return false;
    }

    @ConfigItem(keyName = "highlightBankPlaceholders", name = "Highlight Placeholders", description = "Whether to highlight empty bank placeholder items when bank highlighting is enabled", section = bankSection, position = 2)
    default boolean highlightBankPlaceholders() {
        return false;
    }
}
