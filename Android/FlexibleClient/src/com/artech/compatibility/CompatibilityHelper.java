package com.artech.compatibility;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;

public class CompatibilityHelper
{
	/**
	 * Returns true if the device supports the specified API level (or greater).
	 */
	public static boolean isApiLevel(int apiLevel)
	{
		return (VERSION.SDK_INT >= apiLevel);
	}

	/**
	 * Returns true if the device has Honeycomb (3.0) or greater installed.
	 */
	public static boolean isHoneycomb()
	{
		return isApiLevel(VERSION_CODES.HONEYCOMB); // Android 3.0, API Level 11.
	}

	/**
	 * Returns true if the device has Ice Cream Sandwich (4.0) or greater installed.
	 */
	public static boolean isIceCreamSandwich()
	{
		return isApiLevel(VERSION_CODES.ICE_CREAM_SANDWICH); // Android 4.0, API Level 14.
	}

	/**
	 * Runs an AsyncTask. These are always executed in parallel, independently of the device's API level.
	 * @param task Task to be run.
	 * @param params Task parameters.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static <Params> void executeAsyncTask(AsyncTask<Params, ?, ?> task, Params... params)
	{
		// By default, in Honeycomb and later AsyncTasks execute serially, while they execute
		// in parallel (using a thread pool) in earlier versions. Force parallelism here.
		if (isHoneycomb())
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		else
			task.execute(params);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void disableHardwareAcceleration(View view)
	{
		// HA was introduced in Honeycomb.
		if (isHoneycomb())
			view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void invalidateOptionsMenu(Activity activity)
	{
		// Not necessary in pre-Honeycomb, because menu is drawn on tap.
		if (isHoneycomb())
			activity.invalidateOptionsMenu();
	}

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public static void setBackground(View view, Drawable background)
	{
		if (isApiLevel(VERSION_CODES.JELLY_BEAN))
			view.setBackground(background);
		else
			view.setBackgroundDrawable(background);
	}

	public static boolean isStatusBarOverlayingAvailable()
	{
		return isApiLevel(VERSION_CODES.LOLLIPOP);
	}
}
