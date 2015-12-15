package com.genexus.live_editing.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.SharedPreferences;
import android.test.suitebuilder.annotation.SmallTest;

import com.squareup.okhttp.HttpUrl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SharedPreferencesHelperTest {
    private static final String APP_URL_1 = "http://example.com/Id1/";
    private static final String APP_URL_2 = "http://example.com/Id2/";
    private static final String LIVE_EDITING_URL_1 = "http://192.168.0.1:20400/gxlivepreviewservice/?6b7f994d-bf0f-455a-8841-37b47a5d288e:MainTest";
    private static final String LIVE_EDITING_URL_2 = "http://10.0.0.1:30100/gxlivepreviewservice/?6b7f994d-bf0f-455a-8841-37b47a5d288e:MainTest";

    @Test
    public void noAppUrlHasBeenSaved_shouldReturnDefaultUrl() {
        SharedPreferences sharedPreferences = createMockedSharedPreferences(null, null);
        HttpUrl appUrl = HttpUrl.parse(APP_URL_1);
        HttpUrl defaultUrl = HttpUrl.parse(LIVE_EDITING_URL_2);

        SharedPreferencesHelper instance = new SharedPreferencesHelper(sharedPreferences, appUrl, defaultUrl);

        assertThat(instance.getLiveEditingUrl(), is(equalTo(defaultUrl)));
    }

    @Test
    public void savedAppUrlHasNotChanged_shouldReturnSavedLiveEditingUrl() {
        SharedPreferences sharedPreferences = createMockedSharedPreferences(APP_URL_1, LIVE_EDITING_URL_1);
        HttpUrl appUrl = HttpUrl.parse(APP_URL_1);
        HttpUrl defaultUrl = HttpUrl.parse(LIVE_EDITING_URL_2);
        HttpUrl liveEditingUrl = HttpUrl.parse(LIVE_EDITING_URL_1);

        SharedPreferencesHelper instance = new SharedPreferencesHelper(sharedPreferences, appUrl, defaultUrl);

        assertThat(instance.getLiveEditingUrl(), is(equalTo(liveEditingUrl)));
    }

    @Test
    public void savedAppUrlHasChanged_shouldReturnDefaultLiveEditingUrl() {
        SharedPreferences sharedPreferences = createMockedSharedPreferences(APP_URL_1, LIVE_EDITING_URL_1);
        HttpUrl appUrl = HttpUrl.parse(APP_URL_2);
        HttpUrl defaultUrl = HttpUrl.parse(LIVE_EDITING_URL_2);

        SharedPreferencesHelper instance = new SharedPreferencesHelper(sharedPreferences, appUrl, defaultUrl);

        assertThat(instance.getLiveEditingUrl(), is(equalTo(defaultUrl)));
    }

    @Test
    public void saveLiveEditingUrl_shouldPersistAppUrlAndLiveEditingUrl() {

    }

    /**
     * Creates a mocked SharedPreferences instance that has the urls passed by parameter stored.
     */
    private SharedPreferences createMockedSharedPreferences(String appUrl, String liveEditingUrl) {
        SharedPreferences mockedSharedPreferences = mock(SharedPreferences.class);
        SharedPreferences.Editor mockedEditor = mock(SharedPreferences.Editor.class);

        // Mocking reading the SharedPreferences as if mockedSharedPreferences was previously written
        // correctly.
        when(mockedSharedPreferences.getString(eq(SharedPreferencesHelper.KEY_APP_URL), anyString()))
                .thenReturn(appUrl);
        when(mockedSharedPreferences.getString(eq(SharedPreferencesHelper.KEY_LIVE_EDITING_URL), anyString()))
                .thenReturn(liveEditingUrl);

        // Mocking a successful commit.
        when(mockedEditor.commit()).thenReturn(true);

        // Return the MockEditor when requesting it.
        when(mockedSharedPreferences.edit()).thenReturn(mockedEditor);

        return mockedSharedPreferences;
    }
}