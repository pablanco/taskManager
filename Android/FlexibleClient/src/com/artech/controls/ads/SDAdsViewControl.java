package com.artech.controls.ads;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.artech.activities.IntentParameters;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;
import com.artech.ui.Coordinator;
import com.artech.usercontrols.IGxUserControl;

import java.util.List;


public class SDAdsViewControl extends FrameLayout implements IGxUserControl, IGxControlRuntime
{
    private LayoutItemDefinition mDefinition = null;
    private IAdsViewController mController;
    private IAdsProvider mProvider;

    public SDAdsViewControl(Context context, Coordinator coordinator, LayoutItemDefinition layoutItemDefinition) {
        super(context);
        ControlInfo info = layoutItemDefinition.getControlInfo();
        String providerId = info.optStringProperty("@SDAdsViewAdsProvider");
        mProvider = Ads.getProvider(providerId);
        mController = mProvider.createViewController(context, layoutItemDefinition);
        this.addView(mController.createView(), new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getWidth() > 0 || getHeight() > 0) {
                    mController.setViewSize(Services.Device.pixelsToDips(getWidth()), Services.Device.pixelsToDips(getHeight()));
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    @Override
    public void setProperty(String name, Object value) {
        mController.setProperty(name, value);
    }

    @Override
    public Object getProperty(String name) {
        return mController.getProperty(name);
    }

    @Override
    public void runMethod(String name, List<Object> parameters) {
        mController.runMethod(name, parameters);
    }
}

