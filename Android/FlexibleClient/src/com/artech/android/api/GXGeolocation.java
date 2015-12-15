package com.artech.android.api;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;

import com.artech.activities.ActivityHelper;
import com.artech.base.services.Services;
import com.artech.base.utils.ReflectionHelper;
import com.genexus.xml.GXXMLSerializable;

public class GXGeolocation
{
	
	public static double getLatitude(String geolocation)
	{
		return com.genexus.util.GXGeolocation.getLatitude(geolocation);
	}

	public static double getLongitude(String geolocation)
	{
		return com.genexus.util.GXGeolocation.getLongitude(geolocation);
	}
	
	
	public static int getDistance(String location1, String location2)
	{
		return com.genexus.util.GXGeolocation.getDistance(location1, location2);
	}
	
	
	@SuppressWarnings("rawtypes")
	public static java.util.Vector getAddress(String location)
	{
		return com.genexus.util.GXGeolocation.getAddress(location);
	}
	
	@SuppressWarnings("rawtypes")
	public static java.util.Vector getLocation(String address)
	{
		return com.genexus.util.GXGeolocation.getLocation(address);
	}

	public static Object getmylocation( Integer minAccuracy , Integer timeout , Boolean includeHeadingAndSpeed )
	{
		return getmylocation( minAccuracy , timeout , includeHeadingAndSpeed, false );
	}
	
	public static Object getmylocation( Integer minAccuracy , Integer timeout , Boolean includeHeadingAndSpeed ,
			Boolean ignoreErrors )
	{
		List<String> values = new ArrayList<String>();
		values.add(String.valueOf(minAccuracy) );
		values.add(String.valueOf(timeout) );
		values.add(String.valueOf(includeHeadingAndSpeed) );
		values.add(String.valueOf(ignoreErrors) );
		
		Activity activity = ActivityHelper.getCurrentActivity();
		
		// must return a SdtGeoLocationInfo
		String className = "SdtGeoLocationInfo";
		Class<?> clazz = ReflectionHelper.getClass(Object.class, className);
		if (clazz!=null)
		{
			Object sdtGeoLocationInfo = ReflectionHelper.createDefaultInstance(clazz, true);
			// call to fromJSonString of GXXMLSerializable
			org.json.JSONObject resultJson = SDActions.getMyLocation(activity, values, false);
			GXXMLSerializable result = (GXXMLSerializable) sdtGeoLocationInfo;
			if (resultJson!=null && result!=null)
				result.fromJSonString(resultJson.toString());
		
			return sdtGeoLocationInfo;
		}
		Services.Log.Error("getmylocation fails, cannot get SdtGeoLocationInfo class" ); //$NON-NLS-1$
		return null;
	}
		

}
