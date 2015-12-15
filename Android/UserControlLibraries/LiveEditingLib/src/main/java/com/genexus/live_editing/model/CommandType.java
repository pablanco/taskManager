package com.genexus.live_editing.model;

import com.google.gson.annotations.SerializedName;

public enum CommandType {
    /* Server to Client commands */
    @SerializedName("ThemeStyleChanged")
    THEME_STYLE_CHANGED,

    @SerializedName("ThemeTransformChanged")
    THEME_TRANSFORMATION_CHANGED,

    @SerializedName("TranslationChanged")
    TRANSLATION_CHANGED,

    @SerializedName("LayoutChanged")
    LAYOUT_CHANGED,

    @SerializedName("ThemeColorChanged")
    THEME_COLOR_CHANGED,

    @SerializedName("ImageChanged")
    IMAGE_CHANGED,

    @SerializedName("InspectUI")
    INSPECT_UI,

    /* Client to Server commands */
    @SerializedName("MasterLayout")
    MASTER_LAYOUT,

    @SerializedName("NoOp")
    NO_OP
}
