package com.artech.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.inputmethod.InputMethodManager;

import com.artech.base.services.Services;
import com.fedorvlasov.lazylist.ImageLoader;

public abstract class GxBaseActivity extends AppCompatActivity implements IGxBaseActivity
{
	private ImageLoader mImageLoader;

	// TODO: Should this be static??
	public static String PickingElementId = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mImageLoader = new ImageLoader(this);
		if (!Services.Application.isLoaded())
		{
			Services.Log.warning("Reaload app metadata from activity onCreate"); //$NON-NLS-1$
			ActivityLauncher.callApplicationMain(this, true, false);
		}
	}

	@Override
	public void setTitle(CharSequence title)
	{
		// Workaround for bug in ActionBarActivity, for Android 2.x. Check if the error persists when updating it!
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
			actionBar.setTitle(title);
		else
			super.setTitle(title);
	}

	@Override
	protected void onDestroy()
	{
		mImageLoader.stopThread();
		mImageLoader = null;

		// hack to release an internal reference see this thread for further informantion
		// http://stackoverflow.com/questions/5038158/main-activity-is-not-garbage-collected-after-destruction-because-it-is-referenced
		InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		manager.isActive();
		super.onDestroy();
	}

	@Override
	public ImageLoader getImageLoader()
	{
		return mImageLoader;
	}
}
