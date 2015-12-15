package com.artech.android.gam;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;

import com.artech.application.MyApplication;
import com.artech.base.utils.Strings;

public class GAMHelper
{
	private static final String PREFERENCES_FILE = "GAMData"; //$NON-NLS-1$
	private static final String USER_DATA_KEY = "UserInformation"; //$NON-NLS-1$

	public static void afterLogin(JSONObject jsonUserData)
	{
		if (jsonUserData != null)
		{
			// Save for later use.
			SharedPreferences prefs = MyApplication.getAppSharedPreferences(PREFERENCES_FILE);
			prefs.edit().putString(USER_DATA_KEY, jsonUserData.toString()).commit();

			// Store a GAMUser object for later querying.
			GAMUser user = new GAMUser(jsonUserData);
			GAMUser.setCurrentUser(user);
		}
	}

	public static void afterLogin(String userId, boolean isAnonymous)
	{
		try
		{
			// Create fake jsonUserData with partial information.
			JSONObject jsonUserData = new JSONObject();
			jsonUserData.put(GAMUser.FIELD_USER_ID, userId);
			jsonUserData.put(GAMUser.FIELD_USER_NAME, "Unknown"); //$NON-NLS-1$
			jsonUserData.put(GAMUser.FIELD_USER_IS_ANONYMOUS, isAnonymous);

			afterLogin(jsonUserData);

		}
		catch (JSONException e) { }
	}

	public static void restoreUserData()
	{
		SharedPreferences prefs = MyApplication.getAppSharedPreferences(PREFERENCES_FILE);
		String strUserData = prefs.getString(USER_DATA_KEY, null);
		if (Strings.hasValue(strUserData))
		{
			// Load the GAMUser with the last stored data.
			try
			{
				JSONObject jsonUserData = new JSONObject(strUserData);
				GAMUser user = new GAMUser(jsonUserData);
				GAMUser.setCurrentUser(user);
			}
			catch (JSONException e) { }
		}
	}

	public static void afterLogout()
	{
		// Clear current GAMUser.
		GAMUser.setCurrentUser(null);

		// Clear stored data.
		SharedPreferences prefs = MyApplication.getAppSharedPreferences(PREFERENCES_FILE);
		prefs.edit().remove(USER_DATA_KEY).commit();
	}
}
