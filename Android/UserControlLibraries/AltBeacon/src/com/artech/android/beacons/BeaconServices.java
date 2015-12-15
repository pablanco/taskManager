package com.artech.android.beacons;

import com.artech.application.MyApplication;

import android.app.Service;

public class BeaconServices
{
	private static final String BEACON_SERVICE_CLASS = "AltBeaconService";
	private static final String BEACON_INTENT_PROCESSOR_CLASS = "AltBeaconIntentProcessor";
	
	public static Class<? extends Service> getBeaconServiceClass()
	{
		return MyApplication.getServiceClass(BEACON_SERVICE_CLASS);
	}
	
	public static void setBeaconServiceClass(Class<? extends Service> serviceClass)
	{
		MyApplication.registerServiceClass(BEACON_SERVICE_CLASS, serviceClass);
	}
	
	public static Class<? extends Service> getBeaconIntentProcessorClass()
	{
		return MyApplication.getServiceClass(BEACON_INTENT_PROCESSOR_CLASS);
	}
	
	public static void setBeaconIntentProcessorClass(Class<? extends Service> serviceClass)
	{
		MyApplication.registerServiceClass(BEACON_INTENT_PROCESSOR_CLASS, serviceClass);
	}
}
