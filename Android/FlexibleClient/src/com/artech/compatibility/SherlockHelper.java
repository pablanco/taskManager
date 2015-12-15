package com.artech.compatibility;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;

/**
 * Helper class for dealing with methods that should be called differently on
 * AppCompatActivity or plain Activity.
 * @author matiash
 *
 */
public class SherlockHelper
{
	private SherlockHelper() { }

	public static void requestWindowFeature(Activity activity, int featureId)
	{
		if (activity instanceof AppCompatActivity)
			((AppCompatActivity)activity).supportRequestWindowFeature(featureId);
		else
			activity.requestWindowFeature(featureId);
	}

	public static boolean hasActionBar(Activity activity) {
		return getActionBar(activity) != null;
	}

	public static ActionBar getActionBar(Activity activity)
	{
		if (activity instanceof AppCompatActivity)
			return ((AppCompatActivity)activity).getSupportActionBar();
		else
			return null; // Android Activities do not have an ABS Action Bar.
	}

	public static void setProgressBarIndeterminateVisibility(Activity activity, boolean visible)
	{
		// As of version 21, AppCompatActivity no longer supports progress bars.
		// I'm not deleting this method because we may yet implement it with a custom Toolbar. 
		/*
		if (activity instanceof AppCompatActivity)
			((AppCompatActivity)activity).setSupportProgressBarIndeterminateVisibility(visible);
		else
			activity.setProgressBarIndeterminateVisibility(visible);
		*/
	}

	public static void setProgressBarVisibility(Activity activity, boolean visible)
	{
		// As of version 21, AppCompatActivity no longer supports progress bars.
		// I'm not deleting this method because we may yet implement it with a custom Toolbar.
		/*
		if (activity instanceof AppCompatActivity)
			((AppCompatActivity)activity).setSupportProgressBarVisibility(visible);
		else
			activity.setProgressBarVisibility(visible);
		*/
	}

	public static void setProgress(Activity activity, int progress)
	{
		// As of version 21, AppCompatActivity no longer supports progress bars.
		// I'm not deleting this method because we may yet implement it with a custom Toolbar. 
		/*
		if (activity instanceof AppCompatActivity)
			((AppCompatActivity)activity).setSupportProgress(progress);
		else
			activity.setProgress(progress);
		*/
	}

	public static Context getActionBarThemedContext(Activity activity)
	{
		ActionBar compatActionBar = null;

		if (activity instanceof AppCompatActivity)
			compatActionBar = ((AppCompatActivity)activity).getSupportActionBar();

		if (compatActionBar != null)
			return compatActionBar.getThemedContext();
		else
			return activity;
	}

	@SuppressLint("NewApi")
	public static void invalidateOptionsMenu(Activity activity)
	{
		if (activity == null)
			return;

		if (activity instanceof AppCompatActivity)
			((AppCompatActivity)activity).supportInvalidateOptionsMenu();
		else if (CompatibilityHelper.isHoneycomb())
			activity.invalidateOptionsMenu(); // Introduced in API Level 11.
	}

	public static ActionMode startActionMode(Activity activity, ActionMode.Callback callback)
	{
		if (activity instanceof AppCompatActivity)
			return ((AppCompatActivity)activity).startSupportActionMode(callback);
		else
			return null;
	}
}
