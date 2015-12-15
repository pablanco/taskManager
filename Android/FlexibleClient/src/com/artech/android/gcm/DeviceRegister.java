package com.artech.android.gcm;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.artech.android.api.ClientInformation;
import com.artech.application.MyApplication;
import com.artech.base.application.IProcedure;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.model.PropertiesObject;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class DeviceRegister {

	  public static final String PROPERTY_REG_ID = "registration_id";
	  static final String TAG = "GCM Client";
	  private static final String PROPERTY_APP_VERSION = "appVersion";
	
	public static void registerDeviceInGCM()
	{
		// Called from application loader, already run in background.
		//registerInBackground(MyApplication.getInstance().getApplicationContext());
		// call in a new thread to no wait for it.
		Thread thread = new Thread(null, doBackgroundDeviceInGCM,"Background"); //$NON-NLS-1$
		thread.start();
	}

	
	private static final Runnable doBackgroundDeviceInGCM = new Runnable()
	{
		@Override
		public void run()
		{
			registerInBackground(MyApplication.getInstance().getApplicationContext());
		}
	};

		
	 /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private static void registerInBackground(final Context context) 
    {
    	String msg;
        try 
        {
        	GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

        	String regid = gcm.register(MyApplication.getApp().getNotificationSenderId());
        	msg = "Device registered, registration ID=" + regid;

        	// You should send the registration ID to your server over HTTP, so it
        	// can use GCM/HTTP or CCS to send messages to your app.
        	//sendRegistrationIdToBackend();
        	registerWithServer(context, regid );

        	// Persist the regID - no need to register again.
        	storeRegistrationId(context, regid);
        } catch (IOException ex) {
        	msg = "Error :" + ex.getMessage();
        	// If there is an error, don't just keep trying to register.
        	// Require the user to click a button again, or perform
        	// exponential back-off.
        }
    	Services.Log.debug(msg);
    }

	public static boolean registerWithServer(Context context, String registration)
	{
		//Register Device in the server

		// Respect procedure connectivity support. Default online.
		IProcedure procedureRegistrer = MyApplication.getApplicationServer(Connectivity.Online).getProcedure(MyApplication.getApp().getNotificationRegistrationHandler());

		PropertiesObject parameters = new PropertiesObject();

		parameters.setProperty("DeviceType", Strings.ONE); //$NON-NLS-1$
		parameters.setProperty("DeviceId", ClientInformation.id()); //$NON-NLS-1$
		parameters.setProperty("DeviceToken", registration); //$NON-NLS-1$
		parameters.setProperty("DeviceName", ClientInformation.osName() + Strings.SPACE + ClientInformation.osVersion()); //$NON-NLS-1$

		Services.Log.debug("GCM Register with server " + parameters.getInternalProperties().toString() );

		OutputResult result = procedureRegistrer.execute(parameters);

		if (result.isOk())
		{
			Services.Log.debug("Call to NotificationRegistrationHandler ok ");
			return true;
		}
		return false;
	}

		
	  
	 /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    public static String getRegistrationId(Context context) 
    {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (!Services.Strings.hasValue(registrationId)) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private static void storeRegistrationId(Context context, String regId)
    {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }
    
    /**
     * @return Application's {@code SharedPreferences}.
     */
    private static SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return context.getSharedPreferences(DeviceRegister.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    
    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
    
    
    
    
}
