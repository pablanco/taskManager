package com.artech.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;

import com.artech.application.MyApplication;
import com.artech.base.utils.Strings;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class GooglePlayServicesHelper
{
	private static int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;

	public static boolean checkPlayServices(Activity activity)
	{
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
		if (status != ConnectionResult.SUCCESS)
		{
			showError(activity, status);
			return false;
		}
		else
			return true;
	}

	public static void showError(Activity activity, int errorCode)
	{
		if (GooglePlayServicesUtil.isUserRecoverableError(errorCode))
			showRecoverableError(activity, errorCode);
		else
			showUnrecoverableError(activity, errorCode);
	}

	private static void showRecoverableError(Activity activity, int code)
	{
		Dialog dialog = GooglePlayServicesUtil.getErrorDialog(code, activity, REQUEST_CODE_RECOVER_PLAY_SERVICES);
		if (dialog != null)
			dialog.show();
		else
			showUnrecoverableError(activity, code);
	}

	private static void showUnrecoverableError(Context context, int code)
	{
		String errorMessage = GooglePlayServicesUtil.getErrorString(code);
		if (!Strings.hasValue(errorMessage))
			errorMessage = String.format("Unknown Google Play Services error (%s).", code);

		MyApplication.getInstance().showMessage(errorMessage);
	}
	
	public static boolean isPlayServicesAvailable(Context context)
	{
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
		return status == ConnectionResult.SUCCESS;
	}
	
}
