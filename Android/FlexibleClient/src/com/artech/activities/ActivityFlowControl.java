package com.artech.activities;

import android.app.Activity;

import com.artech.base.metadata.IViewDefinition;

public class ActivityFlowControl
{
	private static String sObjectToReturn;
	public static final int RETURN_TO_LOGIN = 1235234;

	public static void returnTo(Activity from, String to)
	{
		// Store the name of the object we want to return to, and
		sObjectToReturn = to;
		from.setResult(RETURN_TO_LOGIN);
		from.finish();
	}

	public static void finishWithReturn(Activity activity)
	{
		if (activity.getParent() != null)
		{
			Activity parentActivity = activity.getParent();
			setReturnResult(parentActivity);
		}

		setReturnResult(activity);
		activity.finish();
	}

	private static void setReturnResult(Activity activity)
	{
		if (activity instanceof IGxActivity)
			((IGxActivity)activity).setReturnResult();
		else
			activity.setResult(Activity.RESULT_OK);
	}

	public static void finishWithCancel(Activity activity)
	{
		// No output to be returned, just set result and finish.
		activity.setResult(Activity.RESULT_CANCELED);
		activity.finish();
	}

	/**
	 * Called after an activity resumes to handle flow, if applicable (e.g. closing it because
	 * we were asked to return to a previous point).
	 * @return True if the activity will continue; false if it is finished.
	 */
	static boolean onResume(Activity activity)
	{
		// See if there is a pending "ReturnTo" request.
		if (sObjectToReturn != null)
		{
			if (activity.getParent() != null)
			{
				// if it is a child activity, do not take in account it. 
				return true;
			}
		
			IViewDefinition definition = getMainDefinition(activity);
			if (!isObject(definition, sObjectToReturn))
			{
				// We were asked to return up, and this is NOT the stop point. Continue upwards.
				activity.finish();
				return false;
			}

			// We were asked to return up, but this is either the stop point, or an activity that
			// doesn't correspond to an object (e.g. Main), or we've became lost. Stop here.
			sObjectToReturn = null;
			return true;
		}

		return true;
	}

	public static IViewDefinition getMainDefinition(Activity activity) {
		if (activity instanceof IGxDashboardActivity)
		{
			return ((IGxDashboardActivity)activity).getDashboardDefinition();
		}
		if (activity instanceof IGxActivity)
		{
			return ((IGxActivity)activity).getMainDefinition();
		}
		return null;
	}

	private static boolean isObject(IViewDefinition definition, String objectName)
	{
		if (definition == null)
			return false;

		if (definition.getName().equalsIgnoreCase(objectName))
			return true; // Exact match (e.g. 'WWSDCustomer.Customer.List').

		return definition.getObjectName().equalsIgnoreCase(objectName); // Object name match (e.g. 'WWSDCustomer').

	}

	static void onPause(Activity activity)
	{
	}
}
