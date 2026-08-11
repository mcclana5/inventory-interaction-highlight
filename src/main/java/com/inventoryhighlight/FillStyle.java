package com.inventoryhighlight;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FillStyle {
    BOX("Box", false),
    BACKGROUND("Background Only", true),
    ITEM_SILHOUETTE("Item Silhouette", true);

    private final String name;
    private final boolean spriteBased;

    @Override
    public String toString() {
        return name;
    }
}
