package com.artech.android.api;

import java.util.List;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.artech.base.services.Services;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

public class GeoLocationAPI extends ExternalApi
{
	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		Object result = null;
		Activity myActivity = getActivity();
		List<String> parameterValues = toString(parameters);

		if (method.equalsIgnoreCase("getmylocation")) //$NON-NLS-1$
		{
			// Get current location. Returns null if we have no location and ignoreErrors is false.
			result = SDActions.getMyLocation(myActivity, parameterValues, true);
			if (result == null)
				return ExternalApiResult.FAILURE;
		}
		else if (method.equalsIgnoreCase("getaddress")) //$NON-NLS-1$
		{
			// Get address from geolocation. Returns a collection
			result = SDActions.getAddressFromLocation(myActivity, parameterValues);
		}
		else if (method.equalsIgnoreCase("getlocationhistory")) //$NON-NLS-1$
		{
			// Get distance to geolocation.
			Services.Log.info("call getlocationhistory"); //$NON-NLS-1$
			result = SDActions.getLocationHistory(parameterValues);
		}
		else if (method.equalsIgnoreCase("getlocation")) //$NON-NLS-1$
		{
			// Get geolocation from address. Retruns a collection
			result = SDActions.getLocationFromAddress(myActivity, parameterValues);
		}
		else if (method.equalsIgnoreCase("getlatitude")) //$NON-NLS-1$
		{
			// Get latitude from geolocation.
			result = SDActions.getLatitudeFromLocation(parameterValues);
		}
		else if (method.equalsIgnoreCase("getlongitude")) //$NON-NLS-1$
		{
			// Get longitude from geolocation.
			result = SDActions.getLongitudeFromLocation(parameterValues);
		}
		else if (method.equalsIgnoreCase("getdistance")) //$NON-NLS-1$
		{
			// Get distance to geolocation.
			result = SDActions.getDistanceFromLocations(parameterValues);
		}
		else if (method.equalsIgnoreCase("starttracking")) //$NON-NLS-1$
		{
			// starttracking.
			Services.Log.info("call starttracking"); //$NON-NLS-1$
			result = SDActions.startTracking(myActivity, parameterValues);
		}
		else if (method.equalsIgnoreCase("endtracking")) //$NON-NLS-1$
		{
			// endtracking.
			Services.Log.info("call endtracking"); //$NON-NLS-1$
			result = SDActions.endTracking(myActivity);
		}
		else if (method.equalsIgnoreCase("clearlocationhistory")) //$NON-NLS-1$
		{
			// Get distance to geolocation.
			result = SDActions.clearLocationHistory();
		}
		else if (method.equalsIgnoreCase("authorized")) //$NON-NLS-1$
		{
			// In android always is authorized to use gps if app is installed
			result = true;
		}
		else if (method.equalsIgnoreCase("serviceenabled")) //$NON-NLS-1$
		{
			// In android location services are disable is network and gps location are disabled
			result = SDActions.isLocationServiceEnabled();
		}
		else if (method.equalsIgnoreCase("authorizationstatus")) //$NON-NLS-1$
		{
			result = 3; // APIAuthorizationStatus.Authorized
		}
		else
			return ExternalApiResult.failureUnknownMethod(this, method);
		
		return ExternalApiResult.success(result);
	}
}
