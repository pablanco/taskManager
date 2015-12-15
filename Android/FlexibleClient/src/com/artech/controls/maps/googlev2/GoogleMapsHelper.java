package com.artech.controls.maps.googlev2;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import com.artech.android.GooglePlayServicesHelper;
import com.artech.base.utils.Strings;
import com.artech.controls.maps.GxMapViewDefinition;
import com.google.android.gms.maps.GoogleMap;

class GoogleMapsHelper
{
	public static boolean checkGoogleMapsV2(Activity activity)
	{
		// Check that Google Play Services is available, and that the Google Maps app
		// is installed (necessary, and not covered by previous check).
		return (GooglePlayServicesHelper.checkPlayServices(activity) && isGoogleMapsInstalled(activity));
	}

	private static boolean isGoogleMapsInstalled(Context context)
	{
	    try
	    {
	        context.getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0);
	        return true;

	        // To get:
	        // Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
	        // context.startActivity(intent);
	    }
	    catch (PackageManager.NameNotFoundException e)
	    {
	        return false;
	    }
	}

	static int mapTypeToGoogleMapType(String mapType)
	{
		if (Strings.hasValue(mapType))
		{
			if (mapType.equalsIgnoreCase(GxMapViewDefinition.MAP_TYPE_HYBRID))
				return GoogleMap.MAP_TYPE_HYBRID;
			else if (mapType.equalsIgnoreCase(GxMapViewDefinition.MAP_TYPE_SATELLITE))
				return GoogleMap.MAP_TYPE_SATELLITE;
			else if (mapType.equalsIgnoreCase(GxMapViewDefinition.MAP_TYPE_TERRAIN))
				return GoogleMap.MAP_TYPE_TERRAIN;
		}

		return GoogleMap.MAP_TYPE_NORMAL;
	}

	static String mapTypeFromGoogleMapType(int googleMapType)
	{
		switch (googleMapType)
		{
			case GoogleMap.MAP_TYPE_HYBRID :
				return GxMapViewDefinition.MAP_TYPE_HYBRID;
			case GoogleMap.MAP_TYPE_SATELLITE :
				return GxMapViewDefinition.MAP_TYPE_SATELLITE;
			case GoogleMap.MAP_TYPE_TERRAIN :
				return GxMapViewDefinition.MAP_TYPE_TERRAIN;
			default :
				return GxMapViewDefinition.MAP_TYPE_STANDARD;
		}
	}
}
