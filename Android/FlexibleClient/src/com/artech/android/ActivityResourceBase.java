package com.artech.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Dummy abstract class, so that IActivityResource implementors don't need to override all
 * methods if they don't want to.
 * Calling super() for these methods is not necessary, since they are all empty.
 *
 * @author matiash
 */
public class ActivityResourceBase implements IActivityResource
{
	@Override
	public void onCreate(Activity activity, Bundle savedInstanceState) { }

	@Override
	public void onResume(Activity activity) { }

	@Override
	public void onNewIntent(Activity activity, Intent intent) { }

	@Override
	public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) { }

	@Override
	public void onSaveInstanceState(Activity activity, Bundle outState) { }

	@Override
	public void onPause(Activity activity) { }

	@Override
	public void onDestroy(Activity activity) { }
}
