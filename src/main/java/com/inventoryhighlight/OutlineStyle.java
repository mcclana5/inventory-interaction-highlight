package com.inventoryhighlight;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OutlineStyle {
    BOX("Box", false),
    CORNER_BRACKETS("Corner Brackets", false),
    ITEM_SILHOUETTE("Item Silhouette", true);

    private final String name;
    private final boolean spriteBased;

    @Override
    public String toString() {
        return name;
    }
}
