package com.genexus.controls;

import android.view.View;

import com.adsdk.sdk.Ad;
import com.adsdk.sdk.AdListener;
import com.adsdk.sdk.Gender;
import com.adsdk.sdk.banner.AdView;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.utils.Strings;
import com.artech.controls.ads.IAdsProvider;
import com.artech.controls.ads.IAdsViewController;
import com.artech.utils.Cast;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

/**
 * Created by gmilano on 8/3/15.
 */
public class MobFoxProvider implements IAdsProvider {


    @Override
    public String getId() {
        return "mobfox";
    }


    @Override
    public IAdsViewController createViewController(Context context, LayoutItemDefinition layoutItemDefinition) {
        return new GxAdViewController(context, layoutItemDefinition.getControlInfo());
    }

}
