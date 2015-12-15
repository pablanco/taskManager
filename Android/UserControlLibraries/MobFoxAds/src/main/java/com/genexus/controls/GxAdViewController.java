package com.genexus.controls;

import android.content.Context;
import android.view.View;

import com.adsdk.sdk.Ad;
import com.adsdk.sdk.AdListener;
import com.adsdk.sdk.Gender;
import com.adsdk.sdk.banner.AdView;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.controls.ads.IAdsViewController;

import java.util.Arrays;
import java.util.List;

/**
 * Created by gmilano on 8/4/15.
 */
public class GxAdViewController implements AdListener , IGxControlRuntime , IAdsViewController {

    private ControlInfo mControlInfo;
    private String mPublisherId;
    private String mGender;
    private String mKeywords;
    private int mUserAge;
    private static String REQUEST_URL = "http://my.mobfox.com/request.php";
    private AdView mView;
    private Context mContext;


    public GxAdViewController(Context context, ControlInfo info) {
        mControlInfo = info;
        mContext = context;
        mUserAge = mControlInfo.optIntProperty("@SDAdsViewUserAge");

    }

    public void setViewSize(int width, int height) {

        mView.setAdspaceWidth(width);
        mView.setAdspaceHeight(height);
      //  mView.startReloadTimer();
        mView.loadNextAd();
    }

    @Override
    public View createView() {
        mView = new AdView(mContext, REQUEST_URL, mControlInfo.optStringProperty("@SDAdsViewPublisherId"), true, true);
        if (mUserAge > 0)
            mView.setUserAge(mUserAge);
        mGender = mControlInfo.optStringProperty("@SDAdsViewUserGender");
        if (mGender.equalsIgnoreCase("female"))
            mView.setUserGender(Gender.FEMALE);
        if (mGender.equalsIgnoreCase("male"))
            mView.setUserGender(Gender.MALE);
        mKeywords = mControlInfo.optStringProperty("@SDAdsViewKeywords");
        if (mKeywords.length() > 0)
            mView.setKeywords(Arrays.asList(mKeywords.split(" ")));
        mView.setAdspaceStrict(false); // Optional, tells the server to only supply banner ads that are exactly of the desired size. Without setting it, the server could also supply smaller Ads when no ad of desired size is available.
        mView.setAdListener(this);
        return mView;
    }


    @Override
    public void adClicked() {

    }

    @Override
    public void adClosed(Ad ad, boolean b) {

    }

    @Override
    public void adLoadSucceeded(Ad ad) {

    }

    @Override
    public void adShown(Ad ad, boolean b) {

    }

    @Override
    public void noAdFound() {

    }

    @Override
    public void setProperty(String name, Object value) {
        if (name.equalsIgnoreCase("keywords")) {
            mKeywords = value.toString();
            if (mKeywords.length() > 0)
                mView.setKeywords(Arrays.asList(mKeywords.split(" ")));
        }
    }

    @Override
    public Object getProperty(String name) {
        if (name.equalsIgnoreCase("keywords"))
            return mKeywords;
        return null;
    }

    @Override
    public void runMethod(String name, List<Object> parameters) {
        if (name.equalsIgnoreCase("requestad")) {
            mView.loadNextAd();
        }
    }
}
