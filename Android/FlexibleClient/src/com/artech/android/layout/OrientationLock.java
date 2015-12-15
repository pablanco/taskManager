package com.artech.android.layout;

import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.Surface;

import com.artech.android.ActivityResourceBase;
import com.artech.android.ActivityResources;
import com.artech.base.services.Services;
import com.artech.base.utils.Function;

/**
 * Helper class to lock an activity's orientation to its current one
 * (including reverse rotations in Froyo or newer).
 * @author Matias
 */
public class OrientationLock extends ActivityResourceBase
{
	private final Activity mActivity;
	private final int mOriginalOrientation;
	private Set<String> mLockReasons;

	public static final String REASON_LOAD_METADATA = "GX::LoadingMetadata";
	public static final String REASON_SHOW_POPUP = "GX::ShowingPopup";
	public static final String REASON_RUN_EVENT = "GX::RunningEvent";
	
	private OrientationLock(Activity activity)
	{
		mActivity = activity;
		mOriginalOrientation = activity.getRequestedOrientation();
		mLockReasons = new HashSet<String>();
	}

	@SuppressLint("NewApi")
	private void lockOrientation(String reason)
	{
		boolean wasLocked = (mLockReasons.size() != 0);
		mLockReasons.add(reason);
		
		if (wasLocked)
			return; // Was already locked, just (possibly) added reason.

		int orientation = mActivity.getResources().getConfiguration().orientation;
		int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();

		// Adapted from http://stackoverflow.com/a/8450316
		if (orientation == Configuration.ORIENTATION_PORTRAIT)
		{
            if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_180)
            	mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            else
            	mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		else if (orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90)
				mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			else
				mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
		}
	}

	private void unlockOrientation(String reason)
	{
		if (!mLockReasons.contains(reason))
		{
			// Not locked for this reason?
			Services.Log.debug("Asked to unlock orientation for reason " + reason + " but it was not among the lock reasons. Ignored.");
			return;
		}

		mLockReasons.remove(reason);
		
		if (mLockReasons.size() == 0)
			mActivity.setRequestedOrientation(mOriginalOrientation);
	}
	
	public static void lock(Activity activity, String reason)
	{
		OrientationLock lock = ActivityResources.getResource(activity, OrientationLock.class,
			new Function<Activity, OrientationLock>()
			{
				@Override
				public OrientationLock run(Activity input){ return new OrientationLock(input); }
			});

		lock.lockOrientation(reason);
	}

	public static void unlock(Activity activity, String reason)
	{
		OrientationLock lock = ActivityResources.getResource(activity, OrientationLock.class);
		if (lock != null)
			lock.unlockOrientation(reason);
	}
}
