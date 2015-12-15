package com.artech.controls.ads;

import android.content.Context;
import android.view.View;

import com.artech.base.metadata.layout.LayoutItemDefinition;

import java.util.List;

/**
 * Created by gmilano on 8/3/15.
 */
public interface IAdsProvider {

    /***
     * Get the id for this provider. ie: "mobfox", "admob", "smartadserver", etc
     * @return
     */
    String getId();

    /***
     * Create a view controller for a banner
     * @param context
     * @param layoutItemDefinition
     * @return
     */
    IAdsViewController createViewController(Context context, LayoutItemDefinition layoutItemDefinition);

}
