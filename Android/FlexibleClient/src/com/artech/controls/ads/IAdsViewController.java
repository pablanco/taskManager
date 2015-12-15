package com.artech.controls.ads;

import android.view.View;

import com.artech.base.controls.IGxControlRuntime;

/**
 * Created by gmilano on 8/4/15.
 */
public interface IAdsViewController extends IGxControlRuntime {

    View createView();
    void setViewSize(int width, int height);
}
