package com.artech.android.api;

import java.util.Locale;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;
import com.artech.common.ServiceHelper;

/**
 * This class allow access to device information.
 * @author GMilano
 *
 */
public class ClientInformation
{
	private static final String OSName = "Android"; //$NON-NLS-1$

	/***
	 * Return a value that identify the device. This id is network independent.
	 * @return
	 */
	public static String id()
	{
		DeviceUuidFactory factory = new DeviceUuidFactory(MyApplication.getInstance());
		return factory.getDeviceUuid().toString();
	}

	/***
	 * Returns Android as the operating system
	 * @return
	 */
	public static String osName() { return OSName; }

	/***
	 * Return the OS Version code, you can see the values from
	 * http://developer.android.com/reference/android/os/Build.VERSION_CODES.html
	 * @return
	 */
	public static String osVersion()
	{
		// change implementation, issue 29979, retornar version release.
		//return String.valueOf(android.os.Build.VERSION.SDK_INT);
		return String.valueOf(android.os.Build.VERSION.RELEASE);
	}
	
	public static String deviceName()
	{
		return Build.DEVICE + "-" + Build.SERIAL;
	}

	private static String networkId = null;
	private static Object permission = null;

	private static Boolean hasReadPhonePermission()
	{
		if (permission == null)
			permission = PackageManager.PERMISSION_GRANTED == MyApplication.getInstance().checkCallingOrSelfPermission(android.Manifest.permission.READ_PHONE_STATE);
		return (Boolean) permission;
	}

	/***
	 *  Requires READ_PHONE_STATE permission.
	 * @return
	 */
	public static String networkId()
	{
		if (networkId != null)
			return networkId;

		try
		{
			if (hasReadPhonePermission())
			{
				TelephonyManager manager = (TelephonyManager)MyApplication.getInstance().getSystemService(Context.TELEPHONY_SERVICE);
				networkId = manager.getDeviceId();
			}
		}
		catch (Exception ex)
		{
			Services.Log.Error("Exception Network Id Function", ex.getMessage(), ex); //$NON-NLS-1$
		}

		if (networkId == null)
			networkId = Strings.EMPTY; // Either we didn't have permission, or we did but are not connected to a network.

		return networkId;
	}

	public static String getLocaleString(Locale language)
	{
		return ServiceHelper.getLocaleString(language);
	}

	public static String getPlatformName()
	{
		return PlatformHelper.getPlatform().getName();
	}

	public static String getLanguage()
	{
		return getLocaleString(Locale.getDefault());
	}
}
