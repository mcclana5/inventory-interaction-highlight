package com.inventoryhighlight;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OutlineStyle {
    BOX("Box"),
    CORNER_BRACKETS("Corner Brackets"),
    ITEM_SILHOUETTE("Item Silhouette");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
