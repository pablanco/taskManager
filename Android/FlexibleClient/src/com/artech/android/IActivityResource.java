package com.artech.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Interface implemented by API components bound to an Activity that must receive notifications from it.
 * (e.g. ProgressIndicators, Activity Audio, &c). Methods are called on the corresponding lifecycle events of
 * their owner activities.
 */
public interface IActivityResource
{
	void onNewIntent(Activity activity, Intent intent);
	void onResume(Activity activity);
	void onPause(Activity activity);
	void onDestroy(Activity activity);
	void onCreate(Activity activity, Bundle savedInstance);
	void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data);
	void onSaveInstanceState(Activity activity, Bundle outState);
}
