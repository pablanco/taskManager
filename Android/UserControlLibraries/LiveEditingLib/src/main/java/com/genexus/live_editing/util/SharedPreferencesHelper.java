package com.genexus.live_editing.util;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.squareup.okhttp.HttpUrl;

public class SharedPreferencesHelper {
    public static final String PREFERENCES_FILENAME = "com.genexus.live_editing.settings";
    public static final String KEY_LIVE_EDITING_URL = "key_live_editing_url";
    public static final String KEY_APP_URL = "key_app_url";

    private final SharedPreferences mSharedPreferences;
    private final HttpUrl mAppUrl;
    private final HttpUrl mDefaultLiveEditingUrl;

    public SharedPreferencesHelper(@NonNull SharedPreferences sharedPreferences,
                                   @NonNull HttpUrl appUrl,
                                   @NonNull HttpUrl defaultLiveEditingUrl) {
        mSharedPreferences = sharedPreferences;
        mAppUrl = appUrl;
        mDefaultLiveEditingUrl = defaultLiveEditingUrl;
    }

    public boolean saveLiveEditingUrl(HttpUrl liveEditingUrl) {
        boolean appUrlSaved = saveHttpUrl(KEY_APP_URL, mAppUrl);
        boolean liveEditingUrlSaved = saveHttpUrl(KEY_LIVE_EDITING_URL, liveEditingUrl);
        return liveEditingUrlSaved && appUrlSaved;
    }

    public HttpUrl getLiveEditingUrl() {
        HttpUrl savedAppUrl = getHttpUrl(KEY_APP_URL);
        return mAppUrl.equals(savedAppUrl) ? getHttpUrl(KEY_LIVE_EDITING_URL)
                : mDefaultLiveEditingUrl;
    }

    private boolean saveHttpUrl(String key, HttpUrl url) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, url.toString());
        return editor.commit();
    }

    private HttpUrl getHttpUrl(String key) {
        String url = mSharedPreferences.getString(key, null);
        return url != null ? HttpUrl.parse(url) : null;
    }
}
