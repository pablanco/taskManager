package com.genexus.live_editing.util;

public class Intents {
    private static final String NAMESPACE = "com.genexus.live_editing";
    public static final String EVENT_COMMAND_RECEIVED = NAMESPACE + ".COMMAND_RECEIVED";
    public static final String ACTION_CONNECT = NAMESPACE + ".ACTION_CONNECT";

    public static final String EXTRA_COMMAND_TYPE = "COMMAND_TYPE";
    public static final String EXTRA_OLD_THEME_CLASS_NAME = "OLD_THEME_CLASS_NAME";
    public static final String EXTRA_NEW_THEME_CLASS_NAME = "NEW_THEME_CLASS_NAME";
    public static final String EXTRA_TRANSFORMATION_NAME = "TRANSFORMATION_NAME";
    public static final String EXTRA_IMAGE_NAME = "IMAGE_NAME";

    private Intents() {
    }
}
