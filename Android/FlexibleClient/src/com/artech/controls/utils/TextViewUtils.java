package com.artech.controls.utils;

import android.text.Html;
import android.widget.TextView;

import com.artech.base.metadata.layout.LayoutItemDefinition;

/**
 * Created by gmilano on 6/12/15.
 */
public class TextViewUtils {

    public static void setText(TextView view, String text, LayoutItemDefinition definition) {
        if (definition.isHtml()) {
            view.setText(Html.fromHtml(text));
        } else {
            view.setText(text);
        }
    }
}
