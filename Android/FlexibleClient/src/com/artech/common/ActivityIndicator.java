package com.artech.common;

import android.app.Activity;
import android.os.Handler;

import com.artech.actions.CallGxObjectAction;
import com.artech.activities.ActivityController;
import com.artech.activities.ActivityHelper;
import com.artech.activities.IGxActivity;
import com.artech.compatibility.SherlockHelper;
import com.artech.controls.GxWebView;
import com.artech.services.EntityService;

/**
 * Class used to manage feedback during background activities (mainly remote calls,
 * such as procedure execution or data provider invocations).
 * @author matiash
 *
 */
public class ActivityIndicator
{
	private static Handler sCurrentHandler;
	private static int UPDATE_STATUS_DELAY = 250; // ms

	public static void onResume(Activity activity)
	{
		sCurrentHandler = new Handler();
		sCurrentHandler.postDelayed(sUpdateStatus, UPDATE_STATUS_DELAY);
	}

	public static void onPause(Activity activity)
	{
		if (sCurrentHandler != null)
			sCurrentHandler.removeCallbacks(sUpdateStatus);

		sCurrentHandler = null;
	}

	private static Runnable sUpdateStatus = new Runnable()
	{
		@Override
		public void run()
		{
			Activity activity = ActivityHelper.getCurrentActivity();
			boolean isWorking = EntityService.isWorking() || CallGxObjectAction.isWorking() || GxWebView.isWorking();

			if (activity != null)
				updateLoadingStatus(activity, isWorking);

			if (sCurrentHandler != null)
				sCurrentHandler.postDelayed(sUpdateStatus, UPDATE_STATUS_DELAY);
		}
	};

	private static void updateLoadingStatus(Activity activity, boolean isWorking)
	{
		if (activity instanceof IGxActivity)
		{
			ActivityController controller = ((IGxActivity)activity).getController();
			if (controller != null)
				controller.updateLoadingStatus(isWorking);
		}
		else
		{
			// Fall back to default method.
			SherlockHelper.setProgressBarIndeterminateVisibility(activity, isWorking);
		}
	}
}
