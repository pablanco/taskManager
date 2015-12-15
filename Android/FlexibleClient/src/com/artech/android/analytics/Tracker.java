package com.artech.android.analytics;

import android.app.Activity;

import com.artech.R;
import com.artech.actions.UIContext;
import com.artech.application.MyApplication;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.utils.Strings;
import com.google.android.gms.analytics.HitBuilders;

public class Tracker
{
	private static Boolean sEnabled;
	
	private static boolean isEnabled()
	{
		if (sEnabled == null)
		{
			String trackingId = MyApplication.getAppContext().getString(R.string.ga_trackingId);
			sEnabled = Strings.hasValue(trackingId);
		}
		
		return sEnabled;
	}
	
	private static void onActivityStart(Activity activity, IViewDefinition definition)
	{
		if (!isEnabled())
			return;
		
		// Call activityStart() to set context, but don't use automatic logging
		// (useless because activity class is always the same).
		com.google.android.gms.analytics.Tracker analiticsTracker = MyApplication.getInstance().getTracker();
		
		MyApplication.getInstance().getAnalytics().reportActivityStart(activity);
		
		if (definition != null)
		{
			 // Set screen name.
			analiticsTracker.setScreenName(definition.getName());
	        // Send a screen view.
			analiticsTracker.send(new HitBuilders.AppViewBuilder().build());
		}
	}

	private static void onActivityStop(Activity activity)
	{
		if (!isEnabled())
			return;
		
		// TODO: its needed?
		//com.google.android.gms.analytics.Tracker analiticsTracker = MyApplication.getInstance().getTracker();
		MyApplication.getInstance().getAnalytics().reportActivityStop(activity);
	}

	public static void onAction(UIContext context, ActionDefinition action)
	{
		if (!isEnabled())
			return;
		
		com.google.android.gms.analytics.Tracker analiticsTracker = MyApplication.getInstance().getTracker();
		
		// TODO: its needed?
		//EasyTracker.getInstance().setContext(context);

		String category = "<Unknown>"; //$NON-NLS-1$
		if (action.getDataView() != null)
			category = action.getDataView().getName();

		// Build and send an Event.
		analiticsTracker.send(new HitBuilders.EventBuilder()
            .setCategory(category)
            .setAction(action.getName())
            .setLabel(Strings.EMPTY)
            .build());
	}

	public static class ActivityTracker
	{
		private final Activity mActivity;
		private boolean mStartCalled;

		public ActivityTracker(Activity activity)
		{
			mActivity = activity;
		}

		public void onStart(IViewDefinition definition)
		{
			if (mActivity.isFinishing())
				return; // Don't track redirects.

			onActivityStart(mActivity, definition);
			mStartCalled = true;
		}

		public void onStop()
		{
			if (mStartCalled)
				onActivityStop(mActivity);
		}
	}
}
