package com.genexus.live_editing.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;

import com.genexus.live_editing.R;
import com.genexus.live_editing.ui.preferences.EndpointPreference;
import com.genexus.live_editing.util.Intents;
import com.genexus.live_editing.util.SharedPreferencesHelper;
import com.squareup.okhttp.HttpUrl;


public class SettingsActivity extends AppCompatPreferenceActivity {
    public static final int REQUEST_CODE = 947;
    public static final String EXTRA_DEFAULT_LIVE_EDITING_URL = "DefaultLiveEditingUrl";
    public static final String EXTRA_APP_URL = "AppUrl";
    private SharedPreferencesHelper mSharedPreferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        SharedPreferences sp = getSharedPreferences(SharedPreferencesHelper.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        HttpUrl appUrl = HttpUrl.parse(intent.getStringExtra(EXTRA_APP_URL));
        HttpUrl defaultLiveEditingUrl = HttpUrl.parse(intent.getStringExtra(EXTRA_DEFAULT_LIVE_EDITING_URL));

        mSharedPreferencesHelper = new SharedPreferencesHelper(sp, appUrl, defaultLiveEditingUrl);

        setTitle(R.string.settings_activity_title);
        setupPreferences(defaultLiveEditingUrl.toString());
    }

    private void setupPreferences(String defaultLiveEditingUrl) {
        PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesName(SharedPreferencesHelper.PREFERENCES_FILENAME);
        pm.setSharedPreferencesMode(Context.MODE_PRIVATE);

        EndpointPreference endpointPreference = new EndpointPreference(this);
        endpointPreference.setTitle(R.string.endpoint_pref_title);
        endpointPreference.setDialogTitle(R.string.endpoint_pref_title);
        endpointPreference.setKey(SharedPreferencesHelper.KEY_LIVE_EDITING_URL);
        endpointPreference.setDefaultValue(defaultLiveEditingUrl);
        endpointPreference.setOnPreferenceChangeListener(mPreferenceChangeListener);

        PreferenceCategory category = new PreferenceCategory(this);
        category.setTitle(R.string.pref_category_title);

        PreferenceScreen screen = pm.createPreferenceScreen(this);
        screen.addPreference(category);
        category.addPreference(endpointPreference);
        setPreferenceScreen(screen);
    }

    private Preference.OnPreferenceChangeListener
            mPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            HttpUrl url = HttpUrl.parse((String) newValue);
            boolean success = mSharedPreferencesHelper.saveLiveEditingUrl(url);
            if (success) {
                LocalBroadcastManager.getInstance(SettingsActivity.this).
                        sendBroadcast(new Intent(Intents.ACTION_CONNECT));
            }
            return success;
        }
    };
}
