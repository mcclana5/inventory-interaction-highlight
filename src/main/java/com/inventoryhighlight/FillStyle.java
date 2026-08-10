package com.inventoryhighlight;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FillStyle {
    BOX("Box"),
    BACKGROUND("Background Only"),
    ITEM_SILHOUETTE("Item Silhouette");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
